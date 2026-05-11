package protocolo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * POJO base del protocolo de red. Cada instancia se serializa como JSON con
 * Gson y viaja por TCP en una linea terminada en '\n'.
 */
public class MensajeProtocolo {

    private String requestId;
    private String servicio;
    private String operacion;
    private String token;
    private String origen;
    private boolean exito;
    private String mensaje;
    private Map<String, Object> payload;

    public MensajeProtocolo() {
        this.requestId = UUID.randomUUID().toString();
        this.payload = new HashMap<>();
    }

    public static MensajeProtocolo request(String servicio, String operacion) {
        MensajeProtocolo msg = new MensajeProtocolo();
        msg.servicio = servicio;
        msg.operacion = operacion;
        msg.origen = "CLIENTE";
        return msg;
    }

    public static MensajeProtocolo ok(MensajeProtocolo req, String mensaje) {
        MensajeProtocolo res = baseRespuesta(req, true, mensaje);
        return res;
    }

    public static MensajeProtocolo error(MensajeProtocolo req, String mensaje) {
        MensajeProtocolo res = baseRespuesta(req, false, mensaje);
        return res;
    }

    private static MensajeProtocolo baseRespuesta(MensajeProtocolo req, boolean exito, String mensaje) {
        MensajeProtocolo res = new MensajeProtocolo();
        res.requestId = req != null ? req.requestId : UUID.randomUUID().toString();
        res.servicio = req != null ? req.servicio : null;
        res.operacion = req != null ? req.operacion : null;
        res.token = req != null ? req.token : null;
        res.origen = "SERVIDOR";
        res.exito = exito;
        res.mensaje = mensaje;
        return res;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
    }

    public String getOperacion() {
        return operacion;
    }

    public void setOperacion(String operacion) {
        this.operacion = operacion;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Map<String, Object> getPayload() {
        if (payload == null) {
            payload = new HashMap<>();
        }
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public void put(String clave, Object valor) {
        getPayload().put(clave, valor);
    }

    public String texto(String clave) {
        Object valor = getPayload().get(clave);
        return valor == null ? null : String.valueOf(valor);
    }

    public double numero(String clave) {
        Object valor = getPayload().get(clave);
        if (valor instanceof Number n) {
            return n.doubleValue();
        }
        return valor == null ? 0 : Double.parseDouble(String.valueOf(valor));
    }
}
