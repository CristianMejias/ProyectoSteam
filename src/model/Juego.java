package model;

import java.io.Serializable;

public class Juego implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;

    public Juego(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}