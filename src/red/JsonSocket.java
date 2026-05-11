package red;

import com.google.gson.Gson;
import protocolo.MensajeProtocolo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class JsonSocket {

    private static final Gson GSON = new Gson();

    private JsonSocket() {
    }

    public static MensajeProtocolo enviar(String host, int puerto, MensajeProtocolo mensaje, int timeoutMs) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            escribir(out, mensaje);
            String linea = in.readLine();
            if (linea == null) {
                throw new IOException("Conexion cerrada sin respuesta");
            }
            return leer(linea);
        }
    }

    public static MensajeProtocolo leer(BufferedReader in) throws IOException {
        String linea = in.readLine();
        if (linea == null) {
            return null;
        }
        return leer(linea);
    }

    public static MensajeProtocolo leer(String lineaJson) {
        return GSON.fromJson(lineaJson, MensajeProtocolo.class);
    }

    public static void escribir(BufferedWriter out, MensajeProtocolo mensaje) throws IOException {
        out.write(GSON.toJson(mensaje));
        out.write('\n');
        out.flush();
    }
}
