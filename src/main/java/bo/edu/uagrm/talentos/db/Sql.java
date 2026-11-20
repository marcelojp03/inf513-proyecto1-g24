package bo.edu.uagrm.talentos.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Sql(Connection conexion) {
    @FunctionalInterface
    public interface Fila<T> {
        T leer(ResultSet fila) throws SQLException;
    }

    public <T> List<T> lista(String sql, Fila<T> mapear, Object... parametros) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            bind(consulta, parametros);
            try (ResultSet filas = consulta.executeQuery()) {
                List<T> resultado = new ArrayList<>();
                while (filas.next()) resultado.add(mapear.leer(filas));
                return resultado;
            }
        }
    }

    public <T> Optional<T> uno(String sql, Fila<T> mapear, Object... parametros) throws SQLException {
        return lista(sql, mapear, parametros).stream().findFirst();
    }

    public long id(String sql, Object... parametros) throws SQLException {
        return uno(sql, fila -> fila.getLong(1), parametros).orElseThrow();
    }

    public boolean existe(String sql, Object... parametros) throws SQLException {
        return uno(sql, fila -> true, parametros).isPresent();
    }

    public int ejecutar(String sql, Object... parametros) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            bind(consulta, parametros);
            return consulta.executeUpdate();
        }
    }

    private static void bind(PreparedStatement consulta, Object[] parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) consulta.setObject(i + 1, parametros[i]);
    }
}
