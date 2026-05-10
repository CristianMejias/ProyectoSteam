package server;

import model.Juego;
import model.Mensaje;
import model.Reserva;
import model.Usuario;
import persistencia.Persistencia;
import utils.Constantes;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerMain {

    private static final int PUERTO = Constantes.PUERTO;

    public static List<ManejadorCliente> clientesConectados = new CopyOnWriteArrayList<>();
    public static Map<String, ManejadorCliente> clientesPorNombre = new ConcurrentHashMap<>();
    public static Map<String, Usuario> usuariosRegistrados = new ConcurrentHashMap<>();
    public static Set<String> usuariosActivos = ConcurrentHashMap.newKeySet();
    public static Map<String, Juego> catalogo = new ConcurrentHashMap<>();
    public static Map<String, Reserva> reservasPendientes = new ConcurrentHashMap<>();
    public static Map<String, Queue<Mensaje>> mensajesPendientes = new ConcurrentHashMap<>();
    public static ScheduledExecutorService planificador = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) {
        System.out.println("""

                ====================================
                      SERVIDOR PROYECTO STEAM
                ====================================
                """);

        usuariosRegistrados = new ConcurrentHashMap<>(Persistencia.cargarUsuarios());
        System.out.println("Usuarios cargados: " + usuariosRegistrados.size());

        catalogo = new ConcurrentHashMap<>(Persistencia.cargarCatalogo());
        precargarJuegosSiNoExisten();
        System.out.println("Juegos cargados: " + catalogo.size());

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor escuchando en puerto " + PUERTO);

            while (true) {
                Socket socketCliente = servidor.accept();
                System.out.println("Nuevo cliente conectado: " + socketCliente.getInetAddress());

                ManejadorCliente manejador = new ManejadorCliente(socketCliente);
                clientesConectados.add(manejador);

                Thread hilo = new Thread(manejador);
                hilo.start();
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }
    
    private static void precargarJuegosSiNoExisten() {
        if (!catalogo.isEmpty()) {
            return;
        }

        catalogo.put("minecraft", new Juego("Minecraft", 12000.0, "Mojang", 10, false));
        catalogo.put("terraria", new Juego("Terraria", 8000.0, "Re-Logic", 8, false));
        catalogo.put("portal", new Juego("Portal", 7000.0, "Valve", 6, false));
        catalogo.put("gtav", new Juego("GTA V", 9500.0, "rockstar Games", 5, true));

        Persistencia.guardarCatalogo(catalogo);
        System.out.println("Catálogo inicial creado con 4 juegos.");
    }
}
