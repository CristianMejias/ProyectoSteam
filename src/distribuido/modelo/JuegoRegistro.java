package distribuido.modelo;

public class JuegoRegistro {
    public String id;
    public String nombre;
    public String vendedor;
    public double precio;
    public int stock;

    public JuegoRegistro() {
    }

    public JuegoRegistro(String id, String nombre, String vendedor, double precio, int stock) {
        this.id = id;
        this.nombre = nombre;
        this.vendedor = vendedor;
        this.precio = precio;
        this.stock = stock;
    }
}
