package model;

import java.io.Serializable;

public class Juego implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private double precio;
    private String publisher;
    private int stockTotal;
    private int stockDisponible;
    private boolean soloReserva;

    public Juego(String nombre, double precio, String publisher, int stock, boolean soloReserva) {
        this.nombre = nombre;
        this.precio = precio;
        this.publisher = publisher;
        this.stockTotal = stock;
        this.stockDisponible = stock;
        this.soloReserva = soloReserva;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public String getPublisher() {
        return publisher;
    }

    public boolean isSoloReserva() {
        return soloReserva;
    }

    public synchronized int getStockTotal() {
        return stockTotal;
    }

    public synchronized int getStockDisponible() {
        return stockDisponible;
    }

    public synchronized boolean bloquearCopia() {
        if (stockDisponible <= 0) {
            return false;
        }

        stockDisponible--;
        return true;
    }

    public synchronized void liberarCopia() {
        if (stockDisponible < stockTotal) {
            stockDisponible++;
        }
    }

    public synchronized void agregarStock(int cantidad) {
        if (cantidad > 0) {
            stockTotal += cantidad;
            stockDisponible += cantidad;
        }
    }

    @Override
    public synchronized String toString() {
        String tipo = soloReserva ? "reserva" : "venta";
        return nombre + " | $" + precio + " | publisher: " + publisher + " | stock: " + stockDisponible + "/" + stockTotal + " | " + tipo;
    }
}
