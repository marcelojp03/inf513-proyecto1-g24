package bo.edu.uagrm.talentos;

import bo.edu.uagrm.talentos.correo.CorreoRed;
import bo.edu.uagrm.talentos.correo.FlujoCorreo;
import bo.edu.uagrm.talentos.db.BaseDatos;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegracionCorreoTest {
    private static final String BUZON = "buzon@test.local";
    private static final Pattern CODIGO = Pattern.compile("TOKEN: ([A-Za-z0-9_-]{32})");
    private static final Path ESQUEMA = Path.of("sql", "01_esquema.sql");

    private GreenMail greenMail;
    private Configuracion configuracion;
    private BaseDatos base;
    private FlujoCorreo flujo;
    private CorreoRed red;

    @BeforeAll
    void preparar() throws Exception {
        String url = System.getenv("TEST_DB_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "Definí TEST_DB_URL para ejecutar la prueba integral.");
        resetearEsquema(url);
        greenMail = new GreenMail(new ServerSetup[]{
                new ServerSetup(0, "127.0.0.1", "smtp"),
                new ServerSetup(0, "127.0.0.1", "pop3")});
        greenMail.start();
        for (String usuario : new String[]{"buzon", "admin", "candidato", "reclutador"}) {
            greenMail.setUser(usuario + "@test.local", usuario + "@test.local", "clave-" + usuario);
        }
        Map<String, String> env = new HashMap<>();
        env.put("DB_URL", url);
        env.put("DB_USER", System.getenv().getOrDefault("TEST_DB_USER", "postgres"));
        env.put("DB_PASSWORD", System.getenv().getOrDefault("TEST_DB_PASSWORD", "x"));
        env.put("POP3_HOST", "127.0.0.1");
        env.put("POP3_PORT", Integer.toString(greenMail.getPop3().getPort()));
        env.put("POP3_SECURITY", "PLAIN");
        env.put("POP3_USER", BUZON);
        env.put("POP3_PASSWORD", "clave-buzon");
        env.put("SMTP_HOST", "127.0.0.1");
        env.put("SMTP_PORT", Integer.toString(greenMail.getSmtp().getPort()));
        env.put("SMTP_SECURITY", "PLAIN");
        env.put("SMTP_USER", BUZON);
        env.put("SMTP_PASSWORD", "clave-buzon");
        env.put("MAIL_FROM", BUZON);
        configuracion = new Configuracion(env);
        base = configuracion.baseDatos();
        base.transaccion(db -> {
            db.ejecutar("INSERT INTO usuario(correo, rol) VALUES ('admin@test.local', 'ADMINISTRADOR'), "
                    + "('reclutador@test.local', 'RECLUTADOR'), ('candidato@test.local', 'CANDIDATO')");
            return null;
        });
        flujo = new FlujoCorreo(base, new Bitacora(Path.of("target", "logs-flujo")), java.time.Instant.parse("2026-01-01T00:00:00Z"), 15);
        red = new CorreoRed(configuracion.correo(), new Bitacora(Path.of("target", "logs-red")));
    }

    @AfterAll
    void cerrar() throws Exception {
        if (greenMail != null) greenMail.stop();
    }

    @Test
    void recorridoCompletoConAccesoVerificable() throws Exception {
        enviar("admin@test.local", "ACCESO[]", "");
        ciclo();
        String codigoAdmin = extraerCodigo("admin@test.local");

        enviar("admin@test.local", "INSEMP[\"Acme SRL\",\"rrhh@acme.test\"]", "TOKEN: " + codigoAdmin);
        ciclo();
        assertTrue(ultimaRespuesta("admin@test.local").contains("Empresa registrada"), "La empresa debe quedar registrada");

        enviar("admin@test.local", "LISEMP[]", "TOKEN: " + codigoAdmin);
        ciclo();
        assertTrue(ultimaRespuesta("admin@test.local").contains("Acme SRL"), "El código sirve para varios comandos dentro de la sesión");

        base.transaccion(db -> db.ejecutar("""
                UPDATE token_acceso SET expira_en = clock_timestamp() - interval '1 minute'
                WHERE usuario_id = (SELECT id FROM usuario WHERE correo = 'admin@test.local')
                """));
        enviar("admin@test.local", "LISEMP[]", "TOKEN: " + codigoAdmin);
        ciclo();
        assertTrue(ultimaRespuesta("admin@test.local").contains("[IDENTIDAD]"), "Un código vencido no autentica");

        enviar("candidato@test.local", "ACCESO[]", "");
        ciclo();
        String codigoCandidato = extraerCodigo("candidato@test.local");

        enviar("candidato@test.local", "INSEXP[\"Ana Demo\",\"Administracion\",\"24\",\"excel;atencion\",\"SI\"]", "TOKEN: " + codigoCandidato);
        ciclo();
        assertTrue(ultimaRespuesta("candidato@test.local").contains("Expediente universitario registrado"));
        assertTrue(ultimaRespuesta("candidato@test.local").contains("No se publica"));

        enviar("candidato@test.local", "ACCESO[]", "");
        ciclo();
        String codigoCandidato2 = extraerCodigo("candidato@test.local");
        enviar("candidato@test.local", "LISEMP[]", "TOKEN: " + codigoCandidato2);
        ciclo();
        assertTrue(ultimaRespuesta("candidato@test.local").contains("[PERMISO]"), "Un candidato no lista empresas");

        long entradas = base.lectura(db -> db.uno("SELECT count(*) FROM correo_entrada", f -> f.getLong(1)).orElse(0L));
        ciclo();
        long despues = base.lectura(db -> db.uno("SELECT count(*) FROM correo_entrada", f -> f.getLong(1)).orElse(0L));
        assertEquals(entradas, despues, "Releer el buzón no debe duplicar el procesamiento");

        long antiguos = contar("IGNORADO_ANTIGUO");
        enviar("viejo@test.local", "HELP[]", "", new java.util.Date(1577836800000L));
        ciclo();
        assertEquals(antiguos + 1, contar("IGNORADO_ANTIGUO"), "Un correo previo al corte se registra sin procesarse");
        assertFalse(hayMensajePara("viejo@test.local"), "Un correo previo al corte no recibe respuesta");
    }

    private long contar(String resultado) throws Exception {
        return base.lectura(db -> db.uno("SELECT count(*) FROM correo_entrada WHERE resultado = ?",
                f -> f.getLong(1), resultado).orElse(0L));
    }

    private boolean hayMensajePara(String destino) throws Exception {
        for (MimeMessage mensaje : greenMail.getReceivedMessages()) {
            if (destino.equals(destinoDe(mensaje))) return true;
        }
        return false;
    }

    private void ciclo() throws Exception {
        red.leer(flujo);
        flujo.despachar(red::enviar);
    }

    private void enviar(String de, String asunto, String cuerpo) throws Exception {
        enviar(de, asunto, cuerpo, null);
    }

    private void enviar(String de, String asunto, String cuerpo, java.util.Date fecha) throws Exception {
        Properties p = new Properties();
        p.setProperty("mail.smtp.host", "127.0.0.1");
        p.setProperty("mail.smtp.port", Integer.toString(greenMail.getSmtp().getPort()));
        MimeMessage mensaje = new MimeMessage(Session.getInstance(p));
        mensaje.setFrom(new InternetAddress(de, true));
        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(BUZON, true));
        mensaje.setSubject(asunto, "UTF-8");
        mensaje.setText(cuerpo, "UTF-8");
        if (fecha != null) mensaje.setSentDate(fecha);
        try (var transporte = Session.getInstance(p).getTransport("smtp")) {
            transporte.connect();
            transporte.sendMessage(mensaje, mensaje.getAllRecipients());
        }
    }

    private String extraerCodigo(String destino) throws Exception {
        for (int intento = 0; intento < 50; intento++) {
            String ultimo = null;
            for (MimeMessage mensaje : greenMail.getReceivedMessages()) {
                if (!destino.equals(destinoDe(mensaje)) || !"Banco de Talentos | Código de acceso".equals(mensaje.getSubject())) continue;
                Matcher m = CODIGO.matcher(texto(mensaje));
                if (m.find()) ultimo = m.group(1);
            }
            if (ultimo != null) return ultimo;
            Thread.sleep(100);
        }
        throw new AssertionError("No se recibió el código para " + destino);
    }

    private String ultimaRespuesta(String destino) throws Exception {
        String ultima = null;
        for (MimeMessage mensaje : greenMail.getReceivedMessages()) {
            if (destino.equals(destinoDe(mensaje))) ultima = texto(mensaje);
        }
        assertFalse(ultima == null, "No hay respuesta para " + destino);
        return ultima;
    }

    private static String texto(MimeMessage mensaje) throws Exception {
        Object contenido = mensaje.getContent();
        return contenido == null ? "" : contenido.toString();
    }

    private static String remitente(MimeMessage mensaje) throws Exception {
        return mensaje.getFrom() == null || mensaje.getFrom().length == 0 ? null : mensaje.getFrom()[0].toString();
    }

    private static String destinoDe(MimeMessage mensaje) throws Exception {
        var destinatarios = mensaje.getRecipients(Message.RecipientType.TO);
        return destinatarios == null || destinatarios.length == 0 ? null : destinatarios[0].toString();
    }

    private static void resetearEsquema(String url) throws Exception {
        try (Connection conexion = DriverManager.getConnection(url,
                System.getenv().getOrDefault("TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("TEST_DB_PASSWORD", "x"))) {
            conexion.createStatement().execute("DROP SCHEMA IF EXISTS banco_talentos_mail CASCADE");
            conexion.createStatement().execute(Files.readString(ESQUEMA, StandardCharsets.UTF_8));
        }
    }
}
