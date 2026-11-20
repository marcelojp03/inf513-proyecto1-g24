# Lista de comandos · referencia rápida

Todos los comandos van en el **asunto** del correo, con esta forma:

```text
COMANDO["parámetro1","parámetro2"]
```

El cuerpo lleva una sola línea cuando el comando requiere sesión:

```text
TOKEN: <código>
```

`HELP[]` y `ACCESO[]` no requieren código. `HELP[]` devuelve esta misma lista por correo.

## CU1 · Usuarios

| Comando | Parámetros | Rol | Ejemplo |
|---|---|---|---|
| `INSUSU` | correo, rol | Administrador | `INSUSU["ana@demo.test","CANDIDATO"]` |
| `LISUSU` | — | Administrador | `LISUSU[]` |
| `ACTUSU` | usuarioId, rol, activo | Administrador | `ACTUSU["2","RECLUTADOR","SI"]` |

Roles: `ADMINISTRADOR`, `RECLUTADOR`, `CANDIDATO`.

## CU2 · Candidatos, privacidad y consentimiento

| Comando | Parámetros | Rol | Ejemplo |
|---|---|---|---|
| `INSEXP` | nombre, carrera, meses, competencias, disponible | Candidato | `INSEXP["Ana Demo","Administracion","24","excel;atencion","SI"]` |
| `VINCEXP` | usuarioId, cargoId | Personal | `VINCEXP["2","1"]` |
| `MISEXP` | — | Candidato | `MISEXP[]` |
| `ACTEXP` | expId, nombre, carrera, meses, competencias, disponible | Candidato | `ACTEXP["1","Ana Demo","Administracion","30","excel;atencion;redaccion","SI"]` |
| `CONSEXP` | expId | Todos | `CONSEXP["1"]` |
| `CONSEN` | expId, finalidad, otorgado, destinatarioOvacío | Candidato | `CONSEN["1","EVALUACION","SI",""]` |

Finalidades: `EVALUACION`, `INFORME_EMPRESA`. Para el informe, el cuarto parámetro es el correo de la empresa; si no, `""`.

## CU3 · Empresas, cargos e instrumentos

| Comando | Parámetros | Rol | Ejemplo |
|---|---|---|---|
| `INSEMP` | nombre, correoInformes | Administrador | `INSEMP["Acme SRL","rrhh@acme.test"]` |
| `ACTEMP` | empresaId, nombre, correoInformes | Administrador | `ACTEMP["1","Acme SRL","talento@acme.test"]` |
| `LISEMP` | — | Personal | `LISEMP[]` |
| `INSCAR` | empresaId, nombre, carrera, mesesMínimos, competencias, funciones | Personal | `INSCAR["1","Analista","Administracion","12","excel;atencion","Registrar y analizar datos"]` |
| `ACTCAR` | cargoId, nombre, carrera, mesesMínimos, competencias, funciones | Personal | `ACTCAR["1","Analista senior","Administracion","24","excel;atencion","Registrar, analizar y presentar datos"]` |
| `LISCAR` | — | Personal | `LISCAR[]` |
| `INSINS` | código, versión, nombre, categoría, consigna | Personal | `INSINS["TEC-01","1","Prueba tecnica demo","Aptitud","Explica por que elegirias la empresa."]` |
| `LISINS` | — | Personal | `LISINS[]` |

Un cargo con candidatos vinculados no se puede editar con `ACTCAR`.

## CU4 · Clasificación, planes y trazabilidad

| Comando | Parámetros | Rol | Ejemplo |
|---|---|---|---|
| `CLASEXP` | expId, clasificación, motivo | Personal | `CLASEXP["1","APTO","Revision documental conforme"]` |
| `ASIGPLAN` | expId, ids de instrumentos | Personal | `ASIGPLAN["1","1,2"]` |
| `LISPLAN` | expId | Todos | `LISPLAN["1"]` |
| `HISEXP` | expId | Personal | `HISEXP["1"]` |

Clasificaciones: `APTO`, `OBSERVADO`, `NO_APTO`. Para asignar un plan, el expediente debe estar `APTO`, tener perfil y consentimiento vigente.

## CU5 · Pruebas y calificación

| Comando | Parámetros | Rol | Ejemplo |
|---|---|---|---|
| `VERPRU` | evaluaciónId | Todos | `VERPRU["1"]` |
| `RESPRU` | evaluaciónId, respuesta | Candidato | `RESPRU["1","Mi respuesta de ejemplo"]` |
| `CALPRU` | evaluaciónId, puntaje, observación | Personal | `CALPRU["1","80","Respuesta suficiente"]` |
| `CONSOL` | expId | Personal | `CONSOL["1"]` |

El puntaje va de 0 a 100 y lo registra una persona. La respuesta no se puede sobrescribir.

## CU6 · Búsqueda, ranking y matriz

| Comando | Parámetros | Rol | Ejemplo |
|---|---|---|---|
| `BUSPER` | cargoId, puntajeMínimo | Personal | `BUSPER["1","60"]` |
| `RANKING` | cargoId | Personal | `RANKING["1"]` |
| `MATRIZ` | cargoId | Personal | `MATRIZ["1"]` |

Regla `DEMO_V1`, solo para la demostración: 25 puntos por carrera exacta, 25 por experiencia proporcional, 25 por competencias cumplidas y 25 por consolidado/100. Se listan hasta 50 expedientes, ordenados por puntaje y luego por ID.

## CU7 · Informes y despacho

| Comando | Parámetros | Rol | Ejemplo |
|---|---|---|---|
| `INFORME` | expId, recomendación | Personal | `INFORME["1","Cumple el perfil del cargo"]` |
| `LISINF` | expId | Todos | `LISINF["1"]` |
| `INFCON` | informeId | Todos | `INFCON["1"]` |
| `DESINF` | informeId | Personal | `DESINF["1"]` |

`DESINF` exige consentimiento de evaluación vigente y autorización del candidato para el contacto actual de la empresa.

## CU8 · Reportes

| Comando | Parámetros | Rol | Ejemplo |
|---|---|---|---|
| `ESTAD` | — | Personal | `ESTAD[]` |
| `REPCAR` | cargoId | Personal | `REPCAR["1"]` |

## Acceso y ayuda

| Comando | Parámetros | Rol | Ejemplo |
|---|---|---|---|
| `ACCESO` | — | Cualquier cuenta activa | `ACCESO[]` |
| `HELP` | — | Cualquiera | `HELP[]` |

## Errores

| Código | Significa |
|---|---|
| `SINTAXIS` | Comillas, corchetes o texto sobrante |
| `CANTIDAD` | Parámetros de más o de menos |
| `PARAMETRO` | Formato inválido de ID, correo, puntaje u opción |
| `IDENTIDAD` | Falta el token, venció o no corresponde |
| `PERMISO` | Rol sin permiso |
| `CONSENTIMIENTO` | Falta consentimiento vigente |
| `ESTADO` | Operación fuera de secuencia |
| `DUPLICADO` | Registro repetido |
| `NO_ENCONTRADO` | ID inexistente o ajeno |
| `TAMANO` | Correo mayor a 64 KiB |
| `COMANDO` | Comando desconocido |
