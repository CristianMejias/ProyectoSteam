package persistencia;

import model.Usuario;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import model.Juego;


public class Persistencia {

    private static final String ARCHIVO_USUARIOS = "usuarios.dat";
    private static final String ARCHIVO_CATALOGO = "catalogo.dat";

    public static synchronized void guardarUsuarios(Map<String, Usuario> usuarios) {
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(ARCHIVO_USUARIOS))) {
            salida.writeObject(new ConcurrentHashMap<>(usuarios));
        } catch (IOException e) {
            System.out.println("Error guardando usuarios.");
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized Map<String, Usuario> cargarUsuarios() {
        File archivo = new File(ARCHIVO_USUARIOS);

        if (!archivo.exists()) {
            return new ConcurrentHashMap<>();
        }

        try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(ARCHIVO_USUARIOS))) {
            return (Map<String, Usuario>) entrada.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error cargando usuarios.");
            return new ConcurrentHashMap<>();
        }
    }
    
    public static void guardarCatalogo(Map<String, Juego> catalogo) {
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(ARCHIVO_CATALOGO))) {
            salida.writeObject(catalogo);
            System.out.println("Catálogo guardado.");
        } catch (IOException e) {
            System.out.println("Error guardando catálogo.");
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Juego> cargarCatalogo() {
        File archivo = new File(ARCHIVO_CATALOGO);

        if (!archivo.exists()) {
            return new ConcurrentHashMap<>();
        }

        try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(ARCHIVO_CATALOGO))) {
            return (Map<String, Juego>) entrada.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error cargando catálogo.");
            return new ConcurrentHashMap<>();
        }
    }
}
