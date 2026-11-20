package bo.edu.uagrm.talentos;

import bo.edu.uagrm.talentos.comando.Argumentos;
import bo.edu.uagrm.talentos.db.BaseDatos;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record Configuracion(Map<String, String> entorno) {
    public enum Seguridad { TLS, STARTTLS, PLAIN }
    public record Servidor(String host, int puerto, Seguridad seguridad, String usuario, String clave) {}
    public record Correo(String remitente, Servidor pop, Servidor smtp, int intervaloSegundos) {}

    public Instant correoDesde() {
        String valor = entorno.getOrDefault("MAIL_DESDE", "").strip();
        if (valor.isEmpty()) return null;
        try {
            return LocalDate.parse(valor).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("MAIL_DESDE debe tener el formato AAAA-MM-DD.");
        }
    }

    public int tokenMinutos() {
        return entero("TOKEN_MINUTES", 15, 1, 120);
    }

    public BaseDatos baseDatos() {
        return new BaseDatos(requerida("DB_URL"), requerida("DB_USER"), requerida("DB_PASSWORD"));
    }

    public Correo correo() {
        Servidor pop = servidor("POP3", Seguridad.TLS, requerida("POP3_USER"), requerida("POP3_PASSWORD"));
        String usuario = entorno.getOrDefault("SMTP_USER", "");
        String clave = entorno.getOrDefault("SMTP_PASSWORD", "");
        if (usuario.isBlank() != clave.isBlank()) throw new IllegalArgumentException("Definí ambos o ninguno: SMTP_USER y SMTP_PASSWORD.");
        Servidor smtp = servidor("SMTP", Seguridad.STARTTLS, usuario, clave);
        return new Correo(direccion("MAIL_FROM"), pop, smtp, entero("POLL_SECONDS", 15, 1, 3600));
    }

    public String direccion(String variable) {
        String direccion = Argumentos.normalizarCorreo(requerida(variable));
        if (direccion == null) throw new IllegalArgumentException(variable + " debe ser una dirección de correo completa.");
        return direccion;
    }

    public String valor(String variable, String defecto) {
        return entorno.getOrDefault(variable, defecto);
    }

    private Servidor servidor(String tipo, Seguridad defecto, String usuario, String clave) {
        String host = requerida(tipo + "_HOST");
        Seguridad seguridad;
        try {
            seguridad = Seguridad.valueOf(valor(tipo + "_SECURITY", defecto.name()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(tipo + "_SECURITY admite TLS, STARTTLS o PLAIN.");
        }
        if (seguridad == Seguridad.PLAIN && !Set.of("localhost", "127.0.0.1", "::1").contains(host)
                && !valor("ALLOW_PLAINTEXT_MAIL", "NO").equals("SI")) {
            throw new IllegalArgumentException("El correo remoto requiere TLS. Confirmá la configuración del servidor.");
        }
        int puerto = tipo.equals("POP3") ? (seguridad == Seguridad.TLS ? 995 : 110)
                : switch (seguridad) { case TLS -> 465; case STARTTLS -> 587; case PLAIN -> 25; };
        return new Servidor(host, entero(tipo + "_PORT", puerto, 1, 65535), seguridad, usuario, clave);
    }

    private String requerida(String variable) {
        String valor = entorno.get(variable);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Falta definir " + variable + ".");
        return valor;
    }

    private int entero(String variable, int defecto, int minimo, int maximo) {
        try {
            int valor = Integer.parseInt(valor(variable, Integer.toString(defecto)));
            if (valor >= minimo && valor <= maximo) return valor;
        } catch (NumberFormatException ignorado) {
            // El mensaje no incluye el valor leído del entorno.
        }
        throw new IllegalArgumentException(variable + " debe ser un entero entre " + minimo + " y " + maximo + ".");
    }
}
