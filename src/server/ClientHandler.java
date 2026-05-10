package server;

import model.Mensaje;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socketCliente;

    public ClientHandler(Socket socketCliente) {
        this.socketCliente = socketCliente;
    }

    @Override
    public void run() {

        try {

            ObjectOutputStream salida =
                    new ObjectOutputStream(socketCliente.getOutputStream());

            ObjectInputStream entrada =
                    new ObjectInputStream(socketCliente.getInputStream());

            salida.writeObject(
                    new Mensaje(
                            "Servidor",
                            "Conectado a ProyectoSteam"));

            while (true) {

                Mensaje mensaje =
                        (Mensaje) entrada.readObject();

                System.out.println(
                        "Mensaje recibido -> "
                                + mensaje);

                Mensaje respuesta =
                        new Mensaje(
                                "Servidor",
                                "Recibido correctamente");

                salida.writeObject(respuesta);
            }

        } catch (IOException | ClassNotFoundException e) {

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