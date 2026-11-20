package bo.edu.uagrm.talentos.comando;

import bo.edu.uagrm.talentos.comando.Analizador.Comando;
import bo.edu.uagrm.talentos.db.Sql;
import bo.edu.uagrm.talentos.dominio.Administracion;
import bo.edu.uagrm.talentos.dominio.Administracion.Actor;
import bo.edu.uagrm.talentos.dominio.Administracion.Rol;
import bo.edu.uagrm.talentos.dominio.Analitica;
import bo.edu.uagrm.talentos.dominio.Evaluaciones;
import bo.edu.uagrm.talentos.dominio.Expedientes;
import bo.edu.uagrm.talentos.dominio.Informes;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static bo.edu.uagrm.talentos.dominio.Administracion.Rol.*;

public final class Catalogo {
    private static final Set<Rol> ADMIN = EnumSet.of(ADMINISTRADOR);
    private static final Set<Rol> PERSONAL = EnumSet.of(ADMINISTRADOR, RECLUTADOR);
    private static final Set<Rol> TITULAR = EnumSet.of(CANDIDATO);
    private static final Set<Rol> TODOS = EnumSet.allOf(Rol.class);

    @FunctionalInterface
    private interface Accion {
        String ejecutar(Sql db, Actor actor, Argumentos a, long entrada) throws SQLException;
    }

    private record Entrada(int cu, String campos, Set<Rol> roles, Accion accion) {
        int aridad() { return campos.isEmpty() ? 0 : campos.split(",").length; }
    }

    private final Map<String, Entrada> entradas = new LinkedHashMap<>();

    public record Respuesta(String texto, Long expediente) {}

    public Catalogo() {
        var admin = new Administracion();
        var exp = new Expedientes();
        var ev = new Evaluaciones();
        var analitica = new Analitica();
        var informes = new Informes();
        registrar(1, "INSUSU", "correo,rol", ADMIN, (d, u, a, e) -> admin.crearUsuario(d, a));
        registrar(1, "LISUSU", "", ADMIN, (d, u, a, e) -> admin.listarUsuarios(d));
        registrar(1, "ACTUSU", "usuarioId,rol,activo", ADMIN, (d, u, a, e) -> admin.actualizarUsuario(d, u, a));
        registrar(2, "INSEXP", "nombre,carrera,meses,competencias,disponible", TITULAR, (d, u, a, e) -> exp.inscribir(d, u, a));
        registrar(2, "VINCEXP", "usuarioId,cargoId", PERSONAL, (d, u, a, e) -> exp.vincular(d, u, a.id(0), a.id(1)));
        registrar(2, "MISEXP", "", TITULAR, (d, u, a, e) -> exp.propios(d, u));
        registrar(2, "ACTEXP", "expId,nombre,carrera,meses,competencias,disponible", TITULAR, (d, u, a, e) -> exp.actualizar(d, u, a));
        registrar(2, "CONSEXP", "expId", TODOS, (d, u, a, e) -> exp.consultar(d, u, a.id(0)));
        registrar(2, "CONSEN", "expId,finalidad,otorgado,destinatarioOvacío", TITULAR, (d, u, a, e) -> exp.consentir(d, u, a));
        registrar(3, "INSEMP", "nombre,correoInformes", ADMIN, (d, u, a, e) -> admin.crearEmpresa(d, a));
        registrar(3, "ACTEMP", "empresaId,nombre,correoInformes", ADMIN, (d, u, a, e) -> admin.actualizarEmpresa(d, a));
        registrar(3, "LISEMP", "", PERSONAL, (d, u, a, e) -> admin.listarEmpresas(d));
        registrar(3, "INSCAR", "empresaId,nombre,carrera,mesesMínimos,competencias,funciones", PERSONAL, (d, u, a, e) -> admin.crearCargo(d, a));
        registrar(3, "ACTCAR", "cargoId,nombre,carrera,mesesMínimos,competencias,funciones", PERSONAL, (d, u, a, e) -> admin.actualizarCargo(d, a));
        registrar(3, "LISCAR", "", PERSONAL, (d, u, a, e) -> admin.listarCargos(d));
        registrar(3, "INSINS", "código,versión,nombre,categoría,consigna", PERSONAL, (d, u, a, e) -> admin.crearInstrumento(d, a));
        registrar(3, "LISINS", "", PERSONAL, (d, u, a, e) -> admin.listarInstrumentos(d));
        registrar(4, "CLASEXP", "expId,clasificación,motivo", PERSONAL, (d, u, a, e) -> exp.clasificar(d, u, a));
        registrar(4, "ASIGPLAN", "expId,idsInstrumentos", PERSONAL, (d, u, a, e) -> ev.asignar(d, u, a.id(0), a.ids(1)));
        registrar(4, "LISPLAN", "expId", TODOS, (d, u, a, e) -> ev.plan(d, u, a.id(0)));
        registrar(4, "HISEXP", "expId", PERSONAL, (d, u, a, e) -> exp.historial(d, u, a.id(0)));
        registrar(5, "VERPRU", "evaluaciónId", TODOS, (d, u, a, e) -> ev.ver(d, u, a.id(0)));
        registrar(5, "RESPRU", "evaluaciónId,respuesta", TITULAR, (d, u, a, e) -> ev.responder(d, u, a));
        registrar(5, "CALPRU", "evaluaciónId,puntaje,observación", PERSONAL, (d, u, a, e) -> ev.calificar(d, u, a));
        registrar(5, "CONSOL", "expId", PERSONAL, (d, u, a, e) -> ev.consolidar(d, u, a.id(0)));
        registrar(6, "BUSPER", "cargoId,puntajeMínimo", PERSONAL, (d, u, a, e) -> analitica.buscar(d, a.id(0), a.puntaje(1)));
        registrar(6, "RANKING", "cargoId", PERSONAL, (d, u, a, e) -> analitica.ranking(d, a.id(0)));
        registrar(6, "MATRIZ", "cargoId", PERSONAL, (d, u, a, e) -> analitica.matriz(d, a.id(0)));
        registrar(7, "INFORME", "expId,recomendación", PERSONAL, (d, u, a, e) -> informes.emitir(d, u, a.id(0), a.texto(1, 500)));
        registrar(7, "LISINF", "expId", TODOS, (d, u, a, e) -> informes.listar(d, u, a.id(0)));
        registrar(7, "INFCON", "informeId", TODOS, (d, u, a, e) -> informes.consultar(d, u, a.id(0)));
        registrar(7, "DESINF", "informeId", PERSONAL, (d, u, a, e) -> informes.despachar(d, u, e, a.id(0)));
        registrar(8, "ESTAD", "", PERSONAL, (d, u, a, e) -> analitica.estadisticas(d));
        registrar(8, "REPCAR", "cargoId", PERSONAL, (d, u, a, e) -> analitica.reporteCargo(d, a.id(0)));
    }

    private void registrar(int cu, String nombre, String campos, Set<Rol> roles, Accion accion) {
        entradas.put(nombre, new Entrada(cu, campos, roles, accion));
    }

    public boolean conoce(String nombre) {
        return nombre.equals("HELP") || nombre.equals("ACCESO") || entradas.containsKey(nombre);
    }

    public Respuesta ejecutar(Sql db, Actor actor, Comando comando, long idCorreo) throws SQLException {
        Entrada entrada = entradas.get(comando.nombre());
        if (!entrada.roles().contains(actor.rol())) throw new ErrorComando("PERMISO", "Tu rol no tiene permiso para este comando.");
        exigirCantidad(comando, entrada.aridad());
        String texto = entrada.accion().ejecutar(db, actor, new Argumentos(comando.parametros()), idCorreo);
        return new Respuesta(texto, expedienteAfectado(db, comando.nombre(), comando.parametros()));
    }

    // El despacho revalida el consentimiento del expediente cuya información viaja en la respuesta.
    private static Long expedienteAfectado(Sql db, String comando, java.util.List<String> parametros) throws SQLException {
        if (parametros.isEmpty()) return null;
        Long id = numero(parametros.get(0));
        if (id == null) return null;
        return switch (comando) {
            case "LISPLAN", "INFORME" -> id;
            case "VERPRU", "RESPRU" -> db.uno("""
                    SELECT p.expediente_id FROM evaluacion v JOIN plan p ON p.id = v.plan_id WHERE v.id = ?
                    """, f -> f.getLong(1), id).orElse(null);
            case "LISINF", "INFCON", "DESINF" -> db.uno("SELECT expediente_id FROM informe WHERE id = ?",
                    f -> f.getLong(1), id).orElse(null);
            default -> null;
        };
    }

    private static Long numero(String valor) {
        try {
            return Long.parseLong(valor.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void exigirCantidad(Comando comando, int esperados) {
        if (comando.parametros().size() != esperados) {
            throw new ErrorComando("CANTIDAD", "Se esperaban " + esperados + " parámetros para " + comando.nombre() + ". Consultá HELP[].");
        }
    }

    public String ayuda() {
        StringBuilder texto = new StringBuilder("""
                Banco de Talentos 18B · Tecnología Web
                Poné el comando en el asunto: COMANDO["parametro1","parametro2"]
                HELP[] — ayuda. ACCESO[] — código de un solo uso para una cuenta activa.
                Para otro comando, enviá un correo nuevo con la línea TOKEN: <código> como cuerpo de texto plano.
                Ejemplo: CONSEXP["1"]. Los valores SI/NO se escriben sin tilde.

                """);
        entradas.forEach((nombre, entrada) -> {
            String parametros = entrada.campos().isEmpty() ? "" : "\"" + entrada.campos().replace(",", "\",\"") + "\"";
            texto.append("CU").append(entrada.cu()).append(" | ").append(nombre).append('[').append(parametros)
                    .append("] | ").append(entrada.roles()).append('\n');
        });
        return texto.toString();
    }
}
