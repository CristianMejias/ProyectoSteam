package seguridad;

import protocolo.MensajeProtocolo;
import protocolo.Servicios;
import red.JsonSocket;

import java.io.IOException;
import java.util.Map;

public class ValidadorSesionRemoto {

    private final int[] puertosSesiones;

    public ValidadorSesionRemoto(int... puertosSesiones) {
        this.puertosSesiones = puertosSesiones;
    }

    public MensajeProtocolo validar(String token, String rolRequerido) {
        MensajeProtocolo req = MensajeProtocolo.request(Servicios.SESIONES, "VALIDAR_TOKEN");
        req.setToken(token);
        req.put("rolRequerido", rolRequerido);
        for (int puerto : puertosSesiones) {
            try {
                MensajeProtocolo res = JsonSocket.enviar("127.0.0.1", puerto, req, 1500);
                if (res != null) {
                    return res;
                }
            } catch (IOException ignored) {
                // Fallback al espejo del cluster de sesiones.
            }
        }
        return MensajeProtocolo.error(req, "No se pudo validar token: cluster de sesiones no disponible");
    }

    public static String usuarioDesde(MensajeProtocolo validacion) {
        Object usuario = validacion.getPayload().get("usuario");
        return usuario == null ? null : String.valueOf(usuario);
    }

    public static String rolDesde(MensajeProtocolo validacion) {
        Object rol = validacion.getPayload().get("rol");
        return rol == null ? null : String.valueOf(rol);
    }
}
