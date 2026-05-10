package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private Rol rol;
    private double saldo;
    private List<Juego> biblioteca;

    public Usuario(String nombre, Rol rol) {
        this.nombre = nombre;
        this.rol = rol;
        this.saldo = 0;
        this.biblioteca = new ArrayList<>();
    }

    public String getNombre() {
        return nombre;
    }

    public Rol getRol() {
        return rol;
    }

    public synchronized void setRol(Rol rol) {
        this.rol = rol;
    }

    public synchronized double getSaldo() {
        return saldo;
    }

    public synchronized void cargarSaldo(double monto) {
        if (monto > 0) {
            saldo += monto;
        }
    }

    public synchronized boolean descontarSaldo(double monto) {
        if (monto <= 0 || saldo < monto) {
            return false;
        }

        saldo -= monto;
        return true;
    }

    public synchronized boolean tieneJuego(String nombreJuego) {
        for (Juego juego : biblioteca) {
            if (juego.getNombre().equalsIgnoreCase(nombreJuego)) {
                return true;
            }
        }

        return false;
    }

    public synchronized void agregarJuego(Juego juego) {
        if (!tieneJuego(juego.getNombre())) {
            biblioteca.add(juego);
        }
    }

    public synchronized List<Juego> getBibliotecaCopia() {
        return new ArrayList<>(biblioteca);
    }
}
