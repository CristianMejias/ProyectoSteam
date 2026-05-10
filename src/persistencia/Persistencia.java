package persistencia;

import model.Juego;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Persistencia {

    private static final String ARCHIVO =
            "bibliotecas.dat";

    // Guardar datos
    public static void guardarBibliotecas(
            Map<String, List<Juego>> bibliotecas) {

        try (

                ObjectOutputStream salida =
                        new ObjectOutputStream(
                                new FileOutputStream(ARCHIVO))
        ) {

            salida.writeObject(bibliotecas);

            System.out.println(
                    "Bibliotecas guardadas.");

        } catch (IOException e) {

            System.out.println(
                    "Error guardando datos.");
        }
    }

    // Cargar datos
    @SuppressWarnings("unchecked")
    public static Map<String, List<Juego>>
    cargarBibliotecas() {

        File archivo =
                new File(ARCHIVO);

        // Si no existe archivo
        if (!archivo.exists()) {

            return new HashMap<>();
        }

        try (

                ObjectInputStream entrada =
                        new ObjectInputStream(
                                new FileInputStream(ARCHIVO))
        ) {

            return (Map<String, List<Juego>>)
                    entrada.readObject();

        } catch (IOException | ClassNotFoundException e) {

            System.out.println(
                    "Error cargando datos.");

            return new HashMap<>();
        }
    }
}