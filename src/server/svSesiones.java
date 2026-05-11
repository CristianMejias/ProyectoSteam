package server;

import distribuido.modelo.SesionesData;
import distribuido.modelo.TokenSesion;
import distribuido.modelo.UsuarioSesion;
import persistencia.GestorPersistencia;
import protocolo.MensajeProtocolo;
import protocolo.Servicios;
import red.JsonSocket;
import seguridad.HashUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class svSesiones {

    private static final int THREADS = 16;
    private final int puerto;
    private final GestorPersistencia<SesionesData> persistencia;
    private final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    private SesionesData data;
    private final SecureRandom random = new SecureRandom();

    public svSesiones(int puerto) {
        this.puerto = puerto;
        this.persistencia = new GestorPersistencia<>("data/SES_Main.txt", "data/SES_Copy.txt", SesionesData.class);
        this.data = persistencia.leer(datosIniciales());
    }

    public static void main(String[] args) throws Exception {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
        new svSesiones(puerto).iniciar();
    }

    public void iniciar() throws Exception {
        try (ServerSocket server = new ServerSocket(puerto)) {
            System.out.println("svSesiones escuchando en puerto " + puerto);
            while (true) {
                Socket socket = server.accept();
                pool.submit(() -> atender(socket));
            }
        }
    }

    private void atender(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(5000);
            MensajeProtocolo req = JsonSocket.leer(in);
            MensajeProtocolo res = procesar(req);
            JsonSocket.escribir(out, res);
        } catch (Exception e) {
            System.out.println("Error svSesiones: " + e.getMessage());
        }
    }

    private MensajeProtocolo procesar(MensajeProtocolo req) {
        if (req == null) {
            return MensajeProtocolo.error(null, "Solicitud vacia");
        }
        if ("HEALTH".equalsIgnoreCase(req.getOperacion())) {
            return MensajeProtocolo.ok(req, "OK");
        }
        return switch (req.getOperacion()) {
            case "LOGIN" -> login(req);
            case "VALIDAR_TOKEN" -> validar(req);
            case "CREAR_USUARIO" -> crearUsuario(req);
            default -> MensajeProtocolo.error(req, "Operacion de sesiones no soportada: " + req.getOperacion());
        };
    }

    private synchronized MensajeProtocolo login(MensajeProtocolo req) {
        String usuario = req.texto("usuario");
        String password = req.texto("password");
        UsuarioSesion registro = data.usuarios.get(usuario);
        if (registro == null || !registro.passwordHash.equals(HashUtil.sha256(password))) {
            return MensajeProtocolo.error(req, "Credenciales invalidas");
        }
        String token = nuevoToken();
        data.tokens.put(token, new TokenSesion(token, usuario, registro.rol, System.currentTimeMillis()));
        persistencia.escribir(data);

        MensajeProtocolo res = MensajeProtocolo.ok(req, "Login exitoso");
        res.put("token", token);
        res.put("usuario", usuario);
        res.put("rol", registro.rol);
        return res;
    }

    private synchronized MensajeProtocolo validar(MensajeProtocolo req) {
        TokenSesion token = data.tokens.get(req.getToken());
        if (token == null) {
            return MensajeProtocolo.error(req, "Token invalido");
        }
        String requerido = req.texto("rolRequerido");
        if (requerido != null && !requerido.isBlank() && !"ANY".equals(requerido)) {
            Set<String> aceptados = Set.of(requerido.split(","));
            if (!aceptados.contains(token.rol)) {
                return MensajeProtocolo.error(req, "Rol insuficiente: requiere " + requerido);
            }
        }
        MensajeProtocolo res = MensajeProtocolo.ok(req, "Token valido");
        res.put("usuario", token.usuario);
        res.put("rol", token.rol);
        return res;
    }

    private synchronized MensajeProtocolo crearUsuario(MensajeProtocolo req) {
        MensajeProtocolo validacion = validar(req);
        if (!validacion.isExito() || !"ADMIN".equals(validacion.texto("rol"))) {
            return MensajeProtocolo.error(req, "Solo ADMIN puede crear usuarios");
        }
        String usuario = req.texto("usuarioNuevo");
        String password = req.texto("passwordNuevo");
        String rol = req.texto("rolNuevo");
        if (usuario == null || password == null || rol == null) {
            return MensajeProtocolo.error(req, "Faltan campos de usuario");
        }
        data.usuarios.put(usuario, new UsuarioSesion(usuario, HashUtil.sha256(password), rol.toUpperCase()));
        persistencia.escribir(data);
        return MensajeProtocolo.ok(req, "Usuario creado");
    }

    private String nuevoToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static SesionesData datosIniciales() {
        SesionesData datos = new SesionesData();
        datos.usuarios.put("comprador", new UsuarioSesion("comprador", HashUtil.sha256("1234"), "COMPRADOR"));
        datos.usuarios.put("vendedor", new UsuarioSesion("vendedor", HashUtil.sha256("1234"), "VENDEDOR"));
        datos.usuarios.put("admin", new UsuarioSesion("admin", HashUtil.sha256("admin"), "ADMIN"));
        return datos;
    }
}
