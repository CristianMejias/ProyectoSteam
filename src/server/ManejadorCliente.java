package server;

import model.Juego;
import model.Mensaje;
import model.Reserva;
import model.Rol;
import model.Usuario;
import persistencia.Persistencia;
import utils.Constantes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class ManejadorCliente implements Runnable {

    private Socket socketCliente;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private String nombreUsuario;
    private Rol rol;

    public ManejadorCliente(Socket socketCliente) {
        this.socketCliente = socketCliente;
    }

    @Override
    public void run() {
        try {
            salida = new ObjectOutputStream(socketCliente.getOutputStream());
            entrada = new ObjectInputStream(socketCliente.getInputStream());

            while (true) {
                Mensaje mensaje = (Mensaje) entrada.readObject();

                if (nombreUsuario == null) {
                    boolean registrado = registrarUsuario(mensaje.getUsuario(), mensaje.getContenido());

                    if (!registrado) {
                        return;
                    }

                    continue;
                }

                String contenido = mensaje.getContenido().trim();

                if (contenido.isEmpty()) {
                    continue;
                }

                System.out.println("Mensaje recibido -> " + mensaje);

                if (contenido.equals("/salir")) {
                    break;
                }

                try {
                    procesarComando(contenido, mensaje);
                } catch (SecurityException e) {
                    enviarMensajeServidor(e.getMessage());
                } catch (Exception e) {
                    enviarMensajeServidor("Entrada inválida o error procesando comando.");
                    System.out.println("Error procesando comando de " + nombreUsuario + ": " + e.getMessage());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Cliente desconectado inesperadamente: " + nombreUsuario);
        } finally {
            cerrarConexion();
        }
    }

    private boolean registrarUsuario(String posibleNombre, String contenido) throws IOException {
        if (!contenido.startsWith("/entrar")) {
            enviarMensajeServidor("Debes entrar indicando un rol.");
            socketCliente.close();
            return false;
        }

        String textoRol = contenido.replace("/entrar", "").trim();
        Rol rolSolicitado = Rol.desdeTexto(textoRol);

        if (ServerMain.usuariosActivos.contains(posibleNombre)) {
            enviarMensajeServidor("El nombre ya está en uso");
            socketCliente.close();
            return false;
        }

        nombreUsuario = posibleNombre;
        rol = rolSolicitado;

        ServerMain.usuariosActivos.add(nombreUsuario);
        ServerMain.clientesPorNombre.put(nombreUsuario, this);
        ServerMain.usuariosRegistrados.putIfAbsent(nombreUsuario, new Usuario(nombreUsuario, rol));
        ServerMain.usuariosRegistrados.get(nombreUsuario).setRol(rol);
        Persistencia.guardarUsuarios(ServerMain.usuariosRegistrados);

        System.out.println("Usuario conectado: " + nombreUsuario + " (" + rol + ")");
        enviarMensajeServidor("Conectado como " + rol + ". Escribe /ayuda para ver tus comandos.");
        entregarMensajesPendientes();
        enviarATodos(new Mensaje("Servidor", nombreUsuario + " se ha conectado"));
        return true;
    }

    private void procesarComando(String contenido, Mensaje mensaje) {
        if (contenido.equals("/ayuda")) {
            enviarAyuda();
        } else if (contenido.equals("/catalogo")) {
            enviarCatalogo();
        } else if (contenido.equals("/saldo")) {
            requiereUsuario();
            enviarSaldo();
        } else if (contenido.startsWith("/cargar ")) {
            requiereUsuario();
            cargarSaldo(contenido);
        } else if (contenido.startsWith("/comprar ")) {
            requiereUsuario();
            iniciarCompra(contenido.replace("/comprar ", "").trim());
        } else if (contenido.startsWith("/reservar ")) {
            requiereUsuario();
            iniciarCompra(contenido.replace("/reservar ", "").trim());
        } else if (contenido.startsWith("/confirmar ")) {
            requiereUsuario();
            confirmarCompra(contenido.replace("/confirmar ", "").trim());
        } else if (contenido.startsWith("/comprobar ")) {
            requiereUsuario();
            comprobarReserva(contenido.replace("/comprobar ", "").trim());
        } else if (contenido.equals("/misjuegos") || contenido.equals("/biblioteca")) {
            requiereUsuario();
            enviarBiblioteca(nombreUsuario);
        } else if (contenido.startsWith("/msg ")) {
            enviarPrivado(contenido);
        } else if (contenido.startsWith("/publicar ")) {
            requierePublisher();
            publicarJuego(contenido);
        } else if (contenido.startsWith("/agregarstock ")) {
            requierePublisher();
            agregarStock(contenido);
        } else if (contenido.equals("/usuarios")) {
            requiereAdmin();
            enviarUsuariosRegistrados();
        } else if (contenido.equals("/conectados")) {
            requiereAdmin();
            enviarConectados();
        } else if (contenido.equals("/juegos")) {
            requiereAdmin();
            enviarCatalogo();
        } else if (contenido.equals("/publishers")) {
            requiereAdmin();
            enviarPublishers();
        } else if (contenido.equals("/reservas")) {
            requiereAdmin();
            enviarReservas();
        } else if (contenido.startsWith("/biblioteca ")) {
            requiereAdmin();
            enviarBiblioteca(contenido.replace("/biblioteca ", "").trim());
        } else {
            enviarATodos(mensaje);
        }
    }

    private boolean esUsuario() {
        return rol == Rol.USUARIO;
    }

    private boolean esPublisher() {
        return rol == Rol.PUBLISHER;
    }

    private boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    private void requiereUsuario() {
        if (!esUsuario()) {
            throw new SecurityException("Comando solo permitido para usuarios normales.");
        }
    }

    private void requierePublisher() {
        if (!esPublisher()) {
            throw new SecurityException("Comando solo permitido para publishers.");
        }
    }

    private void requiereAdmin() {
        if (!esAdmin()) {
            throw new SecurityException("Comando solo permitido para administradores.");
        }
    }

    private void iniciarCompra(String nombreJuego) {
        if (nombreJuego.isEmpty()) {
            enviarMensajeServidor("Debes escribir el nombre del juego.");
            return;
        }

        Usuario usuario = ServerMain.usuariosRegistrados.get(nombreUsuario);
        Juego juego = ServerMain.catalogo.get(nombreJuego.toLowerCase());

        if (juego == null) {
            enviarMensajeServidor("El juego no existe en el catálogo.");
            return;
        }

        if (usuario.tieneJuego(juego.getNombre())) {
            enviarMensajeServidor("Ya tienes ese juego en tu biblioteca.");
            return;
        }

        String clave = claveReserva(nombreUsuario, juego.getNombre());

        if (ServerMain.reservasPendientes.containsKey(clave)) {
            enviarMensajeServidor("Ya tienes una compra pendiente para ese juego. Usa /confirmar " + juego.getNombre());
            return;
        }

        if (!juego.bloquearCopia()) {
            enviarMensajeServidor("No hay stock disponible para ese juego.");
            return;
        }

        Reserva reserva = new Reserva(nombreUsuario, juego.getNombre(), LocalDateTime.now().plusSeconds(Constantes.TIEMPO_RESERVA_MS / 1000));
        ServerMain.reservasPendientes.put(clave, reserva);

        ServerMain.planificador.schedule(() -> expirarReserva(clave), Constantes.TIEMPO_RESERVA_MS, TimeUnit.MILLISECONDS);
        enviarMensajeServidor("Copia bloqueada temporalmente. Confirma con /confirmar " + juego.getNombre() + " antes de " + Constantes.MSJ_TIEMPO_RESERVA_S);
    }

    private void confirmarCompra(String nombreJuego) {
        String clave = claveReserva(nombreUsuario, nombreJuego);
        Reserva reserva = ServerMain.reservasPendientes.get(clave);

        if (reserva == null || !reserva.isActiva()) {
            enviarMensajeServidor("No tienes una compra pendiente para ese juego.");
            return;
        }

        Juego juego = ServerMain.catalogo.get(nombreJuego.toLowerCase());
        Usuario usuario = ServerMain.usuariosRegistrados.get(nombreUsuario);

        if (juego == null) {
            enviarMensajeServidor("El juego ya no existe.");
            return;
        }

        synchronized (reserva) {
            if (!reserva.isActiva()) {
                enviarMensajeServidor("La reserva ya expiró.");
                return;
            }

            if (!usuario.descontarSaldo(juego.getPrecio())) {
                enviarMensajeServidor("Saldo insuficiente. La copia seguirá bloqueada hasta que expire.");
                return;
            }

            usuario.agregarJuego(juego);
            reserva.cerrar();
            ServerMain.reservasPendientes.remove(clave);
            Persistencia.guardarUsuarios(ServerMain.usuariosRegistrados);
            enviarMensajeServidor("Compra confirmada. Juego agregado a tu biblioteca: " + juego.getNombre());
        }
    }

    private void expirarReserva(String clave) {
        Reserva reserva = ServerMain.reservasPendientes.get(clave);

        if (reserva == null || !reserva.isActiva()) {
            return;
        }

        synchronized (reserva) {
            if (!reserva.isActiva()) {
                return;
            }

            Juego juego = ServerMain.catalogo.get(reserva.getJuego().toLowerCase());

            if (juego != null) {
                juego.liberarCopia();
            }

            reserva.cerrar();
            ServerMain.reservasPendientes.remove(clave);

            ManejadorCliente cliente = ServerMain.clientesPorNombre.get(reserva.getUsuario());

            if (cliente != null) {
                cliente.enviarMensajeServidor("Tu reserva para " + reserva.getJuego() + " expiró y el stock fue liberado.");
            }
        }
    }

    private void comprobarReserva(String nombreJuego) {
        Reserva reserva = ServerMain.reservasPendientes.get(claveReserva(nombreUsuario, nombreJuego));

        if (reserva == null || !reserva.isActiva()) {
            enviarMensajeServidor("No existe reserva activa para ese juego.");
            return;
        }

        enviarMensajeServidor("Reserva activa: " + reserva);
    }

    private void cargarSaldo(String contenido) {
        try {
            double monto = Double.parseDouble(contenido.replace("/cargar ", "").trim());

            if (monto <= 0) {
                enviarMensajeServidor("El monto debe ser mayor que cero.");
                return;
            }

            Usuario usuario = ServerMain.usuariosRegistrados.get(nombreUsuario);
            usuario.cargarSaldo(monto);
            Persistencia.guardarUsuarios(ServerMain.usuariosRegistrados);
            enviarMensajeServidor("Saldo cargado. Saldo actual: $" + usuario.getSaldo());
        } catch (NumberFormatException e) {
            enviarMensajeServidor("Uso correcto: /cargar monto");
        }
    }

    private void enviarSaldo() {
        Usuario usuario = ServerMain.usuariosRegistrados.get(nombreUsuario);
        enviarMensajeServidor("Saldo actual: $" + usuario.getSaldo());
    }

    private void publicarJuego(String contenido) {
        String[] partes = contenido.split("\\s+");

        if (partes.length < 4) {
            enviarMensajeServidor("Uso: /publicar nombre precio stock [reserva]");
            return;
        }

        try {
            String nombre = partes[1];
            double precio = Double.parseDouble(partes[2]);
            int stock = Integer.parseInt(partes[3]);
            boolean soloReserva = partes.length >= 5 && partes[4].equalsIgnoreCase("reserva");

            if (precio < 0 || stock <= 0) {
                enviarMensajeServidor("Precio debe ser >= 0 y stock debe ser > 0.");
                return;
            }

            String clave = nombre.toLowerCase();

            if (ServerMain.catalogo.containsKey(clave)) {
                enviarMensajeServidor("Ese juego ya existe. Usa /agregarstock.");
                return;
            }

            ServerMain.catalogo.put(clave, new Juego(nombre, precio, nombreUsuario, stock, soloReserva));
            enviarMensajeServidor("Juego publicado correctamente: " + nombre);
        } catch (NumberFormatException e) {
            enviarMensajeServidor("Precio o stock inválido.");
        }
    }

    private void agregarStock(String contenido) {
        String[] partes = contenido.split("\\s+");

        if (partes.length < 3) {
            enviarMensajeServidor("Uso: /agregarstock juego cantidad");
            return;
        }

        try {
            String nombre = partes[1];
            int cantidad = Integer.parseInt(partes[2]);
            Juego juego = ServerMain.catalogo.get(nombre.toLowerCase());

            if (juego == null) {
                enviarMensajeServidor("El juego no existe.");
                return;
            }

            if (!juego.getPublisher().equalsIgnoreCase(nombreUsuario)) {
                enviarMensajeServidor("Solo el publisher dueño puede agregar stock.");
                return;
            }

            juego.agregarStock(cantidad);
            enviarMensajeServidor("Stock actualizado: " + juego);
        } catch (NumberFormatException e) {
            enviarMensajeServidor("Cantidad inválida.");
        }
    }

    private void enviarPrivado(String contenido) {
        String[] partes = contenido.split("\\s+", 3);

        if (partes.length < 3) {
            enviarMensajeServidor("Uso: /msg usuario mensaje");
            return;
        }

        String destino = partes[1];
        String texto = partes[2];
        Mensaje privado = new Mensaje("Privado de " + nombreUsuario, texto);
        ManejadorCliente receptor = ServerMain.clientesPorNombre.get(destino);

        if (receptor != null) {
            receptor.enviarMensaje(privado);
            enviarMensajeServidor("Mensaje privado enviado a " + destino);
            return;
        }

        ServerMain.mensajesPendientes.putIfAbsent(destino, new ConcurrentLinkedQueue<>());
        ServerMain.mensajesPendientes.get(destino).add(privado);
        enviarMensajeServidor("El usuario no está conectado. Mensaje privado guardado temporalmente.");
    }

    private void entregarMensajesPendientes() {
        Queue<Mensaje> pendientes = ServerMain.mensajesPendientes.remove(nombreUsuario);

        if (pendientes == null || pendientes.isEmpty()) {
            return;
        }

        enviarMensajeServidor("Tienes mensajes privados pendientes:");
        Mensaje mensaje;

        while ((mensaje = pendientes.poll()) != null) {
            enviarMensaje(mensaje);
        }
    }

    private void enviarCatalogo() {
        if (ServerMain.catalogo.isEmpty()) {
            enviarMensajeServidor("Catálogo vacío.");
            return;
        }

        StringBuilder respuesta = new StringBuilder("\nCatálogo:\n");

        for (Juego juego : ServerMain.catalogo.values()) {
            respuesta.append("- ").append(juego).append("\n");
        }

        enviarMensajeServidor(respuesta.toString());
    }

    private void enviarBiblioteca(String usuarioBuscado) {
        Usuario usuario = ServerMain.usuariosRegistrados.get(usuarioBuscado);

        if (usuario == null) {
            enviarMensajeServidor("Usuario no encontrado.");
            return;
        }

        List<Juego> biblioteca = usuario.getBibliotecaCopia();

        if (biblioteca.isEmpty()) {
            enviarMensajeServidor("Biblioteca vacía.");
            return;
        }

        StringBuilder respuesta = new StringBuilder("\nBiblioteca de " + usuarioBuscado + ":\n");

        for (Juego juego : biblioteca) {
            respuesta.append("- ").append(juego.getNombre()).append("\n");
        }

        enviarMensajeServidor(respuesta.toString());
    }

    private void enviarUsuariosRegistrados() {
        if (ServerMain.usuariosRegistrados.isEmpty()) {
            enviarMensajeServidor("No hay usuarios registrados.");
            return;
        }

        StringBuilder respuesta = new StringBuilder("\nUsuarios registrados:\n");

        for (Usuario usuario : ServerMain.usuariosRegistrados.values()) {
            respuesta.append("- ").append(usuario.getNombre()).append(" (").append(usuario.getRol()).append(")\n");
        }

        enviarMensajeServidor(respuesta.toString());
    }

    private void enviarConectados() {
        if (ServerMain.usuariosActivos.isEmpty()) {
            enviarMensajeServidor("No hay usuarios conectados.");
            return;
        }

        StringBuilder respuesta = new StringBuilder("\nUsuarios conectados:\n");

        for (String usuario : ServerMain.usuariosActivos) {
            respuesta.append("- ").append(usuario).append("\n");
        }

        enviarMensajeServidor(respuesta.toString());
    }

    private void enviarPublishers() {
        StringBuilder respuesta = new StringBuilder("\nPublishers:\n");

        for (Usuario usuario : ServerMain.usuariosRegistrados.values()) {
            if (usuario.getRol() == Rol.PUBLISHER) {
                respuesta.append("- ").append(usuario.getNombre()).append("\n");
            }
        }

        enviarMensajeServidor(respuesta.toString());
    }

    private void enviarReservas() {
        if (ServerMain.reservasPendientes.isEmpty()) {
            enviarMensajeServidor("No hay reservas o compras pendientes.");
            return;
        }

        StringBuilder respuesta = new StringBuilder("\nReservas pendientes:\n");

        for (Reserva reserva : ServerMain.reservasPendientes.values()) {
            respuesta.append("- ").append(reserva).append("\n");
        }

        enviarMensajeServidor(respuesta.toString());
    }

    private void enviarAyuda() {
        String ayuda;

        if (rol == Rol.USUARIO) {
            ayuda = """

                    ===== COMANDOS USUARIO =====
                    /catalogo
                    /saldo
                    /cargar monto
                    /comprar juego
                    /confirmar juego
                    /reservar juego
                    /comprobar juego
                    /misjuegos
                    /msg usuario mensaje
                    /ayuda
                    /salir
                    ===========================
                    """;
        } else if (rol == Rol.PUBLISHER) {
            ayuda = """

                    ===== COMANDOS PUBLISHER =====
                    /catalogo
                    /publicar nombre precio stock [reserva]
                    /agregarstock juego cantidad
                    /msg usuario mensaje
                    /ayuda
                    /salir
                    =============================
                    """;
        } else {
            ayuda = """

                    ===== COMANDOS ADMIN =====
                    /usuarios
                    /conectados
                    /juegos
                    /publishers
                    /reservas
                    /biblioteca usuario
                    /msg usuario mensaje
                    /ayuda
                    /salir
                    =========================
                    """;
        }

        enviarMensajeServidor(ayuda);
    }

    private String claveReserva(String usuario, String juego) {
        return usuario.toLowerCase() + "::" + juego.toLowerCase();
    }

    private void enviarATodos(Mensaje mensaje) {
        for (ManejadorCliente cliente : ServerMain.clientesConectados) {
            cliente.enviarMensaje(mensaje);
        }
    }

    public void enviarMensaje(Mensaje mensaje) {
        try {
            synchronized (salida) {
                salida.writeObject(mensaje);
                salida.flush();
            }
        } catch (IOException e) {
            System.out.println("Error enviando mensaje.");
        }
    }

    private void enviarMensajeServidor(String contenido) {
        enviarMensaje(new Mensaje("Servidor", contenido));
    }

    private void cerrarConexion() {
        if (nombreUsuario != null) {
            ServerMain.usuariosActivos.remove(nombreUsuario);
            ServerMain.clientesPorNombre.remove(nombreUsuario);
            System.out.println("Cliente desconectado: " + nombreUsuario);
            enviarATodos(new Mensaje("Servidor", nombreUsuario + " se ha desconectado"));
        }

        ServerMain.clientesConectados.remove(this);

        try {
            socketCliente.close();
        } catch (IOException e) {
            System.out.println("Error al cerrar socket.");
        }
    }
}
