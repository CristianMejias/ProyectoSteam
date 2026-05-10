package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {

    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;

    public static void main(String[] args) {

        try (

                Socket socket = new Socket(HOST, PUERTO);

                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                PrintWriter salida = new PrintWriter(
                        socket.getOutputStream(), true);

                Scanner scanner = new Scanner(System.in)) {

            System.out.println(entrada.readLine());

            while (true) {

                System.out.print("Escribe un mensaje: ");

                String mensaje = scanner.nextLine();

                salida.println(mensaje);

                String respuesta = entrada.readLine();

                System.out.println(respuesta);
            }

        } catch (IOException e) {

            System.out.println("Error de conexión: " + e.getMessage());
        }
    }
}