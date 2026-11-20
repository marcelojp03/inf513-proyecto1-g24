package bo.edu.uagrm.talentos.dominio;

import bo.edu.uagrm.talentos.comando.ErrorComando;
import bo.edu.uagrm.talentos.db.Sql;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class Analitica {
    private static final int LIMITE = 50;

    private static final String COMPATIBILIDAD = """
            WITH factores AS (
                SELECT e.id,
                       CASE WHEN lower(e.carrera) = lower(c.carrera) THEN 25 ELSE 0 END AS carrera,
                       CASE WHEN c.experiencia_meses = 0 THEN 1
                            ELSE LEAST(COALESCE(e.experiencia_meses, 0), c.experiencia_meses)
                       END AS exp_cumplida,
                       GREATEST(c.experiencia_meses, 1) AS exp_requerida,
                       CASE WHEN comp.requeridas = 0 THEN 1 ELSE comp.cumplidas END AS comp_cumplidas,
                       GREATEST(comp.requeridas, 1) AS comp_requeridas,
                       0.25 * s.puntaje AS consolidado
                FROM cargo c
                JOIN expediente e ON e.cargo_id = c.id AND e.tipo = 'B' AND e.estado = 'APTO'
                JOIN usuario u ON u.id = e.usuario_id AND u.activo AND u.rol = 'CANDIDATO'
                JOIN consentimiento ce ON ce.expediente_id = e.id AND ce.finalidad = 'EVALUACION' AND ce.otorgado
                JOIN consolidado s ON s.expediente_id = e.id
                CROSS JOIN LATERAL (
                    SELECT COUNT(DISTINCT requerida) AS requeridas,
                           COUNT(DISTINCT requerida) FILTER (WHERE requerida = ANY(e.competencias)) AS cumplidas
                    FROM unnest(c.competencias) AS requisitos(requerida)
                ) comp
                WHERE c.id = ?
            ), puntajes AS (
                SELECT id,
                       ROUND(carrera, 2) AS carrera,
                       ROUND(25.0 * exp_cumplida / exp_requerida, 2) AS experiencia,
                       ROUND(25.0 * comp_cumplidas / comp_requeridas, 2) AS competencias,
                       ROUND(consolidado, 2) AS consolidado,
                       -- Un denominador común evita errores de redondeo en el total al sumar las dos proporciones.
                       ROUND(carrera + consolidado
                             + 25.0 * (exp_cumplida * comp_requeridas + comp_cumplidas * exp_requerida)
                               / (exp_requerida * comp_requeridas), 2) AS total
                FROM factores
            )
            SELECT id, carrera, experiencia, competencias, consolidado, total
            FROM puntajes
            """;

    private static final String METRICAS = """
            COUNT(e.id) AS registrados,
            COUNT(ce.expediente_id) AS consentidos,
            COUNT(*) FILTER (WHERE e.estado = 'APTO') AS aptos,
            COUNT(p.id) AS planes,
            COUNT(s.expediente_id) AS consolidados,
            ROUND(AVG(s.puntaje) FILTER (WHERE u.activo AND u.rol = 'CANDIDATO' AND ce.expediente_id IS NOT NULL), 2)
                AS promedio
            """;

    private static final String RELACIONES = """
            LEFT JOIN usuario u ON u.id = e.usuario_id
            LEFT JOIN consentimiento ce ON ce.expediente_id = e.id AND ce.finalidad = 'EVALUACION' AND ce.otorgado
            LEFT JOIN plan p ON p.expediente_id = e.id
            LEFT JOIN consolidado s ON s.expediente_id = e.id
            """;

    public String buscar(Sql db, long cargoId, BigDecimal minimo) throws SQLException {
        return listar(db, cargoId, minimo, "BUSPER", false);
    }

    public String ranking(Sql db, long cargoId) throws SQLException {
        return listar(db, cargoId, null, "RANKING", false);
    }

    public String matriz(Sql db, long cargoId) throws SQLException {
        return listar(db, cargoId, null, "MATRIZ", true);
    }

    public String estadisticas(Sql db) throws SQLException {
        List<String> tipos = db.lista("""
                SELECT tipos.tipo,
                """ + METRICAS + """
                FROM (VALUES ('A'), ('B')) AS tipos(tipo)
                LEFT JOIN expediente e ON e.tipo = tipos.tipo
                """ + RELACIONES + """
                GROUP BY tipos.tipo
                ORDER BY tipos.tipo
                """, fila -> "Tipo " + fila.getString("tipo") + "\n" + resumen(fila));
        return "ESTADÍSTICAS por tipo de expediente\n" + String.join("\n\n", tipos);
    }

    public String reporteCargo(Sql db, long cargoId) throws SQLException {
        String resumen = db.uno("""
                SELECT
                """ + METRICAS + """
                FROM cargo c
                LEFT JOIN expediente e ON e.cargo_id = c.id AND e.tipo = 'B'
                """ + RELACIONES + """
                WHERE c.id = ?
                GROUP BY c.id
                """, Analitica::resumen, cargoId).orElseThrow(Analitica::cargoInexistente);
        return "REPORTE DE CARGO " + cargoId + "\n" + resumen;
    }

    private static String listar(Sql db, long cargoId, BigDecimal minimo, String comando, boolean desglosar)
            throws SQLException {
        if (!db.existe("SELECT 1 FROM cargo WHERE id = ?", cargoId)) throw cargoInexistente();

        String consulta = COMPATIBILIDAD + (minimo == null ? "" : "WHERE total >= ?\n")
                + "ORDER BY total DESC, id ASC LIMIT ?";
        Object[] parametros = minimo == null ? new Object[]{cargoId, LIMITE} : new Object[]{cargoId, minimo, LIMITE};
        List<String> filas = db.lista(consulta, fila -> {
            String datos = Long.toString(fila.getLong("id"));
            if (desglosar) {
                datos += " | " + fila.getBigDecimal("carrera").toPlainString()
                        + " | " + fila.getBigDecimal("experiencia").toPlainString()
                        + " | " + fila.getBigDecimal("competencias").toPlainString()
                        + " | " + fila.getBigDecimal("consolidado").toPlainString();
            }
            return datos + " | " + fila.getBigDecimal("total").toPlainString();
        }, parametros);

        StringBuilder salida = new StringBuilder(comando).append(" (DEMO_V1)\nLímite: ")
                .append(LIMITE).append(" expedientes\n");
        if (filas.isEmpty()) return salida.append("No hay expedientes que cumplan los criterios.").toString();

        salida.append("Posición | Expediente | ");
        salida.append(desglosar
                ? "Carrera (pts) | Experiencia (pts) | Competencias (pts) | Consolidado (pts) | Total (pts)"
                : "Puntaje");
        for (int i = 0; i < filas.size(); i++) {
            salida.append('\n').append(i + 1).append(" | ").append(filas.get(i));
        }
        return salida.toString();
    }

    private static String resumen(ResultSet fila) throws SQLException {
        BigDecimal promedio = fila.getBigDecimal("promedio");
        return "Registrados: " + fila.getLong("registrados")
                + "\nCon consentimiento de evaluación: " + fila.getLong("consentidos")
                + "\nAPTO: " + fila.getLong("aptos")
                + "\nPlanes: " + fila.getLong("planes")
                + "\nConsolidados: " + fila.getLong("consolidados")
                + "\nPromedio consolidado: " + (promedio == null ? "no disponible" : promedio.toPlainString());
    }

    private static ErrorComando cargoInexistente() {
        return new ErrorComando("NO_ENCONTRADO", "Cargo inexistente.");
    }
}
