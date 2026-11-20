package bo.edu.uagrm.talentos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public final class Bitacora implements AutoCloseable {
    private final FileHandler acceso;
    private final FileHandler errores;

    public Bitacora(Path directorio) throws IOException {
        Files.createDirectories(directorio);
        acceso = handler(directorio.resolve("access.log"));
        try {
            errores = handler(directorio.resolve("error.log"));
        } catch (IOException e) {
            acceso.close();
            throw e;
        }
    }

    private static FileHandler handler(Path ruta) throws IOException {
        var salida = new FileHandler(ruta.toString(), true);
        salida.setEncoding("UTF-8");
        salida.setFormatter(new Formatter() {
            @Override public String format(LogRecord r) {
                return Instant.ofEpochMilli(r.getMillis()) + " " + r.getMessage() + System.lineSeparator();
            }
        });
        return salida;
    }

    public void acceso(String evento) {
        acceso.publish(new LogRecord(Level.INFO, evento));
    }

    public void error(String evento) {
        errores.publish(new LogRecord(Level.WARNING, evento));
    }

    @Override public void close() {
        acceso.close();
        errores.close();
    }
}
