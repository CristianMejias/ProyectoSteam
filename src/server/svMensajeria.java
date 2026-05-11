package server;

import distribuido.modelo.MensajeChat;
import distribuido.modelo.MensajeriaData;
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

public class svMensajeria {

    private final int puerto;
    private final GestorPersistencia<MensajeriaData> persistencia;
    private final ValidadorSesionRemoto validador = new ValidadorSesionRemoto(8081, 8181);
    private final ExecutorService pool = Executors.newFixedThreadPool(16);
    private MensajeriaData data;

    public svMensajeria(int puerto) {
        this.puerto = puerto;
        this.persistencia = new GestorPersistencia<>("data/MSG_Main.txt", "data/MSG_Copy.txt", MensajeriaData.class);
        this.data = persistencia.leer(new MensajeriaData());
    }

    public static void main(String[] args) throws Exception {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8083;
        new svMensajeria(puerto).iniciar();
    }

    public void iniciar() throws Exception {
        try (ServerSocket server = new ServerSocket(puerto)) {
            System.out.println("svMensajeria escuchando en puerto " + puerto);
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
            System.out.println("Error svMensajeria: " + e.getMessage());
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
            case "ENVIAR_MENSAJE" -> enviar(req);
            case "LEER_MENSAJES" -> leer(req);
            default -> MensajeProtocolo.error(req, "Operacion de mensajeria no soportada: " + req.getOperacion());
        };
    }

    private synchronized MensajeProtocolo enviar(MensajeProtocolo req) {
        MensajeProtocolo validacion = validador.validar(req.getToken(), "ANY");
        if (!validacion.isExito()) {
            return MensajeProtocolo.error(req, validacion.getMensaje());
        }
        String de = ValidadorSesionRemoto.usuarioDesde(validacion);
        String para = req.texto("para");
        String texto = req.texto("texto");
        if (para == null || texto == null || texto.isBlank()) {
            return MensajeProtocolo.error(req, "Mensaje incompleto");
        }
        MensajeChat chat = new MensajeChat(UUID.randomUUID().toString(), de, para, texto, System.currentTimeMillis());
        // Region critica: persistencia de mensajes offline y replicacion Main->Copy.
        data.buzonesOffline.computeIfAbsent(para, k -> new ArrayList<>()).add(chat);
        persistencia.escribir(data);
        return MensajeProtocolo.ok(req, "Mensaje almacenado para entrega offline");
    }

    private synchronized MensajeProtocolo leer(MensajeProtocolo req) {
        MensajeProtocolo validacion = validador.validar(req.getToken(), "ANY");
        if (!validacion.isExito()) {
            return MensajeProtocolo.error(req, validacion.getMensaje());
        }
        String usuario = ValidadorSesionRemoto.usuarioDesde(validacion);
        var mensajes = data.buzonesOffline.getOrDefault(usuario, new ArrayList<>());
        MensajeProtocolo res = MensajeProtocolo.ok(req, "Mensajes pendientes recuperados");
        res.put("mensajes", new ArrayList<>(mensajes));
        data.buzonesOffline.remove(usuario);
        persistencia.escribir(data);
        return res;
    }
}
