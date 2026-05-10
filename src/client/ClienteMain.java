package client;

import model.Mensaje;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ClienteMain {

    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;

    public static void main(String[] args) {

        try {

            Socket socket =
                    new Socket(HOST, PUERTO);

            ObjectOutputStream salida =
                    new ObjectOutputStream(
                            socket.getOutputStream());

            ObjectInputStream entrada =
                    new ObjectInputStream(
                            socket.getInputStream());

            Scanner scanner =
                    new Scanner(System.in);

            Mensaje bienvenida =
                    (Mensaje) entrada.readObject();

            System.out.println(bienvenida);

            System.out.print("Ingresa tu nombre: ");
            String usuario =
                    scanner.nextLine();

            // Hilo que escucha mensajes
            Thread hiloLectura =
                    new Thread(() -> {

                        try {

                            while (true) {

                                Mensaje mensaje =
                                        (Mensaje) entrada.readObject();

                                System.out.println(mensaje);
                                
                                if (mensaje.getContenido()
                                        .equals("El nombre ya está en uso")) {

                                    System.out.println(
                                            "Cerrando cliente...");

                                    socket.close();

                                    System.exit(0);
                                }
                            }

                        } catch (IOException | ClassNotFoundException e) {

                            System.out.println(
                                    "Desconectado del servidor.");
                        }
                    });

            hiloLectura.start();

            // Enviar mensajes
            while (true) {

                String texto =
                        scanner.nextLine();

                Mensaje mensaje =
                        new Mensaje(usuario, texto);

                salida.writeObject(mensaje);

                // Salir correctamente
                if (texto.equals("/salir")) {

                    System.out.println(
                            "Desconectando del servidor...");

                    socket.close();

                    break;
                }
            }
            
            // Finalizar programa
            System.exit(0);

        } catch (IOException | ClassNotFoundException e) {

            System.out.println(
                    "Error de conexión: "
                            + e.getMessage());
        }
    }
}