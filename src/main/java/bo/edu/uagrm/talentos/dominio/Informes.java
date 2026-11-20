package bo.edu.uagrm.talentos.dominio;

import bo.edu.uagrm.talentos.comando.ErrorComando;
import bo.edu.uagrm.talentos.db.Sql;
import bo.edu.uagrm.talentos.dominio.Administracion.Actor;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static bo.edu.uagrm.talentos.dominio.Expedientes.evento;

public final class Informes {
    private record Informe(long expediente, String contenido) {}

    public String emitir(Sql db, Actor actor, long expediente, String recomendacion) throws SQLException {
        Expedientes.Ficha ficha = Expedientes.lectura(db, actor, expediente);
        String puntaje = db.uno("SELECT puntaje || '/100 (' || regla || ')' FROM consolidado WHERE expediente_id = ?",
                        f -> f.getString(1), expediente)
                .orElseThrow(() -> new ErrorComando("ESTADO", "Primero consolidá los resultados del expediente."));
        List<String> resultados = db.lista("""
                SELECT i.nombre, i.codigo, i.version, v.puntaje FROM plan p JOIN evaluacion v ON v.plan_id = p.id
                JOIN instrumento i ON i.id = v.instrumento_id WHERE p.expediente_id = ? ORDER BY v.id
                """, f -> "- %s (%s v%d): %s/100".formatted(f.getString(1), f.getString(2), f.getInt(3), f.getString(4)), expediente);
        String contenido = """
                Informe individual · Banco de Talentos 18B
                Ejercicio académico con datos ficticios. No tiene validez psicométrica ni decide contrataciones.
                Expediente #%d | Proceso %s
                Resultado consolidado: %s
                %s
                Recomendación ejecutiva (revisión humana): %s
                """.formatted(expediente, ficha.tipo(), puntaje, String.join("\n", resultados), recomendacion);
        long id = db.id("INSERT INTO informe(expediente_id, autor_id, contenido) VALUES (?, ?, ?) RETURNING id",
                expediente, actor.id(), contenido);
        evento(db, expediente, actor, "INFORME", "Informe #" + id + " emitido");
        return "Informe #" + id + " emitido. Usá INFCON para consultar su contenido y estado de despacho.";
    }

    public String listar(Sql db, Actor actor, long expediente) throws SQLException {
        Expedientes.lectura(db, actor, expediente);
        List<String> filas = db.lista("""
                SELECT i.id, coalesce(s.estado, 'SIN DESPACHO') FROM informe i
                LEFT JOIN correo_salida s ON s.informe_id = i.id
                WHERE i.expediente_id = ? ORDER BY i.id DESC LIMIT 100
                """, f -> "Informe #" + f.getLong(1) + " | " + f.getString(2), expediente);
        return filas.isEmpty() ? "No hay informes emitidos." : "Últimos 100 informes:\n" + String.join("\n", filas);
    }

    public String consultar(Sql db, Actor actor, long id) throws SQLException {
        Informe informe = leer(db, actor, id);
        String estado = db.uno("SELECT estado FROM correo_salida WHERE informe_id = ?", f -> f.getString(1), id)
                .orElse("SIN DESPACHO");
        return "Informe #" + id + " | Despacho: " + estado + "\n" + informe.contenido();
    }

    public String despachar(Sql db, Actor actor, long entrada, long id) throws SQLException {
        Informe informe = leer(db, actor, id);
        String destino = destinoAutorizado(db, id).orElseThrow(() -> new ErrorComando("CONSENTIMIENTO",
                "El despacho exige un expediente B con consentimiento de evaluación y autorización para el contacto empresarial actual."));
        if (db.existe("""
                SELECT 1 FROM correo_salida WHERE informe_id = ? AND estado IN ('PENDIENTE', 'ENVIANDO', 'ACEPTADO_SMTP')
                """, id)) {
            throw new ErrorComando("ESTADO", "El informe ya tiene un despacho vigente; consultá su estado con INFCON.");
        }
        db.ejecutar("""
                INSERT INTO correo_salida(entrada_id, tipo, destino, asunto, cuerpo, actor_id, rol_actor, informe_id)
                VALUES (?, 'INFORME', ?, ?, ?, ?, ?, ?)
                """, entrada, destino, "Banco de Talentos | Informe #" + id, informe.contenido(), actor.id(), actor.rol().name(), id);
        evento(db, informe.expediente(), actor, "DESINF", "Despacho del informe #" + id + " en cola");
        return "Informe #" + id + " en cola SMTP. Consultá su estado con INFCON; aún no confirma recepción.";
    }

    public static Optional<String> destinoAutorizado(Sql db, long informe) throws SQLException {
        return db.uno("""
                SELECT m.correo_informes FROM informe i JOIN expediente x ON x.id = i.expediente_id
                JOIN usuario u ON u.id = x.usuario_id JOIN cargo c ON c.id = x.cargo_id JOIN empresa m ON m.id = c.empresa_id
                JOIN consentimiento t ON t.expediente_id = x.id AND t.finalidad = 'EVALUACION' AND t.otorgado
                JOIN consentimiento a ON a.expediente_id = x.id AND a.finalidad = 'INFORME_EMPRESA' AND a.otorgado
                WHERE i.id = ? AND x.tipo = 'B' AND u.activo AND a.destino = m.correo_informes
                """, f -> f.getString(1), informe);
    }

    private Informe leer(Sql db, Actor actor, long id) throws SQLException {
        Informe informe = db.uno("SELECT expediente_id, contenido FROM informe WHERE id = ?",
                f -> new Informe(f.getLong(1), f.getString(2)), id)
                .orElseThrow(() -> Administracion.noEncontrado("Informe inexistente o no autorizado."));
        Expedientes.lectura(db, actor, informe.expediente());
        return informe;
    }
}
