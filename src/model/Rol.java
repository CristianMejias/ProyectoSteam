package model;

import java.io.Serializable;

public enum Rol implements Serializable {
    USUARIO,
    PUBLISHER,
    ADMIN;

    public static Rol desdeTexto(String texto) {
        if (texto == null) {
            return USUARIO;
        }

        switch (texto.trim().toLowerCase()) {
            case "publisher":
                return PUBLISHER;
            case "admin":
                return ADMIN;
            default:
                return USUARIO;
        }
    }
}
