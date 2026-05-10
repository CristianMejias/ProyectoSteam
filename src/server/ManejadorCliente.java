package server;

import model.Mensaje;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import model.Juego;
import java.util.ArrayList;
import java.util.List;

import persistencia.Persistencia;

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

            salida = new ObjectOutputStream(
                    socketCliente.getOutputStream());

            entrada = new ObjectInputStream(
                    socketCliente.getInputStream());

            salida.writeObject(
                    new Mensaje(
                            "Servidor",
                            "Conectado al chat global"));

            while (true) {

                Mensaje mensaje
                        = (Mensaje) entrada.readObject();

                if (nombreUsuario == null) {

                    String posibleNombre =
                            mensaje.getUsuario();

                    synchronized (ServerMain.usuariosActivos) {

                        if (ServerMain.usuariosActivos
                                .contains(posibleNombre)) {

                            salida.writeObject(
                                    new Mensaje(
                                            "Servidor",
                                            "El nombre ya está en uso"));

                            socketCliente.close();

                            return;
                        }

                        ServerMain.usuariosActivos
                                .add(posibleNombre);
                    }

                    nombreUsuario = posibleNombre;

                    enviarATodos(
                            new Mensaje(
                                    "Servidor",
                                    nombreUsuario
                                            + " se ha conectado"));
                }

                System.out.println(
                        "Mensaje recibido -> "
                        + mensaje);

                String contenido
                        = mensaje.getContenido();
                if (contenido.trim().isEmpty()) {
                    continue;
                }

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

            System.out.println(
                    "Cliente desconectado: "
                    + nombreUsuario);

        } finally {

            try {

                synchronized (ServerMain.clientesConectados) {

                    ServerMain.clientesConectados.remove(this);
                }
                
                if (nombreUsuario != null) {

                    enviarATodos(
                            new Mensaje(
                                    "Servidor",
                                    nombreUsuario
                                            + " se ha desconectado"));
                }
                
                synchronized (ServerMain.usuariosActivos) {

                    ServerMain.usuariosActivos
                            .remove(nombreUsuario);
                }

                socketCliente.close();

            } catch (IOException e) {

                System.out.println(
                        "Error al cerrar socket.");
            }
        }
    }

    // Método para reenviar mensajes
    private void enviarATodos(Mensaje mensaje) {

        synchronized (ServerMain.clientesConectados) {

            for (ManejadorCliente cliente
                    : ServerMain.clientesConectados) {

                try {

                    cliente.salida.writeObject(mensaje);

                } catch (IOException e) {

                    System.out.println(
                            "Error enviando mensaje.");
                }
            }
        }
    }

    private void comprarJuego(Mensaje mensaje) {

        String nombreJuego
                = mensaje.getContenido()
                        .replace("/comprar ", "");

        synchronized (ServerMain.bibliotecasUsuarios) {

            ServerMain.bibliotecasUsuarios
                    .putIfAbsent(
                            nombreUsuario,
                            new ArrayList<>());

            List<Juego> biblioteca
                    = ServerMain.bibliotecasUsuarios
                            .get(nombreUsuario);

            for (Juego juegoExistente : biblioteca) {

                if (juegoExistente.getNombre()
                        .equalsIgnoreCase(nombreJuego)) {

                    try {

                        salida.writeObject(
                                new Mensaje(
                                        "Servidor",
                                        "Ya tienes ese juego"));

                    } catch (IOException e) {

                        System.out.println(
                                "Error enviando mensaje.");
                    }

                    return;
                }
            }

            Juego juego =
                    new Juego(nombreJuego);

            biblioteca.add(juego);

            Persistencia.guardarBibliotecas(ServerMain.bibliotecasUsuarios);

            try {

                salida.writeObject(
                        new Mensaje(
                                "Servidor",
                                "Juego agregado: "
                                + nombreJuego));

            } catch (IOException e) {

                System.out.println(
                        "Error enviando confirmación.");
            }
        }
    }

    private void enviarBiblioteca() {

        synchronized (ServerMain.bibliotecasUsuarios) {

            List<Juego> biblioteca
                    = ServerMain.bibliotecasUsuarios
                            .get(nombreUsuario);

            try {

                if (biblioteca == null
                        || biblioteca.isEmpty()) {

                    salida.writeObject(
                            new Mensaje(
                                    "Servidor",
                                    "Biblioteca vacía"));

                    return;
                }

                StringBuilder juegos
                        = new StringBuilder();

                for (Juego juego : biblioteca) {

                    juegos.append("- ")
                            .append(juego.getNombre())
                            .append("\n");
                }

                salida.writeObject(
                        new Mensaje(
                                "Servidor",
                                "\nBiblioteca:\n"
                                + juegos));

            } catch (IOException e) {

                System.out.println(
                        "Error enviando biblioteca.");
            }
        }
    }
    
    
    private void enviarUsuarios() {

        StringBuilder usuarios =
                new StringBuilder();

        synchronized (ServerMain.clientesConectados) {

            for (ManejadorCliente cliente
                    : ServerMain.clientesConectados) {

                if (cliente.nombreUsuario != null) {

                    usuarios.append("- ")
                            .append(cliente.nombreUsuario)
                            .append("\n");
                }
            }
        }

        try {

            salida.writeObject(
                    new Mensaje(
                            "Servidor",
                            "\nUsuarios conectados:\n"
                                    + usuarios));

        } catch (IOException e) {

            System.out.println(
                    "Error enviando usuarios.");
        }
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

            try {

                salida.writeObject(
                        new Mensaje(
                                "Servidor",
                                ayuda));

            } catch (IOException e) {

                System.out.println(
                        "Error enviando ayuda.");
            }
        }
}
