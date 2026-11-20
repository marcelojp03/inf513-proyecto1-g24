package bo.edu.uagrm.talentos.dominio;

import bo.edu.uagrm.talentos.comando.Argumentos;
import bo.edu.uagrm.talentos.comando.ErrorComando;
import bo.edu.uagrm.talentos.db.Sql;
import bo.edu.uagrm.talentos.dominio.Administracion.Actor;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static bo.edu.uagrm.talentos.dominio.Expedientes.evento;

public final class Evaluaciones {
    public static final String REGLA = "DEMO_V1";
    private record Prueba(long expediente, String instrumento, String consigna, String respuesta, BigDecimal puntaje) {}

    public String asignar(Sql db, Actor actor, long id, List<Long> instrumentos) throws SQLException {
        Expedientes.Ficha ficha = Expedientes.lectura(db, actor, id);
        if (!ficha.estado().equals("APTO")) throw estado("El expediente debe estar clasificado como APTO.");
        if (db.existe("SELECT 1 FROM plan WHERE expediente_id = ?", id)) throw estado("El expediente ya tiene un plan.");
        Set<String> codigos = new HashSet<>();
        for (long instrumento : instrumentos) {
            String codigo = db.uno("SELECT codigo FROM instrumento WHERE id = ?", f -> f.getString(1), instrumento)
                    .orElseThrow(() -> Administracion.noEncontrado("Uno de los instrumentos no existe."));
            if (!codigos.add(codigo)) throw estado("No se pueden asignar dos versiones del mismo instrumento en un plan.");
        }
        long plan = db.id("INSERT INTO plan(expediente_id) VALUES (?) RETURNING id", id);
        for (long instrumento : instrumentos) {
            db.ejecutar("INSERT INTO evaluacion(plan_id, instrumento_id) VALUES (?, ?)", plan, instrumento);
        }
        evento(db, id, actor, "ASIGPLAN", "Plan #" + plan + " con " + instrumentos.size() + " instrumento(s)");
        return "Plan #" + plan + " asignado.\n" + listado(db, id);
    }

    public String plan(Sql db, Actor actor, long id) throws SQLException {
        Expedientes.lectura(db, actor, id);
        return listado(db, id);
    }

    private String listado(Sql db, long id) throws SQLException {
        List<String> filas = db.lista("""
                SELECT v.id, i.nombre, i.codigo, i.version, v.respuesta IS NOT NULL, v.puntaje
                FROM plan p JOIN evaluacion v ON v.plan_id = p.id JOIN instrumento i ON i.id = v.instrumento_id
                WHERE p.expediente_id = ? ORDER BY v.id
                """, f -> "Evaluación #%d | %s (%s v%d) | %s".formatted(f.getLong(1), f.getString(2), f.getString(3), f.getInt(4),
                f.getString(6) != null ? "Calificada: " + f.getString(6) + "/100" : f.getBoolean(5) ? "Pendiente de calificación" : "Pendiente de respuesta"), id);
        return filas.isEmpty() ? "El expediente no tiene plan asignado." : String.join("\n", filas);
    }

    public String ver(Sql db, Actor actor, long id) throws SQLException {
        Prueba prueba = prueba(db, actor, id);
        return "Evaluación #" + id + " | " + prueba.instrumento() + "\nConsigna: " + prueba.consigna()
                + (prueba.respuesta() == null ? "\nAún no respondida." : "\nRespuesta registrada: " + prueba.respuesta());
    }

    public String responder(Sql db, Actor actor, Argumentos a) throws SQLException {
        long id = a.id(0);
        Prueba prueba = prueba(db, actor, id);
        Expedientes.exigirEvaluacion(db, prueba.expediente());
        if (prueba.respuesta() != null) throw estado("La respuesta ya fue registrada y no se sobrescribe.");
        db.ejecutar("UPDATE evaluacion SET respuesta = ?, respondido_en = now() WHERE id = ?", a.texto(1, 1200), id);
        evento(db, prueba.expediente(), actor, "RESPRU", "Respuesta registrada para evaluación #" + id);
        return "Respuesta registrada en la evaluación #" + id + ".";
    }

    public String calificar(Sql db, Actor actor, Argumentos a) throws SQLException {
        long id = a.id(0);
        Prueba prueba = prueba(db, actor, id);
        if (prueba.respuesta() == null) throw estado("La prueba no fue respondida.");
        if (prueba.puntaje() != null) throw estado("La prueba ya está calificada.");
        BigDecimal puntaje = a.puntaje(1);
        db.ejecutar("UPDATE evaluacion SET puntaje = ?, observacion = ?, calificado_en = now() WHERE id = ?",
                puntaje, a.texto(2, 400), id);
        evento(db, prueba.expediente(), actor, "CALPRU", "Calificación manual de evaluación #" + id);
        return "Calificación registrada: " + puntaje.toPlainString() + "/100 (demo).";
    }

    public String consolidar(Sql db, Actor actor, long id) throws SQLException {
        Expedientes.lectura(db, actor, id);
        if (db.existe("SELECT 1 FROM consolidado WHERE expediente_id = ?", id)) throw estado("Ya existe un resultado consolidado.");
        BigDecimal promedio = db.uno("""
                SELECT round(avg(v.puntaje), 2) FROM plan p JOIN evaluacion v ON v.plan_id = p.id
                WHERE p.expediente_id = ? HAVING count(*) > 0 AND count(*) = count(v.puntaje)
                """, f -> f.getBigDecimal(1), id).orElseThrow(() -> estado("Se necesita un plan con todas las pruebas calificadas."));
        db.ejecutar("INSERT INTO consolidado(expediente_id, puntaje, regla) VALUES (?, ?, ?)", id, promedio, REGLA);
        evento(db, id, actor, "CONSOL", "Resultado consolidado por regla " + REGLA);
        return "Puntaje consolidado: " + promedio.toPlainString() + "/100 (" + REGLA + ").";
    }

    private Prueba prueba(Sql db, Actor actor, long id) throws SQLException {
        Prueba prueba = db.uno("""
                SELECT p.expediente_id, i.nombre || ' (' || i.codigo || ' v' || i.version || ')', i.consigna, v.respuesta, v.puntaje
                FROM evaluacion v JOIN plan p ON p.id = v.plan_id JOIN instrumento i ON i.id = v.instrumento_id
                WHERE v.id = ?
                """, f -> new Prueba(f.getLong(1), f.getString(2), f.getString(3), f.getString(4), f.getBigDecimal(5)), id)
                .orElseThrow(() -> Administracion.noEncontrado("Evaluación inexistente o no autorizada."));
        Expedientes.lectura(db, actor, prueba.expediente());
        return prueba;
    }

    private static ErrorComando estado(String mensaje) {
        return new ErrorComando("ESTADO", mensaje);
    }
}
