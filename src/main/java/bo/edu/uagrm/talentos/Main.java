package bo.edu.uagrm.talentos;

import bo.edu.uagrm.talentos.correo.CorreoRed;
import bo.edu.uagrm.talentos.correo.FlujoCorreo;
import bo.edu.uagrm.talentos.db.BaseDatos;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        int codigo = iniciar(args);
        if (codigo != 0) System.exit(codigo);
    }

    private static int iniciar(String[] args) {
        String opcion = args.length == 0 ? "" : args[0];
        if (args.length > 1 || !java.util.Set.of("", "--help", "--una-vez", "--crear-admin").contains(opcion)) {
            System.err.println("Opción no reconocida. Usá --help.");
            return 2;
        }
        if (opcion.equals("--help")) {
            System.out.println("Banco de Talentos 18B | java -jar banco-talentos-mail.jar [--una-vez | --crear-admin]\n"
                    + "Configuración por variables de entorno. Consultá README.md.");
            return 0;
        }

        var configuracion = new Configuracion(System.getenv());
        BaseDatos base;
        Configuracion.Correo correo;
        String primerAdmin;
        java.time.Instant desde;
        try {
            base = configuracion.baseDatos();
            primerAdmin = opcion.equals("--crear-admin") ? configuracion.direccion("ADMIN_EMAIL") : null;
            desde = configuracion.correoDesde();
            correo = primerAdmin == null ? configuracion.correo() : null;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        try (var log = new Bitacora(Path.of(configuracion.valor("LOG_DIR", "logs")))) {
            try {
                base.lectura(d -> d.existe("SELECT 1 FROM usuario"));
                if (primerAdmin != null) {
                    boolean creado = base.transaccion(d -> {
                        d.ejecutar("INSERT INTO usuario(correo, rol) VALUES (?, 'ADMINISTRADOR') ON CONFLICT (correo) DO NOTHING", primerAdmin);
                        return d.existe("SELECT 1 FROM usuario WHERE correo = ? AND rol = 'ADMINISTRADOR' AND activo", primerAdmin);
                    });
                    log.acceso("bootstrap_admin=" + (creado ? "OK" : "CUENTA_EXISTENTE"));
                    System.out.println(creado ? "Administrador activo listo." : "El correo ya está registrado con otro rol o inactivo.");
                    return creado ? 0 : 1;
                }
            } catch (SQLException e) {
                log.error("canal=JDBC fase=ARRANQUE sqlstate=" + estadoSql(e));
                System.err.println("No se pudo verificar el esquema. Revisá la configuración PostgreSQL y logs/error.log.");
                return 1;
            }
            var flujo = new FlujoCorreo(base, log, desde);
            var red = new CorreoRed(correo, log);
            try {
                flujo.reconciliar();
            } catch (SQLException e) {
                log.error("canal=JDBC fase=RECONCILIACION sqlstate=" + estadoSql(e));
                System.err.println("No se pudo reconciliar envíos pendientes.");
                return 1;
            }
            boolean unaVez = opcion.equals("--una-vez");
            System.out.println("Banco de Talentos 18B | POP3 → PostgreSQL → SMTP");
            do {
                boolean ok = ciclo(flujo, red, log);
                if (unaVez) return ok ? 0 : 1;
                try {
                    Thread.sleep(correo.intervaloSegundos() * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            } while (!Thread.currentThread().isInterrupted());
            return 0;
        } catch (IOException | java.nio.file.InvalidPathException e) {
            System.err.println("No se pudo abrir la bitácora. Revisá LOG_DIR y sus permisos.");
            return 1;
        }
    }

    private static boolean ciclo(FlujoCorreo flujo, CorreoRed red, Bitacora log) {
        boolean ok = true;
        try {
            red.leer(flujo);
        } catch (MessagingException e) {
            log.error("canal=POP3 resultado=NO_DISPONIBLE");
            ok = false;
        } catch (SQLException e) {
            log.error("canal=JDBC fase=ENTRADA sqlstate=" + estadoSql(e));
            ok = false;
        }
        try {
            flujo.despachar(red::enviar);
        } catch (SQLException e) {
            log.error("canal=JDBC fase=SALIDA sqlstate=" + estadoSql(e));
            ok = false;
        }
        if (!ok) System.err.println("Ciclo incompleto. Revisá la bitácora de errores.");
        return ok;
    }

    private static String estadoSql(SQLException e) {
        String estado = e.getSQLState();
        return estado != null && estado.matches("[A-Z0-9]{5}") ? estado : "-";
    }
}
