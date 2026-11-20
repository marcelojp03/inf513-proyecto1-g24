package bo.edu.uagrm.talentos.comando;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Analizador {
    public record Comando(String nombre, List<String> parametros) {}

    private final String fuente;
    private int posicion;

    private Analizador(String fuente) {
        this.fuente = fuente;
    }

    public static Comando analizar(String asunto) {
        if (asunto == null || asunto.isBlank()) throw sintaxis("El asunto está vacío.");
        if (asunto.length() > 3200) throw sintaxis("El asunto supera el máximo permitido.");
        return new Analizador(asunto).leer();
    }

    private Comando leer() {
        espacios();
        int inicio = posicion;
        while (posicion < fuente.length() && fuente.charAt(posicion) < 128
                && (Character.isLetterOrDigit(fuente.charAt(posicion)) || fuente.charAt(posicion) == '_')) {
            posicion++;
        }
        String nombre = fuente.substring(inicio, posicion);
        if (!nombre.matches("[a-zA-Z][a-zA-Z0-9_]{0,23}")) throw sintaxis("Nombre de comando inválido.");
        espacios();
        exigir('[');
        espacios();
        List<String> parametros = new ArrayList<>();
        if (!tomar(']')) {
            do {
                espacios();
                parametros.add(cadena());
                espacios();
            } while (tomar(','));
            exigir(']');
        }
        espacios();
        if (posicion != fuente.length()) throw sintaxis("Hay texto después del corchete de cierre.");
        return new Comando(nombre.toUpperCase(Locale.ROOT), List.copyOf(parametros));
    }

    private String cadena() {
        exigir('"');
        StringBuilder valor = new StringBuilder();
        while (posicion < fuente.length()) {
            char c = fuente.charAt(posicion++);
            if (c == '"') return valor.toString();
            if (c == '\\') {
                if (posicion == fuente.length()) throw sintaxis("Escape incompleto.");
                c = fuente.charAt(posicion++);
                if (c != '"' && c != '\\') throw sintaxis("Solo se pueden escapar comillas dobles y barras invertidas.");
            }
            if (Character.isISOControl(c)) throw sintaxis("Hay un carácter de control en un parámetro.");
            valor.append(c);
        }
        throw sintaxis("Falta cerrar una cadena entre comillas dobles.");
    }

    private void espacios() {
        while (posicion < fuente.length()) {
            char c = fuente.charAt(posicion);
            if (c != '\t' && !Character.isSpaceChar(c)) return;
            posicion++;
        }
    }

    private boolean tomar(char c) {
        if (posicion == fuente.length() || fuente.charAt(posicion) != c) return false;
        posicion++;
        return true;
    }

    private void exigir(char c) {
        if (!tomar(c)) throw sintaxis("Se esperaba «" + c + "» en la posición " + (posicion + 1) + ".");
    }

    private static ErrorComando sintaxis(String mensaje) {
        return new ErrorComando("SINTAXIS", mensaje + " Usá COMANDO[\"parametro\"] o HELP[].");
    }
}
