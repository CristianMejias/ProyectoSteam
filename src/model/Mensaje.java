package model;

import java.io.Serializable;

public class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private String contenido;

    public Mensaje(String usuario, String contenido) {
        this.usuario = usuario;
        this.contenido = contenido;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getContenido() {
        return contenido;
    }

    @Override
    public String toString() {
        return usuario + ": " + contenido;
    }
}