package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private String contenido;
    private String hora;

    public Mensaje(String usuario, String contenido) {
        this.usuario = usuario;
        this.contenido = contenido;
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm:ss");
        this.hora = LocalDateTime.now().format(formato);
    }

    public String getUsuario() {
        return usuario;
    }

    public String getContenido() {
        return contenido;
    }

    @Override
    public String toString() {
        return "[" + hora + "] " + usuario + ": " + contenido;
    }
}
