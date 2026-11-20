# Guía de uso · Banco de Talentos por correo

Esta guía explica cómo operar el sistema del Proyecto N.º 1 sin tocar el código. El sistema no tiene pantalla: se usa **mandando correos** al buzón del grupo y leyendo la respuesta.

## 1. Antes de empezar

El programa tiene que estar corriendo en la computadora que tiene el buzón configurado:

```powershell
cd "Primer parcial"
.\ejecutar.ps1
```

Sin eso, los correos quedan en el buzón y no se procesan. `--una-vez` procesa un solo ciclo y termina, útil para probar.

Cada integrante necesita estar registrado con **su propio correo** y un rol:

| Rol | Puede |
|---|---|
| `ADMINISTRADOR` | Usuarios, empresas y todo lo del personal |
| `RECLUTADOR` | Empresas, cargos, instrumentos, clasificación, planes, calificación, informes y reportes |
| `CANDIDATO` | Su expediente, sus consentimientos, sus pruebas y sus informes |

Si tu correo no está registrado, cualquier comando devuelve `[IDENTIDAD]`.

## 2. Cómo se manda un correo

Todo va en el **asunto**, con esta forma:

```text
COMANDO["parámetro1","parámetro2"]
```

Reglas:

- Sin parámetros: `HELP[]` o `LISEMP[]`.
- Cada parámetro va entre comillas dobles y separado por coma.
- Los `SI`/`NO` van sin tilde.
- Las competencias se separan con punto y coma: `"excel;atencion;redaccion"`.
- Se ignoran espacios extra y se aceptan comillas tipográficas.
- Nada de adjuntos ni HTML: solo texto.

Ejemplo de asunto: `CONSEXP["3"]`

## 3. Primer paso: pedir el código de acceso

Mandá un correo **desde tu cuenta registrada** al buzón del grupo:

- **Asunto:** `ACCESO[]`
- **Cuerpo:** vacío

El sistema responde con un código:

```text
TOKEN: AbC123...
```

Ese código vale **15 minutos** y sirve para **varios comandos**. Si pedís otro, el anterior deja de servir.

## 4. Cómo usar el código

Para cada comando, mandá un correo nuevo:

- **Asunto:** el comando, por ejemplo `INSEMP["Acme SRL","rrhh@acme.test"]`
- **Cuerpo:** una sola línea de texto plano:

```text
TOKEN: AbC123...
```

No respondas el correo del código: mandá uno nuevo. El sistema contesta al remitente con el resultado o con un error explicativo.

> Las respuestas llegan desde la cuenta configurada en `MAIL_FROM`, no desde el buzón de la cátedra. En esta instalación es una cuenta Gmail: revisá spam la primera vez y marcá el remitente como seguro.

## 5. Catálogo de comandos

### CU1 · Usuarios

| Comando | Parámetros | Rol |
|---|---|---|
| `INSUSU` | correo, rol | Administrador |
| `LISUSU` | — | Administrador |
| `ACTUSU` | usuarioId, rol, activo (`SI`/`NO`) | Administrador |

### CU2 · Candidatos, privacidad y consentimiento

| Comando | Parámetros | Rol |
|---|---|---|
| `INSEXP` | nombre, carrera, meses, competencias, disponible | Candidato |
| `VINCEXP` | usuarioId, cargoId | Personal |
| `MISEXP` | — | Candidato |
| `ACTEXP` | expId, nombre, carrera, meses, competencias, disponible | Candidato |
| `CONSEXP` | expId | Todos |
| `CONSEN` | expId, finalidad, otorgado, destinatarioOvacío | Candidato |

Finalidades de `CONSEN`: `EVALUACION` o `INFORME_EMPRESA`. Para autorizar el informe a la empresa, el cuarto parámetro debe ser el correo de contacto de esa empresa; en los demás casos va vacío (`""`).

### CU3 · Empresas, cargos e instrumentos

| Comando | Parámetros | Rol |
|---|---|---|
| `INSEMP` | nombre, correoInformes | Administrador |
| `ACTEMP` | empresaId, nombre, correoInformes | Administrador |
| `LISEMP` | — | Personal |
| `INSCAR` | empresaId, nombre, carrera, mesesMínimos, competencias, funciones | Personal |
| `ACTCAR` | cargoId, nombre, carrera, mesesMínimos, competencias, funciones | Personal |
| `LISCAR` | — | Personal |
| `INSINS` | código, versión, nombre, categoría, consigna | Personal |
| `LISINS` | — | Personal |

### CU4 · Clasificación, planes y trazabilidad

| Comando | Parámetros | Rol |
|---|---|---|
| `CLASEXP` | expId, clasificación (`APTO`/`OBSERVADO`/`NO_APTO`), motivo | Personal |
| `ASIGPLAN` | expId, ids de instrumentos separados por coma | Personal |
| `LISPLAN` | expId | Todos |
| `HISEXP` | expId | Personal |

### CU5 · Pruebas y calificación

| Comando | Parámetros | Rol |
|---|---|---|
| `VERPRU` | evaluaciónId | Todos |
| `RESPRU` | evaluaciónId, respuesta | Candidato |
| `CALPRU` | evaluaciónId, puntaje (0–100), observación | Personal |
| `CONSOL` | expId | Personal |

### CU6 · Búsqueda, ranking y matriz

| Comando | Parámetros | Rol |
|---|---|---|
| `BUSPER` | cargoId, puntajeMínimo | Personal |
| `RANKING` | cargoId | Personal |
| `MATRIZ` | cargoId | Personal |

### CU7 · Informes

| Comando | Parámetros | Rol |
|---|---|---|
| `INFORME` | expId, recomendación | Personal |
| `LISINF` | expId | Todos |
| `INFCON` | informeId | Todos |
| `DESINF` | informeId | Personal |

### CU8 · Reportes

| Comando | Parámetros | Rol |
|---|---|---|
| `ESTAD` | — | Personal |
| `REPCAR` | cargoId | Personal |

### Ayuda

`HELP[]` devuelve el catálogo con roles. No requiere código.

## 6. Recorrido de demostración

Los datos son ficticios. Reemplazá los IDs por los que devuelva cada respuesta.

1. **Administrador**
   - `ACCESO[]` → guardar el código.
   - `INSUSU["reclutador@demo.test","RECLUTADOR"]`
   - `INSUSU["ana@demo.test","CANDIDATO"]`
   - `INSEMP["Acme SRL","rrhh@acme.test"]`
   - `INSCAR["1","Analista","Administracion","12","excel;atencion","Registrar y analizar datos"]`
   - `INSINS["TEC-01","1","Prueba técnica demo","Aptitud","Responder por qué elegirías la empresa."]`
2. **Candidato**
   - `ACCESO[]` → guardar el código.
   - `INSEXP["Ana Demo","Administracion","24","excel;atencion","SI"]`
   - `CONSEN["1","EVALUACION","SI",""]`
3. **Personal**
   - `VINCEXP["2","1"]`
   - `CLASEXP["2","APTO","Revisión documental conforme"]`
   - `ASIGPLAN["2","1"]`
   - `LISPLAN["2"]` → obtener el `evaluaciónId`
4. **Candidato**
   - `VERPRU["1"]`
   - `RESPRU["1","Mi respuesta de ejemplo"]`
5. **Personal**
   - `CALPRU["1","80","Respuesta suficiente"]`
   - `CONSOL["2"]`
   - `BUSPER["1","60"]`, `RANKING["1"]`, `MATRIZ["1"]`
   - `INFORME["2","Cumple el perfil del cargo"]`
6. **Candidato**
   - `CONSEN["2","INFORME_EMPRESA","SI","rrhh@acme.test"]`
7. **Personal**
   - `DESINF["1"]`
   - `INFCON["1"]`
   - `ESTAD[]`, `REPCAR["1"]`

## 7. Errores frecuentes

| Respuesta | Significa |
|---|---|
| `[SINTAXIS]` | Comillas sin cerrar, corchete faltante o texto después del `]` |
| `[CANTIDAD]` | Número de parámetros distinto al del catálogo |
| `[PARAMETRO]` | Tipo o formato inválido (ID, correo, puntaje, opción) |
| `[IDENTIDAD]` | Falta el `TOKEN`, venció o no corresponde a tu cuenta |
| `[PERMISO]` | Tu rol no puede ejecutar ese comando |
| `[CONSENTIMIENTO]` | Falta consentimiento de evaluación vigente |
| `[ESTADO]` | La operación no corresponde al estado actual (por ejemplo, calificar sin respuesta) |
| `[DUPLICADO]` | Ya existe un registro con esos datos |
| `[NO_ENCONTRADO]` | El ID no existe o no te pertenece |
| `[TAMANO]` | El correo supera 64 KiB |
| `[COMANDO]` | El comando no existe; probá `HELP[]` |

## 8. Reglas y cuidados
- **El código es personal.** No lo reenvíes ni lo pongas en el asunto.
- **No mandes contraseñas ni datos sensibles** en el cuerpo del correo.
- **No hay adjuntos**: el parcial no incluye OCR ni IA.
- Los correos anteriores a `MAIL_DESDE` se registran como ignorados y no reciben respuesta; sirve para cuentas reutilizadas entre semestres.
- La calificación y el ranking son **DEMO_V1**: puntaje manual y fórmula de demostración, no un baremo institucional.
- El sistema no borra correos del buzón: los identifica por `UIDL` y no los reprocesa.

## 9. Si la respuesta no llega

Primero revisá spam. Si el correo de respuesta no aparece:

- El buzón de la cátedra **recibe bien** por POP3; el problema está en la salida.
- Su SMTP rechaza relay hacia destinatarios externos desde IPs de hogar, con `550 5.7.1 Relaying denied`, y solo anuncia `AUTH CRAM-MD5`.
- Por eso esta instalación envía las respuestas por **Gmail con App Password**. El remitente que ves es la cuenta Gmail, no el buzón de la cátedra.
- El grupo solicitó al docente habilitar las IPs de los hogares. Mientras eso no ocurra, no cambies el SMTP a `mail.tecnoweb.org.bo`, porque el envío va a fallar.
- Si el docente habilita el relay, alcanza con volver `SMTP_*` al servidor de la cátedra y poner `MAIL_FROM` en el buzón.

Los pedidos de habilitación con nombres e IPs se guardan fuera del repositorio: son datos personales y el repositorio es público.

## 10. Base de datos: local o de la cátedra

La base se elige en `.env`; el esquema es el mismo (14 tablas) en las dos:

| Base | Cuándo usarla |
|---|---|
| `127.0.0.1/banco_talentos_mail` | Desarrollo y pruebas en tu máquina |
| `db.tecnoweb.org.bo/db_grupo24sa` | Demo compartida, si el docente consulta desde su entorno |

Los datos **no se sincronizan**: si cambiás de base, cambiás de datos. El esquema propio `banco_talentos_mail` ya está creado en la base de la cátedra y el esquema `public` de esa base pertenece a grupos anteriores: no se toca.

Regla importante: **un solo equipo corre el poller por buzón**. Si dos personas lo dejan corriendo con bases distintas, ambas leen los mismos correos y cada una responde, así que el remitente recibe duplicados.

## 11. Varios integrantes

Sí, cada integrante puede operar con su propio correo:

1. Un administrador registra cada dirección con `INSUSU["correo@dominio","ROL"]`.
2. Cada persona pide su código con `ACCESO[]` desde su cuenta y usa ese `TOKEN` durante 15 minutos.
3. Todos escriben al mismo buzón y cada respuesta vuelve a quien escribió.
4. Los candidatos pueden hacer su propio recorrido (CU2 y CU5); los reclutadores, el resto.

Usá direcciones reales solo con permiso de su titular. Para una demostración alcanza con datos ficticios, y conviene no registrar direcciones de terceros sin avisarles.

## 12. Dónde va esta guía

Este archivo se versiona en el repositorio (`docs/GUIA_USO.md`) porque no contiene secretos y forma parte de la entrega. Las credenciales van únicamente en `.env`, que está ignorado por Git.

```text
Primer parcial/
├── docs/GUIA_USO.md        ← se sube
├── README.md               ← se sube
├── .env                    ← NO se sube
└── .env.example            ← se sube, con valores vacíos
```
