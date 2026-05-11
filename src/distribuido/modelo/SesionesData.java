package distribuido.modelo;

import java.util.HashMap;
import java.util.Map;

public class SesionesData {
    public Map<String, UsuarioSesion> usuarios = new HashMap<>();
    public Map<String, TokenSesion> tokens = new HashMap<>();
}
