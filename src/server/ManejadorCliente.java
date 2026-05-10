package server;

import model.Mensaje;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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

                enviarATodos(mensaje);
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
}