package cliente;

import protocolo.MensajeProtocolo;
import protocolo.Servicios;
import red.JsonSocket;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ClienteDistribuido {

    private static final String HOST_PROXY = "127.0.0.1";
    private static final int PUERTO_PROXY = 8080;
    private static String token;
    private static String usuario;
    private static String rol;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("""

                ====================================
                    Cliente Steam Distribuido
                ====================================
                Usuarios demo:
                comprador / 1234
                vendedor  / 1234
                admin     / admin
                """);

        if (!login(sc)) {
            return;
        }

        while (true) {
            System.out.println("""

                    1. Listar juegos
                    2. Cargar billetera
                    3. Reservar juego
                    4. Confirmar compra
                    5. Publicar juego
                    6. Enviar mensaje
                    7. Leer mensajes pendientes
                    0. Salir
                    """);
            System.out.print("Opcion: ");
            String opcion = sc.nextLine().trim();
            try {
                switch (opcion) {
                    case "1" -> listarJuegos();
                    case "2" -> cargarBilletera(sc);
                    case "3" -> reservar(sc);
                    case "4" -> confirmar(sc);
                    case "5" -> publicar(sc);
                    case "6" -> enviarMensaje(sc);
                    case "7" -> leerMensajes();
                    case "0" -> {
                        System.out.println("Hasta luego.");
                        return;
                    }
                    default -> System.out.println("Opcion no valida.");
                }
            } catch (Exception e) {
                System.out.println("Error de red: " + e.getMessage());
            }
        }
    }

    private static boolean login(Scanner sc) {
        try {
            System.out.print("Usuario: ");
            String user = sc.nextLine().trim();
            System.out.print("Password: ");
            String pass = sc.nextLine().trim();

            MensajeProtocolo req = MensajeProtocolo.request(Servicios.SESIONES, "LOGIN");
            req.put("usuario", user);
            req.put("password", pass);
            MensajeProtocolo res = enviar(req);
            imprimirRespuesta(res);
            if (res.isExito()) {
                token = String.valueOf(res.getPayload().get("token"));
                usuario = String.valueOf(res.getPayload().get("usuario"));
                rol = String.valueOf(res.getPayload().get("rol"));
                System.out.println("Sesion: " + usuario + " (" + rol + ")");
                return true;
            }
        } catch (Exception e) {
            System.out.println("No se pudo iniciar sesion: " + e.getMessage());
        }
        return false;
    }

    private static void listarJuegos() throws Exception {
        MensajeProtocolo req = MensajeProtocolo.request(Servicios.JUEGOS, "LISTAR_JUEGOS");
        MensajeProtocolo res = enviar(req);
        imprimirRespuesta(res);
        Object catalogo = res.getPayload().get("catalogo");
        if (catalogo instanceof List<?> lista) {
            for (Object item : lista) {
                if (item instanceof Map<?, ?> juego) {
                    System.out.printf("- %s | %s | $%.0f | stock %.0f | vendedor %s%n",
                            juego.get("id"), juego.get("nombre"), asDouble(juego.get("precio")),
                            asDouble(juego.get("stock")), juego.get("vendedor"));
                }
            }
        }
    }

    private static void cargarBilletera(Scanner sc) throws Exception {
        MensajeProtocolo req = autenticada(Servicios.JUEGOS, "CARGAR_BILLETERA");
        System.out.print("Monto: ");
        req.put("monto", Double.parseDouble(sc.nextLine().trim()));
        imprimirRespuesta(enviar(req));
    }

    private static void reservar(Scanner sc) throws Exception {
        MensajeProtocolo req = autenticada(Servicios.JUEGOS, "RESERVAR_JUEGO");
        System.out.print("ID juego: ");
        req.put("juegoId", sc.nextLine().trim());
        MensajeProtocolo res = enviar(req);
        imprimirRespuesta(res);
        if (res.isExito()) {
            System.out.println("Reserva ID para confirmar: " + res.getPayload().get("reservaId"));
        }
    }

    private static void confirmar(Scanner sc) throws Exception {
        MensajeProtocolo req = autenticada(Servicios.JUEGOS, "CONFIRMAR_COMPRA");
        System.out.print("Reserva ID: ");
        req.put("reservaId", sc.nextLine().trim());
        imprimirRespuesta(enviar(req));
    }

    private static void publicar(Scanner sc) throws Exception {
        MensajeProtocolo req = autenticada(Servicios.JUEGOS, "PUBLICAR_JUEGO");
        System.out.print("ID juego: ");
        req.put("juegoId", sc.nextLine().trim());
        System.out.print("Nombre: ");
        req.put("nombre", sc.nextLine().trim());
        System.out.print("Precio: ");
        req.put("precio", Double.parseDouble(sc.nextLine().trim()));
        System.out.print("Stock: ");
        req.put("stock", Integer.parseInt(sc.nextLine().trim()));
        imprimirRespuesta(enviar(req));
    }

    private static void enviarMensaje(Scanner sc) throws Exception {
        MensajeProtocolo req = autenticada(Servicios.MENSAJERIA, "ENVIAR_MENSAJE");
        System.out.print("Para: ");
        req.put("para", sc.nextLine().trim());
        System.out.print("Texto: ");
        req.put("texto", sc.nextLine());
        imprimirRespuesta(enviar(req));
    }

    private static void leerMensajes() throws Exception {
        MensajeProtocolo req = autenticada(Servicios.MENSAJERIA, "LEER_MENSAJES");
        MensajeProtocolo res = enviar(req);
        imprimirRespuesta(res);
        Object mensajes = res.getPayload().get("mensajes");
        if (mensajes instanceof List<?> lista && !lista.isEmpty()) {
            for (Object item : lista) {
                if (item instanceof Map<?, ?> msg) {
                    System.out.println(msg.get("de") + " -> " + msg.get("para") + ": " + msg.get("texto"));
                }
            }
        } else {
            System.out.println("No hay mensajes pendientes.");
        }
    }

    private static MensajeProtocolo autenticada(String servicio, String operacion) {
        MensajeProtocolo req = MensajeProtocolo.request(servicio, operacion);
        req.setToken(token);
        return req;
    }

    private static MensajeProtocolo enviar(MensajeProtocolo req) throws Exception {
        return JsonSocket.enviar(HOST_PROXY, PUERTO_PROXY, req, 5000);
    }

    private static void imprimirRespuesta(MensajeProtocolo res) {
        System.out.println((res.isExito() ? "OK: " : "ERROR: ") + res.getMensaje());
        System.out.println("requestId=" + res.getRequestId());
    }

    private static double asDouble(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(valor));
    }
}
