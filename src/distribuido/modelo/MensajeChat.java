package distribuido.modelo;

public class MensajeChat {
    public String id;
    public String de;
    public String para;
    public String texto;
    public long creadoEnEpochMs;

    public MensajeChat() {
    }

    public MensajeChat(String id, String de, String para, String texto, long creadoEnEpochMs) {
        this.id = id;
        this.de = de;
        this.para = para;
        this.texto = texto;
        this.creadoEnEpochMs = creadoEnEpochMs;
    }
}
