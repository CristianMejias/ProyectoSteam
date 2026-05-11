package persistencia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistencia HA: toda escritura se hace primero en Main y se replica de
 * inmediato a Copy dentro del mismo bloque synchronized.
 */
public class GestorPersistencia<T> {

    private final Path main;
    private final Path copy;
    private final Class<T> tipo;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public GestorPersistencia(String main, String copy, Class<T> tipo) {
        this.main = Path.of(main);
        this.copy = Path.of(copy);
        this.tipo = tipo;
        crearDirectorios();
    }

    public synchronized T leer(T valorInicial) {
        try {
            if (Files.exists(main) && Files.size(main) > 0) {
                return gson.fromJson(Files.readString(main, StandardCharsets.UTF_8), tipo);
            }
        } catch (Exception e) {
            System.out.println("No se pudo leer Main; recuperando desde Copy: " + e.getMessage());
        }

        try {
            if (Files.exists(copy) && Files.size(copy) > 0) {
                T recuperado = gson.fromJson(Files.readString(copy, StandardCharsets.UTF_8), tipo);
                escribir(recuperado);
                return recuperado;
            }
        } catch (Exception e) {
            System.out.println("No se pudo leer Copy; usando datos iniciales: " + e.getMessage());
        }

        escribir(valorInicial);
        return valorInicial;
    }

    public synchronized void escribir(T data) {
        try {
            crearDirectorios();
            String json = gson.toJson(data);
            Files.writeString(main, json, StandardCharsets.UTF_8);
            // Replicacion inmediata del archivo principal al respaldo.
            Files.writeString(copy, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Fallo de persistencia HA: " + e.getMessage(), e);
        }
    }

    private void crearDirectorios() {
        try {
            if (main.getParent() != null) {
                Files.createDirectories(main.getParent());
            }
            if (copy.getParent() != null) {
                Files.createDirectories(copy.getParent());
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron crear directorios de datos", e);
        }
    }
}
