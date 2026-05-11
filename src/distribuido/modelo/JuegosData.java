package distribuido.modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JuegosData {
    public Map<String, JuegoRegistro> catalogo = new HashMap<>();
    public Map<String, Double> billeteras = new HashMap<>();
    public Map<String, List<String>> bibliotecas = new HashMap<>();
    public Map<String, ReservaRegistro> reservas = new HashMap<>();
}
