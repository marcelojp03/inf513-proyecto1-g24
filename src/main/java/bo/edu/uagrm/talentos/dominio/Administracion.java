package bo.edu.uagrm.talentos.dominio;

import bo.edu.uagrm.talentos.comando.Argumentos;
import bo.edu.uagrm.talentos.comando.ErrorComando;
import bo.edu.uagrm.talentos.db.Sql;

import java.sql.SQLException;
import java.util.List;

public final class Administracion {
    public enum Rol { ADMINISTRADOR, RECLUTADOR, CANDIDATO }
    public record Actor(long id, Rol rol) {}

    public String crearUsuario(Sql db, Argumentos a) throws SQLException {
        String rol = rol(a, 1);
        long id = db.id("INSERT INTO usuario(correo, rol) VALUES (?, ?) RETURNING id", a.correo(0), rol);
        return "Usuario registrado. ID: " + id + ". Rol: " + rol + ".";
    }

    public String listarUsuarios(Sql db) throws SQLException {
        return listado(db.lista("SELECT id, correo, rol, activo FROM usuario ORDER BY id LIMIT 100", f ->
                "%d | %s | %s | %s".formatted(f.getLong(1), f.getString(2), f.getString(3), f.getBoolean(4) ? "Activo" : "Inactivo")));
    }

    public String actualizarUsuario(Sql db, Actor actor, Argumentos a) throws SQLException {
        long id = a.id(0);
        String rol = rol(a, 1);
        boolean activo = a.siNo(2);
        if (id == actor.id()) throw new ErrorComando("PERMISO", "No podés cambiar tu propio rol o estado.");
        if (!rol.equals("CANDIDATO") && db.existe("SELECT 1 FROM expediente WHERE usuario_id = ?", id)) {
            throw new ErrorComando("ESTADO", "Un usuario con expedientes debe conservar el rol CANDIDATO.");
        }
        verificar(db.ejecutar("UPDATE usuario SET rol = ?, activo = ? WHERE id = ?", rol, activo, id), "Usuario inexistente.");
        db.ejecutar("DELETE FROM token_acceso WHERE usuario_id = ?", id);
        return "Usuario actualizado. ID: " + id + ".";
    }

    public String crearEmpresa(Sql db, Argumentos a) throws SQLException {
        long id = db.id("INSERT INTO empresa(nombre, correo_informes) VALUES (?, ?) RETURNING id", a.texto(0, 120), a.correo(1));
        return "Empresa registrada. ID: " + id + ".";
    }

    public String actualizarEmpresa(Sql db, Argumentos a) throws SQLException {
        long id = a.id(0);
        verificar(db.ejecutar("UPDATE empresa SET nombre = ?, correo_informes = ? WHERE id = ?",
                a.texto(1, 120), a.correo(2), id), "Empresa inexistente.");
        return "Empresa actualizada. ID: " + id + ". Si cambió el correo, hace falta un consentimiento para el nuevo destinatario.";
    }

    public String listarEmpresas(Sql db) throws SQLException {
        return listado(db.lista("SELECT id, nombre, correo_informes FROM empresa ORDER BY id LIMIT 100", f ->
                "%d | %s | %s".formatted(f.getLong(1), f.getString(2), f.getString(3))));
    }

    public String crearCargo(Sql db, Argumentos a) throws SQLException {
        long empresa = a.id(0);
        if (!db.existe("SELECT 1 FROM empresa WHERE id = ?", empresa)) throw noEncontrado("Empresa inexistente.");
        long id = db.id("""
                INSERT INTO cargo(empresa_id, nombre, carrera, experiencia_meses, competencias, funciones)
                VALUES (?, ?, ?, ?, string_to_array(?, ';'), ?) RETURNING id
                """, empresa, a.texto(1, 120), a.texto(2, 120), a.entero(3, 0, 600), a.competencias(4), a.texto(5, 500));
        return "Perfil de cargo registrado. ID: " + id + ".";
    }

    public String actualizarCargo(Sql db, Argumentos a) throws SQLException {
        long id = a.id(0);
        if (db.existe("SELECT 1 FROM expediente WHERE cargo_id = ?", id)) {
            throw new ErrorComando("ESTADO", "El perfil ya tiene candidatos vinculados. Registrá otro perfil para cambiar sus criterios.");
        }
        verificar(db.ejecutar("""
                UPDATE cargo SET nombre = ?, carrera = ?, experiencia_meses = ?, competencias = string_to_array(?, ';'), funciones = ?
                WHERE id = ?
                """, a.texto(1, 120), a.texto(2, 120), a.entero(3, 0, 600), a.competencias(4), a.texto(5, 500), id), "Cargo inexistente.");
        return "Perfil de cargo actualizado. ID: " + id + ".";
    }

    public String listarCargos(Sql db) throws SQLException {
        return listado(db.lista("""
                SELECT c.id, c.nombre, e.nombre, c.carrera, c.experiencia_meses, array_to_string(c.competencias, '; '), c.funciones
                FROM cargo c JOIN empresa e ON c.empresa_id = e.id ORDER BY c.id LIMIT 100
                """, f -> "%d | %s | %s | %s | %d meses | %s | %s".formatted(f.getLong(1), f.getString(2), f.getString(3),
                f.getString(4), f.getInt(5), f.getString(6), f.getString(7))));
    }

    public String crearInstrumento(Sql db, Argumentos a) throws SQLException {
        String codigo = a.texto(0, 24);
        if (!codigo.matches("[A-Za-z0-9_-]+")) {
            throw new ErrorComando("PARAMETRO", "El código del instrumento admite letras sin tilde, dígitos, guion y guion bajo.");
        }
        long id = db.id("""
                INSERT INTO instrumento(codigo, version, nombre, categoria, consigna) VALUES (?, ?, ?, ?, ?) RETURNING id
                """, codigo.toUpperCase(java.util.Locale.ROOT), a.entero(1, 1, 9999),
                a.texto(2, 120), a.texto(3, 80), a.texto(4, 600));
        return "Instrumento registrado. ID: " + id + ".";
    }

    public String listarInstrumentos(Sql db) throws SQLException {
        return listado(db.lista("SELECT id, codigo, version, nombre, categoria FROM instrumento ORDER BY id LIMIT 100", f ->
                "%d | %s v%d | %s | %s".formatted(f.getLong(1), f.getString(2), f.getInt(3), f.getString(4), f.getString(5))));
    }

    static ErrorComando noEncontrado(String mensaje) {
        return new ErrorComando("NO_ENCONTRADO", mensaje);
    }

    private static String rol(Argumentos a, int i) {
        return a.opcion(i, "ADMINISTRADOR", "RECLUTADOR", "CANDIDATO");
    }

    private static void verificar(int afectadas, String mensaje) {
        if (afectadas == 0) throw noEncontrado(mensaje);
    }

    private static String listado(List<String> filas) {
        return filas.isEmpty() ? "No hay registros." : "Primeros 100 registros:\n" + String.join("\n", filas);
    }
}
