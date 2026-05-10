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

                Mensaje mensaje =
                        (Mensaje) entrada.readObject();

                nombreUsuario =
                        mensaje.getUsuario();

                System.out.println(
                        "Mensaje recibido -> "
                                + mensaje);

                String contenido =
                        mensaje.getContenido();

                if (contenido.startsWith("/comprar ")) {

                    comprarJuego(mensaje);

                } else if (contenido.equals("/biblioteca")) {

                    enviarBiblioteca();

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

        String nombreJuego =
                mensaje.getContenido()
                        .replace("/comprar ", "");

        synchronized (ServerMain.bibliotecasUsuarios) {

            ServerMain.bibliotecasUsuarios
                    .putIfAbsent(
                            nombreUsuario,
                            new ArrayList<>());

            List<Juego> biblioteca =
                    ServerMain.bibliotecasUsuarios
                            .get(nombreUsuario);

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

            List<Juego> biblioteca =
                    ServerMain.bibliotecasUsuarios
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

                StringBuilder juegos =
                        new StringBuilder();

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
}