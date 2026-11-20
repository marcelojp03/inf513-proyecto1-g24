# Guion de demostración · 15 minutos

Los datos ya están cargados con `sql/02_datos_demo.sql` y `sql/03_datos_prueba_pedro.sql`; en vivo solo se muestran operaciones por correo.

## Estado inicial de la base

| Dato | Valor |
|---|---|
| Usuarios | 7: Marcelo (admin), Letizia (reclutador), Luis (candidato), Pedro (prueba), un reclutador institucional y dos ficticios |
| Empresa | FICCT, contacto `marcelojp03@gmail.com` |
| Cargo | Analista de datos, Administración, 12 meses |
| Instrumentos | `TEC-01` y `PER-01`, ambos versión 1 |
| Expedientes B | Luis 79, Ana Demo 86, Carlos Demo 61 y Pedro sin consolidar |
| Planes y pruebas | 4 planes; 6 pruebas calificadas y 2 pendientes (las de Pedro) |
| Consentimientos | Evaluación en cuatro; informe empresarial en Luis y Ana |
| Trazabilidad | 14 eventos con actor y motivo |
| Informes | Ninguno: el informe se emite en vivo |

## Reparto

| Persona | Rol | Correo |
|---|---|---|
| Marcelo | `ADMINISTRADOR` | `marcelojp03@gmail.com` |
| Letizia | `RECLUTADOR` | `mendietaletizia@gmail.com` |
| Luis | `CANDIDATO` | `guzmanluisdavid12@gmail.com` |
| Pedro | `CANDIDATO` (cuenta de prueba) | `elproo16399@gmail.com` |
| Marcelo (institucional) | `RECLUTADOR` | `mjimenezp126@ficct.uagrm.edu.bo` |

## Antes de la defensa

1. Poller corriendo: `.\ejecutar.ps1`.
2. `.\ejecutar.ps1 --verificar-correo` debe responder `POP3 OK ... SMTP OK`.
3. Cada integrante pide su `ACCESO[]` al comenzar su bloque; el código dura 30 minutos.
4. Ensayar una vez y guardar capturas de las respuestas.

## Guion minuto a minuto

### 0:00–1:30 · Apertura

- Qué resuelve y cómo se opera: asunto `COMANDO["..."]`, cuerpo con `TOKEN:`.
- Arquitectura: POP3 del buzón → analizador → PostgreSQL → SMTP.
- Mostrar el repo y `HELP[]`.

### 1:30–3:00 · CU1 · Usuarios (Marcelo)

| Paso | Correo | Respuesta |
|---|---|---|
| 1 | `ACCESO[]` | Código |
| 2 | `INSUSU["candidato.demo@example.test","CANDIDATO"]` | Usuario registrado |
| 3 | `LISUSU[]` | 6–7 usuarios |

### 3:00–4:30 · CU3 · Empresas, cargos e instrumentos

| Paso | Quién | Correo |
|---|---|---|
| 1 | Marcelo | `LISEMP[]` |
| 2 | Letizia | `ACCESO[]` y `LISCAR[]` |
| 3 | Letizia | `LISINS[]` |

Opcional si sobra tiempo: `INSEMP["Nova SRL","novedades@example.test"]`.

### 4:30–6:30 · CU2 · Candidato, privacidad y consentimiento (Luis)

| Paso | Correo |
|---|---|
| 1 | `ACCESO[]` |
| 2 | `INSEXP["Luis Guzman","Administracion","18","excel;atencion","SI"]` |
| 3 | `CONSEN["<nuevoExpId>","EVALUACION","SI",""]` |
| 4 | `MISEXP[]` y `CONSEXP["<nuevoExpId>"]` |

Qué destacar: el expediente A nace **interno**, no se publica; el expediente B ya cargado conserva su propio consentimiento. `CONSEXP` no muestra contactos de terceros.

### 6:30–8:00 · CU4 · Clasificación, plan y trazabilidad (Letizia)

| Paso | Correo |
|---|---|
| 1 | `LISPLAN["1"]` |
| 2 | `HISEXP["1"]` |

Qué destacar: cada evento tiene actor, operación y motivo; el plan conserva los instrumentos aplicados.

### 8:00–10:30 · CU5 · Resultados en vivo (Pedro y Letizia)

Pedro tiene el expediente B `4` con las pruebas `7` y `8` sin responder.

| Quién | Correo |
|---|---|
| Pedro | `VERPRU["7"]` |
| Pedro | `RESPRU["7","Automatizaria el reporte y mediria el impacto."]` |
| Pedro | `RESPRU["8","Priorizo por impacto y plazo."]` |
| Letizia | `CALPRU["7","85","Respuesta aplicada"]` |
| Letizia | `CALPRU["8","78","Criterio claro"]` |
| Letizia | `CONSOL["4"]` |

Qué destacar: la nota la registra una persona del personal, no el candidato; el consolidado aparece recién cuando todas las pruebas están calificadas.

### 10:30–12:00 · CU6 · Búsqueda y ranking (Marcelo)

| Paso | Correo |
|---|---|
| 1 | `RANKING["1"]` |
| 2 | `MATRIZ["1"]` |
| 3 | `BUSPER["1","70"]` |

Qué destacar: ahora aparecen cuatro candidatos; solo cuentan los `APTO` con consentimiento y consolidado; la salida usa IDs y `MATRIZ` desglosa los 25 puntos por factor.

### 12:00–13:30 · CU7 · Informe y despacho (Letizia)

| Paso | Correo |
|---|---|
| 1 | `INFORME["1","Cumple el perfil del cargo"]` |
| 2 | `DESINF["<informeId>"]` |
| 3 | `INFCON["<informeId>"]` |

Qué destacar: el despacho exige consentimiento del candidato y contacto autorizado; el estado del envío queda registrado. Llega copia a la bandeja de la empresa.

Alternativa con Pedro, si sobra tiempo: `CONSEN["4","INFORME_EMPRESA","SI","marcelojp03@gmail.com"]` y `INFORME["4","..."]`.

### 13:30–14:30 · CU8 · Reportes (Marcelo)

| Paso | Correo |
|---|---|
| 1 | `ESTAD[]` |
| 2 | `REPCAR["1"]` |

### 14:30–15:00 · Cierre

- Repo público, README, guía, lista de comandos y flujo para copiar.
- Límites: ranking `DEMO_V1`, sin adjuntos/OCR/IA; relay de la cátedra habilitado.
- Preguntas.

## Plan B

| Síntoma | Qué hacer |
|---|---|
| No llega la respuesta en 30 s | Revisar spam; mostrar `logs/access.log` y decir el estado real |
| `[IDENTIDAD]` | El código venció o se pidió otro: `ACCESO[]` de nuevo |
| `[ESTADO]` | El paso anterior no se completó; mostrarlo y seguir desde ahí |
| SMTP falla | `.\ejecutar.ps1 --verificar-correo`; si el relay no está habilitado, volver a Gmail en `.env` |
| POP3 no responde | Mostrar `logs/error.log` y seguir con capturas ya tomadas |

## Reglas de la demo

- Un solo poller. Dos instancias producen respuestas duplicadas.
- Los datos ya están cargados; no se carga nada por SQL durante la demo.
- El contacto de la empresa es un buzón real para que el informe no rebote.
- El puntaje lo registra una persona; el sistema no inventa notas.
- El administrador no puede ejecutar los comandos de candidato: `INSEXP`, `MISEXP`, `ACTEXP`, `CONSEN` y `RESPRU`.
- No se muestra `.env` ni se dictan códigos en voz alta.
