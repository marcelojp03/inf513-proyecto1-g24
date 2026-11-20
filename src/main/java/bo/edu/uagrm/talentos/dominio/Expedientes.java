package bo.edu.uagrm.talentos.dominio;

import bo.edu.uagrm.talentos.comando.Argumentos;
import bo.edu.uagrm.talentos.comando.ErrorComando;
import bo.edu.uagrm.talentos.db.Sql;
import bo.edu.uagrm.talentos.dominio.Administracion.Actor;
import bo.edu.uagrm.talentos.dominio.Administracion.Rol;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import static bo.edu.uagrm.talentos.dominio.Administracion.noEncontrado;

public final class Expedientes {
    record Ficha(long id, long usuario, String tipo, String nombre, String estado) {}

    public String inscribir(Sql db, Actor actor, Argumentos a) throws SQLException {
        long id = db.id("""
                INSERT INTO expediente(usuario_id, tipo, nombre, carrera, experiencia_meses, competencias, disponible)
                VALUES (?, 'A', ?, ?, ?, string_to_array(?, ';'), ?) RETURNING id
                """, actor.id(), a.texto(0, 120), a.texto(1, 120), a.entero(2, 0, 600), a.competencias(3), a.siNo(4));
        evento(db, id, actor, "INSEXP", "Inscripción universitaria interna");
        return "Expediente universitario registrado. ID: " + id + ". No se publica. Podés otorgar el consentimiento de evaluación con CONSEN.";
    }

    public String vincular(Sql db, Actor actor, long usuario, long cargo) throws SQLException {
        if (!db.existe("SELECT 1 FROM usuario WHERE id = ? AND rol = 'CANDIDATO' AND activo", usuario)) {
            throw noEncontrado("Candidato activo inexistente.");
        }
        if (!db.existe("SELECT 1 FROM cargo WHERE id = ?", cargo)) throw noEncontrado("Cargo inexistente.");
        long id = db.id("INSERT INTO expediente(usuario_id, tipo, cargo_id) VALUES (?, 'B', ?) RETURNING id", usuario, cargo);
        evento(db, id, actor, "VINCEXP", "Participación empresarial creada");
        return "Expediente empresarial creado. ID: " + id + ". El candidato puede encontrarlo con MISEXP[] y completarlo con ACTEXP.";
    }

    public String propios(Sql db, Actor actor) throws SQLException {
        List<String> filas = db.lista("""
                SELECT x.id, x.tipo, COALESCE(c.nombre, 'Talento universitario'), x.estado
                FROM expediente x LEFT JOIN cargo c ON c.id = x.cargo_id
                WHERE x.usuario_id = ? ORDER BY x.id DESC LIMIT 100
                """, f -> "#%d | %s | %s | %s".formatted(f.getLong(1), f.getString(2), f.getString(3), f.getString(4)), actor.id());
        return filas.isEmpty() ? "No tenés expedientes." : "Tus expedientes (hasta 100):\n" + String.join("\n", filas);
    }

    public String actualizar(Sql db, Actor actor, Argumentos a) throws SQLException {
        Ficha ficha = propio(db, actor, a.id(0));
        if (!List.of("PENDIENTE", "OBSERVADO").contains(ficha.estado())) {
            throw new ErrorComando("ESTADO", "Solo se pueden editar expedientes pendientes u observados.");
        }
        db.ejecutar("""
                UPDATE expediente SET nombre = ?, carrera = ?, experiencia_meses = ?, competencias = string_to_array(?, ';'),
                                      disponible = ?, estado = 'PENDIENTE' WHERE id = ?
                """, a.texto(1, 120), a.texto(2, 120), a.entero(3, 0, 600), a.competencias(4), a.siNo(5), ficha.id());
        evento(db, ficha.id(), actor, "ACTEXP", "Antecedentes actualizados por el titular; pendiente de revisión");
        return "Expediente actualizado. ID: " + ficha.id() + ".";
    }

    public String consultar(Sql db, Actor actor, long id) throws SQLException {
        Ficha ficha = lectura(db, actor, id);
        String info = db.uno("""
                SELECT x.nombre, x.carrera, x.experiencia_meses, array_to_string(x.competencias, '; '), x.disponible,
                       c.nombre, m.nombre, m.correo_informes, p.id, k.puntaje
                FROM expediente x LEFT JOIN cargo c ON c.id = x.cargo_id LEFT JOIN empresa m ON m.id = c.empresa_id
                LEFT JOIN plan p ON p.expediente_id = x.id LEFT JOIN consolidado k ON k.expediente_id = x.id
                WHERE x.id = ?
                """, f -> {
            String perfil = f.getString(1) == null ? "Perfil pendiente de completar."
                    : "Nombre: %s\nCarrera: %s\nExperiencia: %d meses\nCompetencias: %s\nDisponible: %s".formatted(
                    f.getString(1), f.getString(2), f.getInt(3), f.getString(4), f.getBoolean(5) ? "Sí" : "No");
            if (f.getString(6) != null) perfil += "\nCargo: %s (%s)\nDestinatario empresarial actual: %s".formatted(
                    f.getString(6), f.getString(7), f.getString(8));
            return perfil + "\nPlan: " + (f.getString(9) == null ? "sin asignar" : f.getString(9))
                    + "\nPuntaje demo: " + (f.getString(10) == null ? "no consolidado" : f.getString(10));
        }, id).orElseThrow();
        String tratamiento = consentimientoVigente(db, id) ? "otorgado" : "no vigente";
        String informe = ficha.tipo().equals("A") ? "no aplica" :
                db.existe("""
                        SELECT 1 FROM consentimiento t JOIN expediente x ON x.id = t.expediente_id
                        JOIN cargo c ON c.id = x.cargo_id JOIN empresa m ON m.id = c.empresa_id
                        WHERE t.expediente_id = ? AND t.finalidad = 'INFORME_EMPRESA' AND t.otorgado AND t.destino = m.correo_informes
                        """, id) ? "otorgado para el contacto actual" : "no vigente para el contacto actual";
        return "Expediente #" + id + " | Proceso " + ficha.tipo() + " | Clasificación: " + ficha.estado() + "\n" + info
                + "\nPrivacidad: interno\nConsentimiento EVALUACION: " + tratamiento + "\nConsentimiento INFORME_EMPRESA: " + informe;
    }

    public String consentir(Sql db, Actor actor, Argumentos a) throws SQLException {
        Ficha ficha = propio(db, actor, a.id(0));
        String finalidad = a.opcion(1, "EVALUACION", "INFORME_EMPRESA");
        boolean otorgado = a.siNo(2);
        String destino = null;
        if (finalidad.equals("INFORME_EMPRESA") && !ficha.tipo().equals("B")) {
            throw new ErrorComando("ESTADO", "El despacho empresarial se aplica solo a expedientes B.");
        }
        if (finalidad.equals("INFORME_EMPRESA") && otorgado) {
            destino = a.correo(3);
            if (!destino.equals(contactoEmpresa(db, ficha.id()))) {
                throw new ErrorComando("PARAMETRO", "El destinatario debe coincidir con el contacto que muestra CONSEXP.");
            }
        } else if (!a.opcional(3, 254).isEmpty()) {
            throw new ErrorComando("PARAMETRO", "Usá un cuarto parámetro vacío si no estás autorizando un destinatario empresarial.");
        }
        db.ejecutar("""
                INSERT INTO consentimiento(expediente_id, finalidad, otorgado, destino) VALUES (?, ?, ?, ?)
                ON CONFLICT (expediente_id, finalidad) DO UPDATE
                SET otorgado = excluded.otorgado, destino = excluded.destino, actualizado_en = now()
                """, ficha.id(), finalidad, otorgado, destino);
        evento(db, ficha.id(), actor, "CONSEN", finalidad + (otorgado ? ": otorgado" : ": revocado/no otorgado"));
        return finalidad + (otorgado ? ": consentimiento otorgado." : ": consentimiento revocado/no otorgado.");
    }

    public String clasificar(Sql db, Actor actor, Argumentos a) throws SQLException {
        Ficha ficha = lectura(db, actor, a.id(0));
        String estado = a.opcion(1, "APTO", "OBSERVADO", "NO_APTO");
        String motivo = a.texto(2, 400);
        if (ficha.nombre() == null) throw new ErrorComando("ESTADO", "El candidato debe completar primero su perfil.");
        if (db.existe("SELECT 1 FROM plan WHERE expediente_id = ?", ficha.id())) {
            throw new ErrorComando("ESTADO", "El expediente ya tiene plan de evaluación.");
        }
        db.ejecutar("UPDATE expediente SET estado = ? WHERE id = ?", estado, ficha.id());
        evento(db, ficha.id(), actor, "CLASEXP", estado + " | " + motivo);
        return "Clasificación registrada por revisión humana: " + estado + ".";
    }

    public String historial(Sql db, Actor actor, long id) throws SQLException {
        lectura(db, actor, id);
        List<String> eventos = db.lista("""
                SELECT creado_en, actor_id, operacion, detalle FROM evento_expediente
                WHERE expediente_id = ? ORDER BY id DESC LIMIT 100
                """, f -> "%s | usuario #%d | %s | %s".formatted(f.getObject(1, OffsetDateTime.class), f.getLong(2),
                f.getString(3), f.getString(4)), id);
        return "Últimos 100 eventos del expediente #" + id + ":\n" + String.join("\n", eventos);
    }

    static Ficha lectura(Sql db, Actor actor, long id) throws SQLException {
        if (actor.rol() == Rol.CANDIDATO) return propio(db, actor, id);
        Ficha ficha = buscar(db, id, null);
        exigirEvaluacion(db, id);
        return ficha;
    }

    private static Ficha propio(Sql db, Actor actor, long id) throws SQLException {
        return buscar(db, id, actor.id());
    }

    private static Ficha buscar(Sql db, long id, Long titular) throws SQLException {
        String base = "SELECT id, usuario_id, tipo, nombre, estado FROM expediente WHERE id = ?";
        Sql.Fila<Ficha> fila = f -> new Ficha(f.getLong(1), f.getLong(2), f.getString(3), f.getString(4), f.getString(5));
        return (titular == null ? db.uno(base, fila, id) : db.uno(base + " AND usuario_id = ?", fila, id, titular))
                .orElseThrow(() -> noEncontrado("Expediente inexistente o no autorizado."));
    }

    static void exigirEvaluacion(Sql db, long id) throws SQLException {
        if (!consentimientoVigente(db, id)) throw new ErrorComando("CONSENTIMIENTO", "El expediente no tiene un consentimiento de evaluación vigente.");
    }

    public static boolean consentimientoVigente(Sql db, long id) throws SQLException {
        return db.existe("""
                SELECT 1 FROM consentimiento t JOIN expediente x ON x.id = t.expediente_id JOIN usuario u ON u.id = x.usuario_id
                WHERE t.expediente_id = ? AND t.finalidad = 'EVALUACION' AND t.otorgado AND u.activo
                """, id);
    }

    static String contactoEmpresa(Sql db, long id) throws SQLException {
        return db.uno("""
                SELECT m.correo_informes FROM expediente x JOIN cargo c ON c.id = x.cargo_id JOIN empresa m ON m.id = c.empresa_id
                WHERE x.id = ?
                """, f -> f.getString(1), id).orElseThrow(() -> new ErrorComando("ESTADO", "No hay empresa asociada al expediente."));
    }

    static void evento(Sql db, long id, Actor actor, String operacion, String detalle) throws SQLException {
        db.ejecutar("INSERT INTO evento_expediente(expediente_id, actor_id, operacion, detalle) VALUES (?, ?, ?, ?)",
                id, actor.id(), operacion, detalle);
    }
}
