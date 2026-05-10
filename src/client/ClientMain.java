package client;

import model.Mensaje;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {

    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;

    public static void main(String[] args) {

        try (

                Socket socket = new Socket(HOST, PUERTO);

                ObjectOutputStream salida =
                        new ObjectOutputStream(socket.getOutputStream());

                ObjectInputStream entrada =
                        new ObjectInputStream(socket.getInputStream());

                Scanner scanner = new Scanner(System.in)
        ) {

            Mensaje bienvenida =
                    (Mensaje) entrada.readObject();

            System.out.println(bienvenida);

            System.out.print("Ingresa tu nombre: ");
            String usuario = scanner.nextLine();

            while (true) {

                System.out.print("Mensaje: ");

                String texto = scanner.nextLine();

                Mensaje mensaje =
                        new Mensaje(usuario, texto);

                salida.writeObject(mensaje);

                Mensaje respuesta =
                        (Mensaje) entrada.readObject();

                System.out.println(respuesta);
            }

        } catch (IOException | ClassNotFoundException e) {

            System.out.println(
                    "Error de conexión: "
                            + e.getMessage());
        }
    }
}