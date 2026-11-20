package bo.edu.uagrm.talentos.comando;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record Argumentos(List<String> valores) {
    public String texto(int i, int maximo) {
        String valor = opcional(i, maximo);
        if (valor.isEmpty()) throw tipo(i, "no puede quedar vacío");
        return valor;
    }

    public String opcional(int i, int maximo) {
        String valor = valores.get(i).strip();
        if (valor.length() > maximo) throw tipo(i, "supera " + maximo + " caracteres");
        return valor;
    }

    public long id(int i) {
        return id(valores.get(i).strip(), i);
    }

    private long id(String valor, int i) {
        try {
            if (!valor.matches("[0-9]{1,19}")) throw new NumberFormatException();
            long numero = Long.parseLong(valor);
            if (numero > 0) return numero;
        } catch (NumberFormatException ignorado) {
            // Error uniforme para formato y desborde.
        }
        throw tipo(i, "debe contener un ID entero positivo");
    }

    public int entero(int i, int min, int max) {
        try {
            String valor = valores.get(i).strip();
            if (!valor.matches("[0-9]{1,10}")) throw new NumberFormatException();
            int numero = Integer.parseInt(valor);
            if (numero >= min && numero <= max) return numero;
        } catch (NumberFormatException ignorado) {
        }
        throw tipo(i, "debe ser un entero entre " + min + " y " + max);
    }

    public BigDecimal puntaje(int i) {
        String valor = valores.get(i).strip();
        if (!valor.matches("[0-9]{1,3}(\\.[0-9]{1,2})?")) throw tipo(i, "debe ser un número de 0 a 100 (hasta dos decimales con punto)");
        BigDecimal numero = new BigDecimal(valor);
        if (numero.compareTo(BigDecimal.valueOf(100)) > 0) throw tipo(i, "no puede superar 100");
        return numero;
    }

    public String opcion(int i, String... opciones) {
        String valor = valores.get(i).strip().toUpperCase(Locale.ROOT);
        if (!List.of(opciones).contains(valor)) throw tipo(i, "admite: " + String.join(", ", opciones));
        return valor;
    }

    public boolean siNo(int i) {
        return opcion(i, "SI", "NO").equals("SI");
    }

    public List<Long> ids(int i) {
        String[] partes = valores.get(i).split(",", -1);
        if (partes.length > 10) throw tipo(i, "admite hasta 10 IDs separados por coma");
        Set<Long> ids = new LinkedHashSet<>();
        for (String parte : partes) {
            if (!ids.add(id(parte.strip(), i))) throw tipo(i, "contiene IDs repetidos");
        }
        return List.copyOf(ids);
    }

    public String competencias(int i) {
        String valor = opcional(i, 350);
        if (valor.isEmpty()) return "";
        Set<String> lista = new LinkedHashSet<>();
        String[] partes = valor.split(";", -1);
        if (partes.length > 15) throw tipo(i, "admite hasta 15 competencias separadas por punto y coma");
        for (String parte : partes) {
            String limpia = Normalizer.normalize(parte.strip(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
            if (limpia.isEmpty() || limpia.length() > 60) throw tipo(i, "contiene una competencia vacía o demasiado larga");
            lista.add(limpia);
        }
        return String.join(";", lista);
    }

    public String correo(int i) {
        String correo = normalizarCorreo(valores.get(i));
        if (correo == null) throw tipo(i, "debe ser una dirección de correo completa");
        return correo;
    }

    public static String normalizarCorreo(String fuente) {
        if (fuente == null) return null;
        String correo = fuente.strip().toLowerCase(Locale.ROOT);
        if (correo.length() > 254 || !correo.matches("[a-z0-9!#$%&'*+/=?^_`{|}~.\\-]+@[a-z0-9.\\-]+")) return null;
        try {
            new InternetAddress(correo, true).validate();
            return correo;
        } catch (AddressException e) {
            return null;
        }
    }

    private static ErrorComando tipo(int i, String detalle) {
        return new ErrorComando("PARAMETRO", "El parámetro " + (i + 1) + " " + detalle + ".");
    }
}
