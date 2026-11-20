package bo.edu.uagrm.talentos.correo;

import bo.edu.uagrm.talentos.Bitacora;
import bo.edu.uagrm.talentos.comando.Analizador;
import bo.edu.uagrm.talentos.comando.Catalogo;
import bo.edu.uagrm.talentos.comando.ErrorComando;
import bo.edu.uagrm.talentos.db.BaseDatos;
import bo.edu.uagrm.talentos.db.Sql;
import bo.edu.uagrm.talentos.dominio.Administracion.Actor;
import bo.edu.uagrm.talentos.dominio.Administracion.Rol;
import bo.edu.uagrm.talentos.dominio.Expedientes;
import bo.edu.uagrm.talentos.dominio.Informes;
import jakarta.mail.MessagingException;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class FlujoCorreo {
    private static final SecureRandom ALEATORIO = new SecureRandom();

    record Recibido(String huella, String remitente, String asunto, String token, boolean demasiadoGrande, Instant fecha) {}

    private record Resultado(long id, String comando, String codigo) {}

    private record Salida(long id, String tipo, String destino, String asunto, String cuerpo, Long actor, String rol,
                          Long informe, Long expediente, int intentos) {}

    private record Reclamo(long id, String tipo, String destino, String asunto, String cuerpo, Long actor, String rol,
                           int intento, boolean cancelado) {}

    private record Envio(long id, String estado) {}

    @FunctionalInterface
    public interface Entrega {
        void enviar(String destino, String asunto, String cuerpo) throws MessagingException;
    }

    private final BaseDatos base;
    private final Catalogo catalogo = new Catalogo();
    private final Bitacora log;
    private final Instant desde;

    public FlujoCorreo(BaseDatos base, Bitacora log, Instant desde) {
        this.base = base;
        this.log = log;
        this.desde = desde;
    }

    Set<String> registrados(List<String> huellas) throws SQLException {
        return base.lectura(db -> Set.copyOf(db.lista("""
                SELECT huella FROM correo_entrada WHERE huella = ANY(string_to_array(?, ',')::char(64)[])
                """, f -> f.getString(1), String.join(",", huellas))));
    }

    public int reconciliar() throws SQLException {
        int inciertos = base.transaccion(db -> db.ejecutar("""
                UPDATE correo_salida SET estado = 'INDETERMINADO', cuerpo = NULL, resuelto_en = clock_timestamp()
                WHERE estado = 'ENVIANDO'
                """));
        if (inciertos > 0) log.error("canal=SMTP resultado=ENVIO_INDETERMINADO cantidad=" + inciertos);
        return inciertos;
    }

    boolean procesar(Recibido mensaje) throws SQLException {
        Resultado resultado = base.transaccion(db -> ejecutar(db, mensaje));
        if (resultado == null) return false;
        String evento = "entrada=" + resultado.id() + " comando=" + resultado.comando() + " resultado=" + resultado.codigo();
        log.acceso(evento);
        if (!Set.of("OK", "IGNORADO", "IGNORADO_ANTIGUO").contains(resultado.codigo())) log.error(evento);
        return true;
    }

    private Resultado ejecutar(Sql db, Recibido mensaje) throws SQLException {
        Optional<Long> id = db.uno("INSERT INTO correo_entrada(huella) VALUES (?) ON CONFLICT DO NOTHING RETURNING id",
                f -> f.getLong(1), mensaje.huella());
        if (id.isEmpty()) return null;
        long entrada = id.get();
        if (desde != null && mensaje.fecha() != null && mensaje.fecha().isBefore(desde)) {
            db.ejecutar("UPDATE correo_entrada SET resultado = 'IGNORADO_ANTIGUO' WHERE id = ?", entrada);
            return new Resultado(entrada, "-", "IGNORADO_ANTIGUO");
        }
        String comando = null;
        String codigo = "OK";
        Actor actor = null;
        if (mensaje.remitente() == null) {
            codigo = "IGNORADO";
        } else {
            String respuesta = null;
            Long expediente = null;
            try {
                if (mensaje.demasiadoGrande()) throw new ErrorComando("TAMANO", "El correo supera 64 KiB. Enviá un correo de texto sin adjuntos.");
                var analizado = Analizador.analizar(mensaje.asunto());
                if (!catalogo.conoce(analizado.nombre())) throw new ErrorComando("COMANDO", "Comando desconocido. Consultá HELP[].");
                comando = analizado.nombre();
                switch (comando) {
                    case "HELP" -> {
                        Catalogo.exigirCantidad(analizado, 0);
                        respuesta = catalogo.ayuda();
                    }
                    case "ACCESO" -> {
                        Catalogo.exigirCantidad(analizado, 0);
                        if (!encolarAcceso(db, entrada, mensaje.remitente())) {
                            respuesta = "Si la cuenta está habilitada, revisá si ya tenés un código vigente. "
                                    + "Si no podés acceder, consultá al administrador.";
                        }
                    }
                    default -> {
                        actor = autenticar(db, mensaje);
                        if (actor == null) throw new ErrorComando("IDENTIDAD", "Solicitá un código con ACCESO[] y usalo una vez en el cuerpo: TOKEN: <código>.");
                        Savepoint operacion = db.conexion().setSavepoint();
                        try {
                            var resultado = catalogo.ejecutar(db, actor, analizado, entrada);
                            respuesta = resultado.texto();
                            expediente = resultado.expediente();
                        } catch (ErrorComando e) {
                            db.conexion().rollback(operacion);
                            throw e;
                        } catch (SQLException e) {
                            if (!"23505".equals(e.getSQLState())) throw e;
                            db.conexion().rollback(operacion);
                            throw new ErrorComando("DUPLICADO", "Ya existe un registro con esos datos o ese vínculo.");
                        }
                    }
                }
            } catch (ErrorComando e) {
                codigo = e.codigo();
                respuesta = "Error [" + codigo + "]: " + e.getMessage();
            }
            if (respuesta != null) {
                encolar(db, entrada, "RESPUESTA", mensaje.remitente(), "Banco de Talentos | Operación #" + entrada,
                        respuesta, actor, null, expediente);
            }
        }
        db.ejecutar("UPDATE correo_entrada SET comando = ?, resultado = ? WHERE id = ?", comando, codigo, entrada);
        return new Resultado(entrada, comando == null ? "-" : comando, codigo);
    }

    private boolean encolarAcceso(Sql db, long entrada, String correo) throws SQLException {
        Actor actor = cuenta(db, correo).orElse(null);
        if (actor == null) return false;
        if (db.existe("SELECT 1 FROM correo_salida WHERE actor_id = ? AND tipo = 'ACCESO' AND estado IN ('PENDIENTE', 'ENVIANDO')", actor.id())) {
            return false;
        }
        // El código se genera al despachar y vale 15 minutos como sesión: nunca queda en claro en la cola ni en la base.
        encolar(db, entrada, "ACCESO", correo, "Banco de Talentos | Código de acceso", null, actor, null, null);
        return true;
    }

    private static Actor autenticar(Sql db, Recibido mensaje) throws SQLException {
        if (mensaje.token() == null) return null;
        Actor actor = cuenta(db, mensaje.remitente()).orElse(null);
        if (actor == null) return null;
        boolean vigente = db.existe("""
                SELECT 1 FROM token_acceso WHERE usuario_id = ? AND huella = ? AND expira_en > clock_timestamp()
                """, actor.id(), Huellas.sha256(mensaje.token()));
        return vigente ? actor : null;
    }

    private static Optional<Actor> cuenta(Sql db, String correo) throws SQLException {
        return db.uno("SELECT id, rol FROM usuario WHERE correo = ? AND activo", f -> new Actor(f.getLong(1), Rol.valueOf(f.getString(2))), correo);
    }

    private static void encolar(Sql db, long entrada, String tipo, String destino, String asunto, String cuerpo,
                                Actor actor, Long informe, Long expediente) throws SQLException {
        db.ejecutar("""
                INSERT INTO correo_salida(entrada_id, tipo, destino, asunto, cuerpo, actor_id, rol_actor, informe_id, expediente_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, entrada, tipo, destino, asunto, cuerpo, actor == null ? null : actor.id(),
                actor == null ? null : actor.rol().name(), informe, expediente);
    }

    public int despachar(Entrega entrega) throws SQLException {
        int aceptados = 0;
        for (int i = 0; i < 50; i++) {
            Reclamo reclamo = base.transaccion(this::reclamar);
            if (reclamo == null) break;
            if (reclamo.cancelado()) {
                log.acceso("salida=" + reclamo.id() + " estado=CANCELADO");
                continue;
            }
            try {
                entrega.enviar(reclamo.destino(), reclamo.asunto(), reclamo.cuerpo());
            } catch (MessagingException e) {
                Envio envio = base.transaccion(db -> fallar(db, reclamo));
                log.error("salida=" + envio.id() + " estado=" + envio.estado() + " motivo=" + diagnostico(e));
                continue;
            }
            Envio envio = base.transaccion(db -> resolver(db, reclamo.id(), "ACEPTADO_SMTP"));
            log.acceso("salida=" + envio.id() + " estado=" + envio.estado());
            aceptados++;
        }
        return aceptados;
    }

    private Reclamo reclamar(Sql db) throws SQLException {
        Salida s = db.uno("""
                SELECT id, tipo, destino, asunto, cuerpo, actor_id, rol_actor, informe_id, expediente_id, intentos
                FROM correo_salida WHERE estado = 'PENDIENTE' AND proximo_intento <= clock_timestamp()
                ORDER BY id LIMIT 1
                """, f -> new Salida(f.getLong(1), f.getString(2), f.getString(3), f.getString(4), f.getString(5),
                f.getObject(6, Long.class), f.getString(7), f.getObject(8, Long.class), f.getObject(9, Long.class), f.getInt(10)))
                .orElse(null);
        if (s == null) return null;
        if (!vigente(db, s)) {
            resolver(db, s.id(), "CANCELADO");
            return new Reclamo(s.id(), s.tipo(), s.destino(), s.asunto(), null, s.actor(), s.rol(), 0, true);
        }
        int intento = s.intentos() + 1;
        String cuerpo = s.tipo().equals("ACCESO") ? tokenNuevo(db, s.actor()) : s.cuerpo();
        db.ejecutar("UPDATE correo_salida SET estado = 'ENVIANDO', intentos = ? WHERE id = ?", intento, s.id());
        return new Reclamo(s.id(), s.tipo(), s.destino(), s.asunto(), cuerpo, s.actor(), s.rol(), intento, false);
    }

    private static String tokenNuevo(Sql db, long usuario) throws SQLException {
        byte[] bytes = new byte[24];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        db.ejecutar("""
                INSERT INTO token_acceso(usuario_id, huella, expira_en)
                VALUES (?, ?, clock_timestamp() + interval '15 minutes')
                ON CONFLICT (usuario_id) DO UPDATE SET huella = excluded.huella, expira_en = excluded.expira_en
                """, usuario, Huellas.sha256(token));
        return "Enviá un correo nuevo (no una respuesta) con el comando en el asunto y esta línea en el cuerpo de texto plano:\n\n"
                + "TOKEN: " + token + "\n\nEl código vale 15 minutos y sirve para varios comandos. No lo compartas.";
    }

    private static boolean vigente(Sql db, Salida s) throws SQLException {
        if (s.actor() != null && !db.existe("SELECT 1 FROM usuario WHERE id = ? AND activo AND rol = ?", s.actor(), s.rol())) return false;
        if (s.tipo().equals("INFORME")) {
            return Informes.destinoAutorizado(db, s.informe()).filter(s.destino()::equals).isPresent();
        }
        return s.expediente() == null || Expedientes.consentimientoVigente(db, s.expediente());
    }

    private static Envio fallar(Sql db, Reclamo r) throws SQLException {
        if (r.tipo().equals("ACCESO")) db.ejecutar("DELETE FROM token_acceso WHERE usuario_id = ?", r.actor());
        if (r.intento() >= 3) return resolver(db, r.id(), "FALLIDO");
        db.ejecutar("""
                UPDATE correo_salida SET estado = 'PENDIENTE', proximo_intento = clock_timestamp() + ? * interval '30 seconds'
                WHERE id = ?
                """, r.intento(), r.id());
        return new Envio(r.id(), "REINTENTO_SMTP");
    }

    private static Envio resolver(Sql db, long id, String estado) throws SQLException {
        db.ejecutar("""
                UPDATE correo_salida SET estado = ?, cuerpo = NULL, resuelto_en = clock_timestamp() WHERE id = ?
                """, estado, id);
        return new Envio(id, estado);
    }

    public static String diagnostico(MessagingException e) {
        StringBuilder texto = new StringBuilder();
        Throwable actual = e;
        for (int i = 0; actual != null && i < 4; i++) {
            if (actual.getMessage() != null) texto.append(" | ").append(actual.getMessage());
            actual = actual instanceof MessagingException mensaje && mensaje.getNextException() != null
                    ? mensaje.getNextException() : actual.getCause();
        }
        String limpio = texto.toString().replaceAll("[\\w.+-]+@[\\w.-]+", "<correo>").replaceAll("[\\r\\n]+", " ").strip();
        return limpio.length() > 300 ? limpio.substring(0, 300) : limpio;
    }
}
