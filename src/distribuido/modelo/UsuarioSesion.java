package distribuido.modelo;

public class UsuarioSesion {
    public String usuario;
    public String passwordHash;
    public String rol;

    public UsuarioSesion() {
    }

    public UsuarioSesion(String usuario, String passwordHash, String rol) {
        this.usuario = usuario;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }
}
