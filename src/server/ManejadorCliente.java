package server;

import model.Juego;
import model.Mensaje;
import persistencia.Persistencia;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ManejadorCliente implements Runnable {

    private Socket socketCliente;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private String nombreUsuario;

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
                    boolean registrado = registrarUsuario(mensaje.getUsuario());

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

                if (contenido.startsWith("/comprar ")) {
                    comprarJuego(mensaje);
                } else if (contenido.equals("/biblioteca")) {
                    enviarBiblioteca();
                } else if (contenido.equals("/usuarios")) {
                    enviarUsuarios();
                } else if (contenido.equals("/ayuda")) {
                    enviarAyuda();
                } else if (contenido.equals("/salir")) {
                    break;
                } else {
                    enviarATodos(mensaje);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Cliente desconectado inesperadamente: " + nombreUsuario);
        } finally {
            cerrarConexion();
        }
    }

    private boolean registrarUsuario(String posibleNombre) throws IOException {

        synchronized (ServerMain.usuariosActivos) {
            if (ServerMain.usuariosActivos.contains(posibleNombre)) {
                salida.writeObject(new Mensaje("Servidor", "El nombre ya está en uso"));
                socketCliente.close();
                return false;
            }

            ServerMain.usuariosActivos.add(posibleNombre);
        }

        nombreUsuario = posibleNombre;

        System.out.println("Usuario conectado: " + nombreUsuario);

        enviarATodos(new Mensaje("Servidor", nombreUsuario + " se ha conectado"));

        return true;
    }

    private void enviarATodos(Mensaje mensaje) {

        synchronized (ServerMain.clientesConectados) {
            for (ManejadorCliente cliente : ServerMain.clientesConectados) {
                try {
                    cliente.salida.writeObject(mensaje);
                } catch (IOException e) {
                    System.out.println("Error enviando mensaje.");
                }
            }
        }
    }

    private void comprarJuego(Mensaje mensaje) {

        String nombreJuego = mensaje.getContenido().replace("/comprar ", "").trim();

        if (nombreJuego.isEmpty()) {
            enviarMensajeServidor("Debes escribir el nombre del juego.");
            return;
        }

        synchronized (ServerMain.bibliotecasUsuarios) {
            ServerMain.bibliotecasUsuarios.putIfAbsent(nombreUsuario, new ArrayList<>());

            List<Juego> biblioteca = ServerMain.bibliotecasUsuarios.get(nombreUsuario);

            for (Juego juegoExistente : biblioteca) {
                if (juegoExistente.getNombre().equalsIgnoreCase(nombreJuego)) {
                    enviarMensajeServidor("Ya tienes ese juego");
                    return;
                }
            }

            Juego juego = new Juego(nombreJuego);
            biblioteca.add(juego);

            Persistencia.guardarBibliotecas(ServerMain.bibliotecasUsuarios);

            enviarMensajeServidor("Juego agregado: " + nombreJuego);
        }
    }

    private void enviarBiblioteca() {

        synchronized (ServerMain.bibliotecasUsuarios) {
            List<Juego> biblioteca = ServerMain.bibliotecasUsuarios.get(nombreUsuario);

            if (biblioteca == null || biblioteca.isEmpty()) {
                enviarMensajeServidor("Biblioteca vacía");
                return;
            }

            StringBuilder juegos = new StringBuilder();

            for (Juego juego : biblioteca) {
                juegos.append("- ").append(juego.getNombre()).append("\n");
            }

            enviarMensajeServidor("\nBiblioteca:\n" + juegos);
        }
    }

    private void enviarUsuarios() {

        StringBuilder usuarios = new StringBuilder();

        synchronized (ServerMain.clientesConectados) {
            for (ManejadorCliente cliente : ServerMain.clientesConectados) {
                if (cliente.nombreUsuario != null) {
                    usuarios.append("- ").append(cliente.nombreUsuario).append("\n");
                }
            }
        }

        enviarMensajeServidor("\nUsuarios conectados:\n" + usuarios);
    }

    private void enviarAyuda() {

        String ayuda = """
                
                ===== COMANDOS DISPONIBLES =====
                
                /ayuda
                Muestra esta lista de comandos
                
                /usuarios
                Muestra usuarios conectados
                
                /comprar NOMBRE
                Compra un juego
                
                /biblioteca
                Muestra tu biblioteca
                
                /salir
                Cierra la conexión
                
                ================================
                """;

        enviarMensajeServidor(ayuda);
    }

    private void enviarMensajeServidor(String contenido) {

        try {
            salida.writeObject(new Mensaje("Servidor", contenido));
        } catch (IOException e) {
            System.out.println("Error enviando mensaje al cliente.");
        }
    }

    private void cerrarConexion() {

        if (nombreUsuario != null) {
            synchronized (ServerMain.usuariosActivos) {
                ServerMain.usuariosActivos.remove(nombreUsuario);
            }

            synchronized (ServerMain.clientesConectados) {
                ServerMain.clientesConectados.remove(this);
            }

            System.out.println("Cliente desconectado: " + nombreUsuario);

            enviarATodos(new Mensaje("Servidor", nombreUsuario + " se ha desconectado"));
        } else {
            synchronized (ServerMain.clientesConectados) {
                ServerMain.clientesConectados.remove(this);
            }
        }

        try {
            socketCliente.close();
        } catch (IOException e) {
            System.out.println("Error al cerrar socket.");
        }
    }
}