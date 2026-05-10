package cliente;

import model.Mensaje;
import utils.Constantes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ClienteMain {

    private static final String HOST = Constantes.HOST;
    private static final int PUERTO = Constantes.PUERTO;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(HOST, PUERTO);
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in);

            System.out.println("""

                    ====================================
                            Cliente STEAM
                     Sistema Distribuido en Java TCP
                    ====================================
                    Roles disponibles: usuario, publisher, admin
                    """);

            System.out.print("Ingresa tu nombre: ");
            String usuario = scanner.nextLine().trim();

            if (usuario.isEmpty()) {
                System.out.println("El nombre no puede estar vacío.");
                socket.close();
                return;
            }

            System.out.print("Ingresa tu rol: ");
            String rol = scanner.nextLine().trim();

            Thread hiloLectura = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        Mensaje mensaje = (Mensaje) entrada.readObject();
                        System.out.println(mensaje);

                        if (mensaje.getContenido().equals("El nombre ya está en uso")) {
                            socket.close();
                            System.exit(0);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Desconectado del servidor.");
                }
            });

            hiloLectura.start();
            salida.writeObject(new Mensaje(usuario, "/entrar " + rol));
            salida.flush();

            while (!socket.isClosed()) {
                String texto = scanner.nextLine().trim();

                if (texto.isEmpty()) {
                    continue;
                }

                salida.writeObject(new Mensaje(usuario, texto));
                salida.flush();

                if (texto.equals("/salir")) {
                    System.out.println("Desconectando del servidor...");
                    socket.close();
                    break;
                }
            }

            System.exit(0);
        } catch (IOException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
    }
}
