package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socketCliente;

    public ClientHandler(Socket socketCliente) {
        this.socketCliente = socketCliente;
    }

    @Override
    public void run() {

        try {

            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socketCliente.getInputStream()));

            PrintWriter salida = new PrintWriter(
                    socketCliente.getOutputStream(), true);

            salida.println("Conectado al servidor de ProyectoSteam");

            String mensaje;

            while ((mensaje = entrada.readLine()) != null) {

                System.out.println("Mensaje recibido: " + mensaje);

                salida.println("Servidor recibió: " + mensaje);
            }

        } catch (IOException e) {

            System.out.println("Cliente desconectado.");

        } finally {

            try {
                socketCliente.close();
            } catch (IOException e) {
                System.out.println("Error al cerrar socket.");
            }
        }
    }
}