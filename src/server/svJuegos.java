package server;

import distribuido.modelo.JuegoRegistro;
import distribuido.modelo.JuegosData;
import distribuido.modelo.ReservaRegistro;
import persistencia.GestorPersistencia;
import protocolo.MensajeProtocolo;
import seguridad.ValidadorSesionRemoto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class svJuegos {

    private static final long TTL_RESERVA_MS = 5 * 60 * 1000L;
    private final int puerto;
    private final GestorPersistencia<JuegosData> persistencia;
    private final ValidadorSesionRemoto validador = new ValidadorSesionRemoto(8081, 8181);
    private final ExecutorService pool = Executors.newFixedThreadPool(24);
    private JuegosData data;

    public svJuegos(int puerto) {
        this.puerto = puerto;
        this.persistencia = new GestorPersistencia<>("data/GME_Main.txt", "data/GME_Copy.txt", JuegosData.class);
        this.data = persistencia.leer(datosIniciales());
    }

    public static void main(String[] args) throws Exception {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8082;
        new svJuegos(puerto).iniciar();
    }

    public void iniciar() throws Exception {
        Thread daemonLocks = new Thread(this::limpiarReservasExpiradas, "GestorDeLocks-Juegos");
        daemonLocks.setDaemon(true);
        daemonLocks.start();

        try (ServerSocket server = new ServerSocket(puerto)) {
            System.out.println("svJuegos escuchando en puerto " + puerto);
            while (true) {
                Socket socket = server.accept();
                pool.submit(() -> atender(socket));
            }
        }
    }

    private void atender(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(5000);
            MensajeProtocolo req = red.JsonSocket.leer(in);
            MensajeProtocolo res = procesar(req);
            red.JsonSocket.escribir(out, res);
        } catch (Exception e) {
            System.out.println("Error svJuegos: " + e.getMessage());
        }
    }

    private MensajeProtocolo procesar(MensajeProtocolo req) {
        if (req == null) {
            return MensajeProtocolo.error(null, "Solicitud vacia");
        }
        if ("HEALTH".equalsIgnoreCase(req.getOperacion())) {
            return MensajeProtocolo.ok(req, "OK");
        }
        return switch (req.getOperacion()) {
            case "LISTAR_JUEGOS" -> listar(req);
            case "CARGAR_BILLETERA" -> cargarBilletera(req);
            case "RESERVAR_JUEGO" -> reservar(req);
            case "CONFIRMAR_COMPRA" -> confirmar(req);
            case "PUBLICAR_JUEGO" -> publicar(req);
            default -> MensajeProtocolo.error(req, "Operacion de juegos no soportada: " + req.getOperacion());
        };
    }

    private synchronized MensajeProtocolo listar(MensajeProtocolo req) {
        MensajeProtocolo res = MensajeProtocolo.ok(req, "Catalogo disponible");
        res.put("catalogo", new ArrayList<>(data.catalogo.values()));
        return res;
    }

    private synchronized MensajeProtocolo cargarBilletera(MensajeProtocolo req) {
        MensajeProtocolo validacion = validador.validar(req.getToken(), "COMPRADOR,ADMIN");
        if (!validacion.isExito()) {
            return MensajeProtocolo.error(req, validacion.getMensaje());
        }
        String usuario = ValidadorSesionRemoto.usuarioDesde(validacion);
        double monto = req.numero("monto");
        if (monto <= 0) {
            return MensajeProtocolo.error(req, "Monto invalido");
        }
        double nuevoSaldo = data.billeteras.getOrDefault(usuario, 0.0) + monto;
        data.billeteras.put(usuario, nuevoSaldo);
        persistencia.escribir(data);
        MensajeProtocolo res = MensajeProtocolo.ok(req, "Billetera cargada");
        res.put("saldo", nuevoSaldo);
        return res;
    }

    private synchronized MensajeProtocolo reservar(MensajeProtocolo req) {
        MensajeProtocolo validacion = validador.validar(req.getToken(), "COMPRADOR,ADMIN");
        if (!validacion.isExito()) {
            return MensajeProtocolo.error(req, validacion.getMensaje());
        }
        String usuario = ValidadorSesionRemoto.usuarioDesde(validacion);
        String juegoId = req.texto("juegoId");
        JuegoRegistro juego = data.catalogo.get(juegoId);
        if (juego == null) {
            return MensajeProtocolo.error(req, "Juego no existe");
        }
        if (juego.stock <= 0) {
            return MensajeProtocolo.error(req, "Juego no disponible");
        }

        // Region critica: stock finito y reserva quedan protegidos por synchronized.
        juego.stock--;
        String reservaId = UUID.randomUUID().toString();
        long vence = System.currentTimeMillis() + TTL_RESERVA_MS;
        data.reservas.put(reservaId, new ReservaRegistro(reservaId, usuario, juegoId, vence));
        persistencia.escribir(data);

        MensajeProtocolo res = MensajeProtocolo.ok(req, "Reserva lista: tienes 5 minutos para pagar");
        res.put("reservaId", reservaId);
        res.put("venceEnEpochMs", vence);
        return res;
    }

    private synchronized MensajeProtocolo confirmar(MensajeProtocolo req) {
        MensajeProtocolo validacion = validador.validar(req.getToken(), "COMPRADOR,ADMIN");
        if (!validacion.isExito()) {
            return MensajeProtocolo.error(req, validacion.getMensaje());
        }
        String usuario = ValidadorSesionRemoto.usuarioDesde(validacion);
        String reservaId = req.texto("reservaId");
        ReservaRegistro reserva = data.reservas.get(reservaId);
        if (reserva == null || !usuario.equals(reserva.usuario)) {
            return MensajeProtocolo.error(req, "Reserva no encontrada");
        }
        if (reserva.venceEnEpochMs < System.currentTimeMillis()) {
            liberarReserva(reservaId);
            return MensajeProtocolo.error(req, "Tiempo agotado");
        }
        JuegoRegistro juego = data.catalogo.get(reserva.juegoId);
        double saldo = data.billeteras.getOrDefault(usuario, 0.0);
        if (saldo < juego.precio) {
            return MensajeProtocolo.error(req, "Saldo insuficiente");
        }

        data.billeteras.put(usuario, saldo - juego.precio);
        data.bibliotecas.computeIfAbsent(usuario, k -> new ArrayList<>()).add(juego.id);
        data.reservas.remove(reservaId);
        persistencia.escribir(data);

        MensajeProtocolo res = MensajeProtocolo.ok(req, "Compra exitosa");
        res.put("saldo", saldo - juego.precio);
        return res;
    }

    private synchronized MensajeProtocolo publicar(MensajeProtocolo req) {
        MensajeProtocolo validacion = validador.validar(req.getToken(), "VENDEDOR,ADMIN");
        if (!validacion.isExito()) {
            return MensajeProtocolo.error(req, validacion.getMensaje());
        }
        String id = req.texto("juegoId");
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString().substring(0, 8);
        }
        JuegoRegistro juego = new JuegoRegistro(id, req.texto("nombre"), ValidadorSesionRemoto.usuarioDesde(validacion), req.numero("precio"), (int) req.numero("stock"));
        data.catalogo.put(id, juego);
        persistencia.escribir(data);
        MensajeProtocolo res = MensajeProtocolo.ok(req, "Juego publicado");
        res.put("juegoId", id);
        return res;
    }

    private void limpiarReservasExpiradas() {
        while (true) {
            try {
                Thread.sleep(5000);
                synchronized (this) {
                    long ahora = System.currentTimeMillis();
                    var expiradas = data.reservas.values().stream()
                            .filter(r -> r.venceEnEpochMs <= ahora)
                            .map(r -> r.reservaId)
                            .toList();
                    for (String reservaId : expiradas) {
                        liberarReserva(reservaId);
                    }
                    if (!expiradas.isEmpty()) {
                        persistencia.escribir(data);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void liberarReserva(String reservaId) {
        ReservaRegistro reserva = data.reservas.remove(reservaId);
        if (reserva != null) {
            JuegoRegistro juego = data.catalogo.get(reserva.juegoId);
            if (juego != null) {
                juego.stock++;
            }
        }
    }

    private static JuegosData datosIniciales() {
        JuegosData datos = new JuegosData();
        datos.catalogo.put("minecraft", new JuegoRegistro("minecraft", "Minecraft", "Mojang", 12000, 10));
        datos.catalogo.put("terraria", new JuegoRegistro("terraria", "Terraria", "Re-Logic", 8000, 8));
        datos.catalogo.put("portal", new JuegoRegistro("portal", "Portal", "Valve", 7000, 6));
        datos.catalogo.put("gtav", new JuegoRegistro("gtav", "GTA V", "Rockstar Games", 9500, 5));
        datos.billeteras.put("comprador", 30000.0);
        return datos;
    }
}
