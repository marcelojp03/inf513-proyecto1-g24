package bo.edu.uagrm.talentos;

import bo.edu.uagrm.talentos.correo.CorreoRed;
import bo.edu.uagrm.talentos.correo.FlujoCorreo;
import bo.edu.uagrm.talentos.correo.SondaRelay;
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
        if (args.length > 1 || !java.util.Set.of("", "--help", "--una-vez", "--crear-admin", "--verificar-correo", "--probar-relay").contains(opcion)) {
            System.err.println("Opción no reconocida. Usá --help.");
            return 2;
        }
        if (opcion.equals("--help")) {
            System.out.println("Banco de Talentos 18B | java -jar banco-talentos-mail.jar [--una-vez | --crear-admin | --verificar-correo | --probar-relay]\n"
                    + "Configuración por variables de entorno. Consultá README.md.");
            return 0;
        }

        var configuracion = new Configuracion(System.getenv());
        boolean verificar = opcion.equals("--verificar-correo");
        boolean probarRelay = opcion.equals("--probar-relay");
        BaseDatos base = null;
        Configuracion.Correo correo = null;
        String primerAdmin = null;
        java.time.Instant desde;
        try {
            desde = configuracion.correoDesde();
            if (probarRelay) {
                // Solo hace falta la configuración del servidor a probar.
            } else if (verificar) {
                correo = configuracion.correo();
            } else {
                base = configuracion.baseDatos();
                primerAdmin = opcion.equals("--crear-admin") ? configuracion.direccion("ADMIN_EMAIL") : null;
                correo = primerAdmin == null ? configuracion.correo() : null;
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        try (var log = new Bitacora(Path.of(configuracion.valor("LOG_DIR", "logs")))) {
            if (probarRelay) {
                try {
                    boolean aceptado = SondaRelay.probar(configuracion.relayHost(), configuracion.relayPuerto(),
                            configuracion.relayDesde(), configuracion.relayHasta());
                    log.acceso("relay=" + (aceptado ? "ACEPTADO" : "RECHAZADO"));
                    return aceptado ? 0 : 1;
                } catch (IOException e) {
                    log.error("relay=ERROR");
                    System.err.println("No se pudo conectar con el servidor. Revisá host y puerto.");
                    return 1;
                }
            }
            if (verificar) {
                var red = new CorreoRed(correo, log);
                try {
                    int mensajes = red.contarMensajes();
                    red.enviar(correo.remitente(), "Banco de Talentos | Prueba de correo",
                            "Verificación de configuración. Si recibís este mensaje, el SMTP configurado puede entregar respuestas.");
                    log.acceso("verificacion=OK mensajes=" + mensajes);
                    System.out.println("POP3 OK (" + mensajes + " mensajes). SMTP OK: prueba enviada a " + correo.remitente() + ".");
                    return 0;
                } catch (MessagingException e) {
                    log.error("verificacion=FALLO motivo=" + FlujoCorreo.diagnostico(e));
                    System.err.println("Falló la verificación. Revisá logs/error.log.");
                    return 1;
                }
            }
            try {
                base.lectura(d -> d.existe("SELECT 1 FROM usuario"));
                if (primerAdmin != null) {
                    String admin = primerAdmin;
                    boolean creado = base.transaccion(d -> {
                        d.ejecutar("INSERT INTO usuario(correo, rol) VALUES (?, 'ADMINISTRADOR') ON CONFLICT (correo) DO NOTHING", admin);
                        return d.existe("SELECT 1 FROM usuario WHERE correo = ? AND rol = 'ADMINISTRADOR' AND activo", admin);
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
            var flujo = new FlujoCorreo(base, log, desde, configuracion.tokenMinutos());
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
            System.out.println("BD: " + baseVisible(configuracion.valor("DB_URL", "")) + " | POP3: " + correo.pop().host() + ":" + correo.pop().puerto()
                    + " | SMTP: " + correo.smtp().host() + ":" + correo.smtp().puerto());
            System.out.println("Sondeo cada " + correo.intervaloSegundos() + " s | Código válido " + configuracion.tokenMinutos() + " min");
            do {
                var ciclo = ciclo(flujo, red, log);
                if (unaVez) return ciclo.ok() ? 0 : 1;
                try {
                    // Tras procesar algo se vuelve a mirar enseguida; en reposo se respeta el intervalo.
                    long espera = ciclo.actividad() > 0 ? 3 : correo.intervaloSegundos();
                    Thread.sleep(espera * 1000L);
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

    private record Ciclo(boolean ok, int actividad) {}

    private static Ciclo ciclo(FlujoCorreo flujo, CorreoRed red, Bitacora log) {
        boolean ok = true;
        int actividad = 0;
        try {
            actividad += red.leer(flujo);
        } catch (MessagingException e) {
            log.error("canal=POP3 resultado=NO_DISPONIBLE");
            ok = false;
        } catch (SQLException e) {
            log.error("canal=JDBC fase=ENTRADA sqlstate=" + estadoSql(e));
            ok = false;
        }
        try {
            actividad += flujo.despachar(red::enviar);
        } catch (SQLException e) {
            log.error("canal=JDBC fase=SALIDA sqlstate=" + estadoSql(e));
            ok = false;
        }
        if (!ok) System.err.println("Ciclo incompleto. Revisá la bitácora de errores.");
        return new Ciclo(ok, actividad);
    }

    private static String estadoSql(SQLException e) {
        String estado = e.getSQLState();
        return estado != null && estado.matches("[A-Z0-9]{5}") ? estado : "-";
    }

    private static String baseVisible(String url) {
        String limpio = url.replaceFirst("^jdbc:postgresql://", "");
        int consulta = limpio.indexOf('?');
        return consulta >= 0 ? limpio.substring(0, consulta) : limpio;
    }
}
