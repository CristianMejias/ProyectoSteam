package distribuido.modelo;

public class ReservaRegistro {
    public String reservaId;
    public String usuario;
    public String juegoId;
    public long venceEnEpochMs;

    public ReservaRegistro() {
    }

    public ReservaRegistro(String reservaId, String usuario, String juegoId, long venceEnEpochMs) {
        this.reservaId = reservaId;
        this.usuario = usuario;
        this.juegoId = juegoId;
        this.venceEnEpochMs = venceEnEpochMs;
    }
}
