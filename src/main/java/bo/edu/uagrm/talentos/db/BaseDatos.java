package bo.edu.uagrm.talentos.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class BaseDatos {
    private final String url;
    private final Properties propiedades = new Properties();

    public BaseDatos(String url, String usuario, String clave) {
        if (!url.startsWith("jdbc:postgresql:")) throw new IllegalArgumentException("DB_URL debe ser una URL JDBC PostgreSQL.");
        this.url = url;
        propiedades.setProperty("user", usuario);
        propiedades.setProperty("password", clave);
        propiedades.setProperty("connectTimeout", "10");
        propiedades.setProperty("socketTimeout", "20");
    }

    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar(Sql db) throws SQLException;
    }

    public <T> T transaccion(Operacion<T> operacion) throws SQLException {
        try (Connection conexion = abrir()) {
            conexion.setAutoCommit(false);
            Sql db = new Sql(conexion);
            try {
                // Serializa mutaciones y despacho incluso si se abren dos pollers sobre el mismo esquema.
                db.existe("SELECT pg_advisory_xact_lock(18513, 1)");
                T resultado = operacion.ejecutar(db);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    public <T> T lectura(Operacion<T> operacion) throws SQLException {
        try (Connection conexion = abrir()) {
            return operacion.ejecutar(new Sql(conexion));
        }
    }

    private Connection abrir() throws SQLException {
        Connection conexion = DriverManager.getConnection(url, propiedades);
        try {
            conexion.setSchema("banco_talentos_mail");
            return conexion;
        } catch (SQLException e) {
            conexion.close();
            throw e;
        }
    }
}
