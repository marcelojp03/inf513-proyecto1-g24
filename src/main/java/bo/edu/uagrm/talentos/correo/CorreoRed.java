package bo.edu.uagrm.talentos.correo;

import bo.edu.uagrm.talentos.Bitacora;
import bo.edu.uagrm.talentos.Configuracion.Correo;
import bo.edu.uagrm.talentos.Configuracion.Seguridad;
import bo.edu.uagrm.talentos.Configuracion.Servidor;
import bo.edu.uagrm.talentos.comando.Argumentos;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.pop3.POP3Folder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

public final class CorreoRed {
    private static final int MAX_CORREO = 64 * 1024;
    private static final Pattern TOKEN = Pattern.compile("TOKEN:[ \\t]*([A-Za-z0-9_-]{32})");
    private final Correo config;
    private final Bitacora log;
    private final String buzon;

    public CorreoRed(Correo config, Bitacora log) {
        this.config = config;
        this.log = log;
        buzon = config.pop().host().toLowerCase(Locale.ROOT) + "\n" + config.pop().usuario();
    }

    public int contarMensajes() throws MessagingException {
        Servidor pop = config.pop();
        Session sesion = sesion("pop3", pop);
        try (Store store = sesion.getStore("pop3")) {
            store.connect(pop.host(), pop.puerto(), pop.usuario(), pop.clave());
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            try {
                return inbox.getMessageCount();
            } finally {
                inbox.close(false);
            }
        }
    }

    public int leer(FlujoCorreo flujo) throws MessagingException, SQLException {
        Servidor pop = config.pop();
        Session sesion = sesion("pop3", pop);
        int procesados = 0;
        try (Store store = sesion.getStore("pop3")) {
            store.connect(pop.host(), pop.puerto(), pop.usuario(), pop.clave());
            POP3Folder inbox = (POP3Folder) store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            try {
                Message[] mensajes = inbox.getMessages();
                if (mensajes.length == 0) return 0;
                var perfil = new FetchProfile();
                perfil.add(UIDFolder.FetchProfileItem.UID);
                inbox.fetch(mensajes, perfil);
                var huellas = new ArrayList<String>(mensajes.length);
                for (Message mensaje : mensajes) {
                    String uid = inbox.getUID(mensaje);
                    if (uid == null || uid.isBlank()) throw new MessagingException("El servidor POP3 no devolvió UIDL.");
                    huellas.add(Huellas.sha256(buzon + "\n" + uid));
                }
                var registrados = flujo.registrados(huellas);
                int[] tamanos = inbox.getSizes();
                for (int i = 0; i < mensajes.length; i++) {
                    String huella = huellas.get(i);
                    if (registrados.contains(huella)) continue;
                    Message mensaje = mensajes[i];
                    try {
                        // Decidir por tamaño antes de pedir cabeceras: con POP3 una cabecera puede arrastrar el mensaje completo.
                        long tamano = i < tamanos.length && tamanos[i] > 0 ? tamanos[i] : mensaje.getSize();
                        boolean grande = tamano > MAX_CORREO;
                        String remitente = grande ? null : remitente(mensaje);
                        boolean leer = remitente != null;
                        String asunto = leer ? mensaje.getSubject() : null;
                        String token = leer ? leerToken(mensaje) : null;
                        java.util.Date enviado = leer ? mensaje.getSentDate() : null;
                        var fecha = enviado == null ? null : enviado.toInstant();
                        if (flujo.procesar(new FlujoCorreo.Recibido(huella, remitente, asunto, token, grande, fecha))) procesados++;
                    } catch (IOException | MessagingException e) {
                        log.error("canal=POP3 resultado=MENSAJE_ILEGIBLE");
                    }
                }
            } finally {
                inbox.close(false);
            }
        }
        return procesados;
    }

    private String remitente(Message mensaje) throws MessagingException {
        String[] cabecera = mensaje.getHeader("From");
        if (cabecera == null || cabecera.length != 1) return null;
        var origen = mensaje.getFrom();
        if (origen == null || origen.length != 1 || !(origen[0] instanceof InternetAddress direccion) || direccion.isGroup()) return null;
        String correo = Argumentos.normalizarCorreo(direccion.getAddress());
        return automatico(mensaje) ? null : correo;
    }

    private static boolean automatico(Message mensaje) throws MessagingException {
        String[] auto = mensaje.getHeader("Auto-Submitted");
        String[] camino = mensaje.getHeader("Return-Path");
        return (auto != null && (auto.length != 1 || !auto[0].strip().equalsIgnoreCase("no")))
                || (camino != null && camino.length == 1 && camino[0].strip().equals("<>"))
                || mensaje.isMimeType("multipart/report");
    }

    private static String leerToken(Part parte) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition())) return null;
        if (parte.isMimeType("text/plain")) {
            try (var entrada = parte.getInputStream()) {
                String linea = new String(entrada.readNBytes(4097), StandardCharsets.UTF_8).strip();
                var coincidencia = TOKEN.matcher(linea);
                return coincidencia.matches() ? coincidencia.group(1) : null;
            }
        }
        if ((parte.isMimeType("multipart/alternative") || parte.isMimeType("multipart/mixed")) && parte.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                Part parteHija = multipart.getBodyPart(i);
                if (!parteHija.isMimeType("text/plain")) continue;
                String token = leerToken(parteHija);
                if (token != null) return token;
            }
        }
        return null;
    }

    public void enviar(String destino, String asunto, String cuerpo) throws MessagingException {
        Servidor smtp = config.smtp();
        Session sesion = sesion("smtp", smtp);
        MimeMessage respuesta = new MimeMessage(sesion);
        respuesta.setFrom(new InternetAddress(config.remitente(), true));
        respuesta.setRecipient(Message.RecipientType.TO, new InternetAddress(destino, true));
        respuesta.setHeader("Auto-Submitted", "auto-replied");
        respuesta.setSubject(asunto, "UTF-8");
        respuesta.setText(cuerpo, "UTF-8");
        respuesta.saveChanges();
        Transport transporte = sesion.getTransport("smtp");
        try {
            transporte.connect(smtp.host(), smtp.puerto(), smtp.usuario().isBlank() ? null : smtp.usuario(),
                    smtp.clave().isBlank() ? null : smtp.clave());
            transporte.sendMessage(respuesta, respuesta.getAllRecipients());
        } finally {
            try {
                transporte.close();
            } catch (MessagingException ignorado) {
                // Un fallo en QUIT no invalida la aceptación de DATA.
            }
        }
    }

    private static Session sesion(String protocolo, Servidor servidor) {
        String prefijo = "mail." + protocolo + ".";
        var p = new Properties();
        p.setProperty(prefijo + "connectiontimeout", "8000");
        p.setProperty(prefijo + "timeout", "8000");
        p.setProperty(prefijo + "writetimeout", "8000");
        p.setProperty(prefijo + "ssl.checkserveridentity", "true");
        if (servidor.seguridad() == Seguridad.TLS) p.setProperty(prefijo + "ssl.enable", "true");
        if (servidor.seguridad() == Seguridad.STARTTLS) {
            p.setProperty(prefijo + "starttls.enable", "true");
            p.setProperty(prefijo + "starttls.required", "true");
        }
        if (protocolo.equals("smtp")) {
            p.setProperty("mail.smtp.auth", Boolean.toString(!servidor.usuario().isBlank()));
            p.setProperty("mail.smtp.quitwait", "false");
        }
        return Session.getInstance(p);
    }
}
