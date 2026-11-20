# Guion de demostración · 15 minutos

Los datos ya están cargados con `sql/02_datos_demo.sql`; en vivo solo se muestran operaciones por correo.

## Estado inicial de la base

| Dato | Valor |
|---|---|
| Usuarios | 5: Marcelo (admin), Letizia (reclutador), Luis (candidato) y dos candidatos ficticios |
| Empresa | FICCT, contacto `marcelojp03@gmail.com` |
| Cargo | Analista de datos, Administración, 12 meses |
| Instrumentos | `TEC-01` y `PER-01`, ambos versión 1 |
| Expedientes B | Luis 79, Ana Demo 86, Carlos Demo 61 |
| Planes y pruebas | 3 planes, 6 pruebas respondidas y calificadas |
| Consentimientos | Evaluación en los tres; informe empresarial en Luis y Ana |
| Trazabilidad | 11 eventos con actor y motivo |
| Informes | Ninguno: el informe se emite en vivo |

## Reparto

| Persona | Rol | Correo |
|---|---|---|
| Marcelo | `ADMINISTRADOR` | `marcelojp03@gmail.com` |
| Letizia | `RECLUTADOR` | `mendietaletizia@gmail.com` |
| Luis | `CANDIDATO` | `guzmanluisdavid12@gmail.com` |

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
| 3 | `LISUSU[]` | 5–6 usuarios |

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

Qué destacar: el expediente A nace **interno**, no se publica; el expediente B ya existente conserva su propio consentimiento. `CONSEXP` no muestra contactos de terceros.

### 6:30–8:00 · CU4 · Clasificación, plan y trazabilidad (Letizia)

| Paso | Correo |
|---|---|
| 1 | `LISPLAN["1"]` |
| 2 | `HISEXP["1"]` |

Qué destacar: cada evento tiene actor, operación y motivo; el plan conserva los instrumentos aplicados.

### 8:00–9:30 · CU5 · Resultados (Letizia)

| Paso | Correo |
|---|---|
| 1 | `VERPRU["1"]` |
| 2 | `VERPRU["2"]` |

Qué destacar: respuesta, calificación y observación humana. Los tres expedientes ya están consolidados (`DEMO_V1`).

### 9:30–11:30 · CU6 · Búsqueda y ranking (Marcelo)

| Paso | Correo |
|---|---|
| 1 | `RANKING["1"]` |
| 2 | `MATRIZ["1"]` |
| 3 | `BUSPER["1","70"]` |

Qué destacar: solo aparecen candidatos `APTO` con consentimiento y consolidado; la salida usa IDs, no nombres; `MATRIZ` desglosa los 25 puntos por factor.

### 11:30–13:30 · CU7 · Informe y despacho (Letizia)

| Paso | Correo |
|---|---|
| 1 | `INFORME["1","Cumple el perfil del cargo"]` |
| 2 | `DESINF["<informeId>"]` |
| 3 | `INFCON["<informeId>"]` |

Qué destacar: el despacho exige consentimiento del candidato y contacto autorizado; el estado del envío queda registrado. Llega copia a la bandeja de la empresa.

### 13:30–14:30 · CU8 · Reportes (Marcelo)

| Paso | Correo |
|---|---|
| 1 | `ESTAD[]` |
| 2 | `REPCAR["1"]` |

### 14:30–15:00 · Cierre

- Repo público, README, guía y lista de comandos.
- Límites: relay de la cátedra pendiente (respuestas por Gmail), ranking `DEMO_V1`, sin adjuntos/OCR/IA.
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
- Los datos de empresa y candidatos ficticios ya están cargados; no se carga nada por SQL durante la demo.
- El contacto de la empresa es un buzón real para que el informe no rebote.
- El puntaje lo puso una persona; el sistema no inventa notas.
- No se muestra `.env` ni se dictan códigos en voz alta.

