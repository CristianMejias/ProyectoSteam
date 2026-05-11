package server;

import protocolo.MensajeProtocolo;
import protocolo.Servicios;
import red.JsonSocket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Proxy {

    private static final int PUERTO = 8080;
    private static final int TIMEOUT_MS = 2000;
    private final ExecutorService pool = Executors.newFixedThreadPool(32);
    private final Map<String, List<Nodo>> clusters = new HashMap<>();
    private final Map<String, AtomicInteger> roundRobin = new ConcurrentHashMap<>();

    public Proxy() {
        clusters.put(Servicios.SESIONES, List.of(new Nodo("127.0.0.1", 8081), new Nodo("127.0.0.1", 8181)));
        clusters.put(Servicios.JUEGOS, List.of(new Nodo("127.0.0.1", 8082), new Nodo("127.0.0.1", 8282)));
        clusters.put(Servicios.MENSAJERIA, List.of(new Nodo("127.0.0.1", 8083), new Nodo("127.0.0.1", 8383)));
        clusters.keySet().forEach(k -> roundRobin.put(k, new AtomicInteger()));
    }

    public static void main(String[] args) throws Exception {
        new Proxy().iniciar();
    }

    public void iniciar() throws Exception {
        Thread health = new Thread(this::healthChecks, "Proxy-HealthChecks");
        health.setDaemon(true);
        health.start();

        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("Proxy balanceador escuchando en puerto " + PUERTO);
            while (true) {
                Socket cliente = server.accept();
                pool.submit(() -> atender(cliente));
            }
        }
    }

    private void atender(Socket cliente) {
        try (cliente;
             BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(cliente.getOutputStream(), StandardCharsets.UTF_8))) {
            cliente.setSoTimeout(6000);
            MensajeProtocolo req = JsonSocket.leer(in);
            MensajeProtocolo res = reenviarConFailover(req);
            JsonSocket.escribir(out, res);
        } catch (Exception e) {
            System.out.println("Error Proxy: " + e.getMessage());
        }
    }

    private MensajeProtocolo reenviarConFailover(MensajeProtocolo req) {
        if (req == null || req.getServicio() == null) {
            return MensajeProtocolo.error(req, "Servicio no indicado");
        }
        List<Nodo> nodos = clusters.get(req.getServicio());
        if (nodos == null) {
            return MensajeProtocolo.error(req, "Servicio desconocido: " + req.getServicio());
        }

        int inicio = Math.floorMod(roundRobin.get(req.getServicio()).getAndIncrement(), nodos.size());
        for (int i = 0; i < nodos.size(); i++) {
            Nodo nodo = nodos.get((inicio + i) % nodos.size());
            if (!nodo.activo) {
                continue;
            }
            try {
                return JsonSocket.enviar(nodo.host, nodo.puerto, req, TIMEOUT_MS);
            } catch (Exception e) {
                nodo.activo = false;
                System.out.println("Nodo caido " + req.getServicio() + " " + nodo + ": " + e.getMessage());
            }
        }
        return MensajeProtocolo.error(req, "No hay nodos activos para " + req.getServicio());
    }

    private void healthChecks() {
        while (true) {
            try {
                Thread.sleep(3000);
                clusters.forEach((servicio, nodos) -> {
                    for (Nodo nodo : nodos) {
                        try {
                            MensajeProtocolo ping = MensajeProtocolo.request(servicio, "HEALTH");
                            MensajeProtocolo pong = JsonSocket.enviar(nodo.host, nodo.puerto, ping, 800);
                            nodo.activo = pong != null && pong.isExito();
                        } catch (Exception e) {
                            nodo.activo = false;
                        }
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static class Nodo {
        final String host;
        final int puerto;
        volatile boolean activo = true;

        Nodo(String host, int puerto) {
            this.host = host;
            this.puerto = puerto;
        }

        @Override
        public String toString() {
            return host + ":" + puerto;
        }
    }
}
