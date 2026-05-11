package distribuido.modelo;

public class TokenSesion {
    public String token;
    public String usuario;
    public String rol;
    public long creadoEnEpochMs;

    public TokenSesion() {
    }

    public TokenSesion(String token, String usuario, String rol, long creadoEnEpochMs) {
        this.token = token;
        this.usuario = usuario;
        this.rol = rol;
        this.creadoEnEpochMs = creadoEnEpochMs;
    }
}
