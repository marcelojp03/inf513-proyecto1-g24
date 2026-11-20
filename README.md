# Banco de Talentos · Proyecto N.º 1 (INF513-SA)

Aplicación Java de consola que opera el Banco de Talentos del Grupo 18B **por correo electrónico**: lee un buzón POP3, interpreta el asunto con la sintaxis `COMANDO["param1","param2"]`, valida identidad y permisos, ejecuta la operación sobre PostgreSQL por JDBC y responde por SMTP.

Entrega del primer parcial de Tecnología Web, gestión 2026-2. Es un repositorio y un esquema independientes del sistema web React/Laravel del proyecto anual.

## 1. Alcance

Los casos de uso aprobados por el docente son CU1–CU8. Los RF01–RF24 históricos se usaron como antecedente del dominio, no como lista de entrega.

| CU | Caso de uso | Comandos |
|---|---|---|
| CU1 | Gestión de usuarios (Administrador, Reclutador, Candidato) | `INSUSU`, `LISUSU`, `ACTUSU` |
| CU2 | Candidatos, privacidad y consentimiento | `INSEXP`, `VINCEXP`, `MISEXP`, `ACTEXP`, `CONSEXP`, `CONSEN` |
| CU3 | Empresas, perfiles de cargo e instrumentos | `INSEMP`, `ACTEMP`, `LISEMP`, `INSCAR`, `ACTCAR`, `LISCAR`, `INSINS`, `LISINS` |
| CU4 | Clasificación, planes de evaluación y trazabilidad | `CLASEXP`, `ASIGPLAN`, `LISPLAN`, `HISEXP` |
| CU5 | Ejecución, resultados y calificación | `VERPRU`, `RESPRU`, `CALPRU`, `CONSOL` |
| CU6 | Búsqueda, ranking y matriz comparativa | `BUSPER`, `RANKING`, `MATRIZ` |
| CU7 | Informes y despacho por correo | `INFORME`, `LISINF`, `INFCON`, `DESINF` |
| CU8 | Reportes y estadísticas | `ESTAD`, `REPCAR` |

Además: `HELP[]` y `ACCESO[]`.

## 2. Arquitectura

```
Buzón POP3 (UIDL) → CorreoRed → FlujoCorreo → Analizador → Catálogo → dominio → JDBC/PostgreSQL
                                                                        ↓
                                                    correo_salida ──→ SMTP
```

| Paquete | Responsabilidad |
|---|---|
| `Main` | Arranque, ciclo de polling, `--crear-admin`, reconciliación de envíos |
| `Configuracion` | Lectura y validación de variables de entorno |
| `Bitacora` | `logs/access.log` y `logs/error.log`, sin credenciales ni cuerpos |
| `comando` | Analizador del asunto, validación de parámetros, catálogo y permisos |
| `dominio` | Reglas de CU1–CU8; no conoce el correo |
| `correo` | POP3/SMTP, autenticación por token, cola de salida |
| `db` | Conexión JDBC, transacciones, `PreparedStatement` |

Decisiones de diseño:

- **El `From` no autentica.** `ACCESO[]` emite un código de sesión, ligado a la cuenta y válido 15 minutos por defecto (`TOKEN_MINUTES`; en la demo, 30) para varios comandos. El comando viaja en el asunto y el código en el cuerpo (`TOKEN: ...`). Pedir otro código invalida el anterior.
- Guía de uso paso a paso: [`docs/GUIA_USO.md`](docs/GUIA_USO.md).
- Lista de comandos para copiar: [`docs/COMANDOS.md`](docs/COMANDOS.md).
- Guion de demostración con roles y tiempos: [`docs/GUION_DEMO.md`](docs/GUION_DEMO.md).
- Flujo completo para copiar y pegar: [`docs/FLUJO_DEMO.md`](docs/FLUJO_DEMO.md).
- Guía con solo dos cuentas (admin y candidato): [`docs/GUIA_PERSONAL.md`](docs/GUIA_PERSONAL.md).
- **El código se genera al despachar**, nunca se guarda en claro: en `token_acceso` solo queda su SHA-256.
- **Idempotencia POP3**: cada mensaje se identifica por `UIDL` y se registra por huella; releer el buzón no reprocesa.
- **Cola transaccional**: se reclama el envío (`ENVIANDO`) antes de hablar con SMTP. Un corte entre el envío y el commit deja el registro `INDETERMINADO` y no se reenvía solo, para no duplicar informes.
- **Canal de correo en la cátedra**: POP3 y SMTP usan `mail.tecnoweb.org.bo` (110 y 25). El relay externo estuvo bloqueado y las respuestas salieron por Gmail; cuando la cátedra habilitó las IPs, se volvió al SMTP propio. `--probar-relay` muestra el diálogo y el veredicto. La alternativa Gmail sigue documentada como respaldo.
- **Consentimiento**: sin consentimiento de evaluación vigente no se asignan ni califican pruebas ni se despachan respuestas con datos del expediente. El informe empresarial exige además autorización para el destinatario actual.
- **Todo acceso a datos usa `PreparedStatement`** y las mutaciones van en transacción con bloqueo de asesoría (`pg_advisory_xact_lock`).

## 3. Base de datos

Base `banco_talentos_mail`, esquema del mismo nombre. 14 tablas:

- `usuario` (rol y estado), `empresa`, `cargo`, `instrumento` (código + versión).
- `expediente` (proceso A universitario o B empresarial), `consentimiento` (por finalidad y destinatario).
- `plan`, `evaluacion`, `consolidado`, `informe`, `evento_expediente` (trazabilidad).
- `correo_entrada` (deduplicación), `correo_salida` (cola), `token_acceso` (huellas).

Separaciones que el modelo respeta: persona ≠ expediente; empresa ≠ cuenta; PPA ≠ prueba técnica; autopercepción ≠ nota del evaluador; perfil interno ≠ ficha pública ≠ informe empresarial.

## 4. Instalación

Requisitos: JDK 17+ (probado con 23), Maven 3.9+, PostgreSQL 14+ (probado con 17 y 18).

`ejecutar.ps1` elige el JDK en este orden: `JAVA_EXE`, `JAVA_HOME`, el `jdk-*` más reciente de `C:\Program Files\Java`, o el `java` del PATH. En esta máquina el `java` del PATH es 8, por eso el orden anterior importa.

```powershell
cd "Primer parcial"
mvn package
$env:PGPASSWORD = 'tu_clave'
createdb -h 127.0.0.1 -U postgres banco_talentos_mail
psql -h 127.0.0.1 -U postgres -d banco_talentos_mail -f sql/01_esquema.sql
Remove-Item Env:\PGPASSWORD
Copy-Item .env.example .env
notepad .env
```

`ejecutar.ps1` carga `.env` en el proceso y arranca el JAR. `.env` está ignorado por Git y no debe incluirse en el ZIP.

Variables obligatorias: `DB_URL`, `DB_USER`, `DB_PASSWORD`. Para el ciclo de correo: `POP3_HOST`, `POP3_USER`, `POP3_PASSWORD`, `SMTP_HOST`, `MAIL_FROM`. Opcionales: `POP3_SECURITY` (`TLS` por defecto), `SMTP_SECURITY` (`STARTTLS`), puertos, `SMTP_USER`/`SMTP_PASSWORD` si el servidor exige autenticación, `POLL_SECONDS` (15), `LOG_DIR` (`logs`) y `MAIL_DESDE` (AAAA-MM-DD) para ignorar correos anteriores en cuentas reutilizadas entre semestres.

El correo remoto exige TLS. `PLAIN` solo se acepta contra `localhost`; si el servidor de la cátedra solo ofrece 110/25 sin cifrado, hay que habilitarlo explícitamente con `ALLOW_PLAINTEXT_MAIL=SI`, asumiendo que las credenciales viajan sin cifrar.

Primer administrador:

```powershell
.\ejecutar.ps1 --crear-admin      # usa ADMIN_EMAIL
.\ejecutar.ps1 --una-vez          # un ciclo, útil para probar
.\ejecutar.ps1 --verificar-correo # POP3 + envío de prueba a MAIL_FROM
.\ejecutar.ps1 --probar-relay    # muestra si el SMTP de la cátedra acepta un destinatario externo
.\ejecutar.ps1                    # bucle continuo
```

## 5. Sintaxis del asunto

```text
COMANDO["parametro1","parametro2"]
```

- Nombre en mayúsculas o minúsculas; se normaliza.
- Espacios y tabulaciones entre tokens se ignoran.
- UTF-8 y comillas tipográficas (`“ ” « »`) se normalizan a `"`.
- Se admiten `\"` y `\\` dentro de una cadena.
- Comillas sin cerrar, texto después del `]`, parámetros de más o de menos y tipos inválidos producen un error descriptivo por correo.

Ejemplo: `CONSEXP["12"]`.

## 6. Recorrido de demostración

Los datos son ficticios. El personal se crea con el administrador; las cuentas de candidato se crean antes de inscribirse.

1. `HELP[]` — catálogo completo.
2. `ACCESO[]` — recibís un código; respondé con un correo nuevo: asunto `INSUSU["reclutador@demo.test","RECLUTADOR"]`, cuerpo `TOKEN: ...`.
3. Alta de empresa, cargo e instrumento: `INSEMP`, `INSCAR`, `INSINS`.
4. Candidato: `INSEXP`, luego `CONSEN["<expId>","EVALUACION","SI",""]`.
5. Personal: `VINCEXP`, `CLASEXP` (`APTO`), `ASIGPLAN`, `RESPRU` (candidato), `CALPRU`, `CONSOL`.
6. `INFORME`, `CONSEN["<expId>","INFORME_EMPRESA","SI","<correo de la empresa>"]`, `DESINF`, `INFCON`.
7. `BUSPER`, `RANKING`, `MATRIZ`, `ESTAD`, `REPCAR`.

Para una demostración rápida, `sql/02_datos_demo.sql` carga empresa, cargo, instrumentos, tres expedientes con pruebas calificadas y consentimientos. `sql/05_datos_metricas.sql` agrega un segundo cargo, expedientes A y estados variados para los reportes. Son idempotentes y no se ejecutan solos: hay que aplicarlos a mano sobre la base elegida.

## 7. Reglas de calificación y ranking

**DEMO_V1, explícitamente de demostración.** No son reglas institucionales ni baremos aprobados.

- `CALPRU` registra un puntaje manual de 0 a 100 con observación humana.
- `CONSOL` promedia las pruebas calificadas; exige que todas lo estén y no sobrescribe un consolidado existente.
- `BUSPER`/`RANKING`/`MATRIZ` exigen expediente B `APTO`, consentimiento vigente y consolidado. La compatibilidad es 25 puntos por carrera exacta + 25 por experiencia proporcional + 25 por competencias cumplidas + 25 por consolidado/100. Los empates se ordenan por ID y la salida se limita a 50 expedientes.

## 8. Verificación ejecutada

- `mvn package` correcto.
- Esquema aplicado sobre PostgreSQL 17: 14 tablas.
- Prueba integral `IntegracionCorreoTest` con GreenMail y PostgreSQL nativo (esta instancia no usa Docker): código de acceso entregado por SMTP, comando autenticado ejecutado, token rechazado al reutilizarse, `PERMISO` para rol incorrecto, releído del buzón sin duplicados y correo anterior al corte ignorado. Resultado: 1 prueba, 0 fallos.
- Contra el servidor de la cátedra se verificaron la conexión PostgreSQL, el login POP3 y la lectura del buzón en modo solo lectura. El `HELP` del 19 de junio quedó `IGNORADO_ANTIGUO`.
- Envío real verificado: el SMTP de la cátedra acepta destinatarios externos y el mensaje completo (`250 Message accepted for delivery` hacia Gmail). También se probó el respaldo por Gmail con App Password.

## 9. Límites conocidos

- El puntaje y el ranking son de demostración.
- Un envío cortado entre SMTP y el commit queda `INDETERMINADO` y requiere revisión manual; se prefiere no duplicar antes que reenviar a ciegas.
- La cola guarda el cuerpo pendiente hasta 3 intentos; no guarda credenciales ni códigos de acceso.
- El relay de la cátedra ya está habilitado; si vuelve a bloquearse, el respaldo es Gmail. Un solo equipo debe correr el poller por buzón.
- No hay adjuntos, OCR ni IA: quedan fuera de este parcial.






