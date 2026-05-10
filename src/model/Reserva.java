package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private String juego;
    private LocalDateTime vencimiento;
    private boolean activa;

    public Reserva(String usuario, String juego, LocalDateTime vencimiento) {
        this.usuario = usuario;
        this.juego = juego;
        this.vencimiento = vencimiento;
        this.activa = true;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getJuego() {
        return juego;
    }

    public LocalDateTime getVencimiento() {
        return vencimiento;
    }

    public synchronized boolean isActiva() {
        return activa;
    }

    public synchronized void cerrar() {
        activa = false;
    }

    @Override
    public synchronized String toString() {
        String estado = activa ? "activa" : "cerrada";
        return usuario + " -> " + juego + " | vence: " + vencimiento + " | " + estado;
    }
}
