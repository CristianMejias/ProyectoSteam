package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerMain {

    private static final int PUERTO = 5000;

    // Lista compartida entre hilos
    public static List<ManejadorCliente> clientesConectados =
            new ArrayList<>();

    public static void main(String[] args) {

        System.out.println("=== SERVIDOR STEAM INICIADO ===");

        try (ServerSocket servidor =
                     new ServerSocket(PUERTO)) {

            System.out.println(
                    "Servidor escuchando en puerto "
                            + PUERTO);

            while (true) {

                Socket socketCliente =
                        servidor.accept();

                System.out.println(
                        "Nuevo cliente conectado: "
                                + socketCliente.getInetAddress());

                ManejadorCliente manejador =
                        new ManejadorCliente(socketCliente);

                synchronized (clientesConectados) {
                    clientesConectados.add(manejador);
                }

                Thread hilo =
                        new Thread(manejador);

                hilo.start();
            }

        } catch (IOException e) {

            System.out.println(
                    "Error en el servidor: "
                            + e.getMessage());
        }
    }
}