# Guion de demostración · 15–20 minutos

Objetivo: mostrar CU1–CU8 funcionando **por correo**, con los tres roles, sin cargar datos por SQL y sin depender de la suerte.

## Reparto

| Persona | Rol | Correo |
|---|---|---|
| Marcelo | `ADMINISTRADOR` | `marcelojp03@gmail.com` |
| Letizia | `RECLUTADOR` | `mendietaletizia@gmail.com` |
| Luis | `CANDIDATO` | `guzmanluisdavid12@gmail.com` |

## Antes de la defensa

1. Poller corriendo en la laptop de Marcelo: `.\ejecutar.ps1`.
2. `.\ejecutar.ps1 --verificar-correo` debe decir `POP3 OK ... SMTP OK`.
3. Los tres correos ya están registrados en la base (`LISUSU`).
4. **Precarga por correo** (la hace Marcelo con su token, no por SQL):
   - `INSEMP["Acme SRL","rrhh@acme.test"]`
   - `INSCAR["1","Analista de datos","Administracion","12","excel;atencion","Registrar y analizar datos"]`
   - `INSINS["TEC-01","1","Prueba tecnica demo","Aptitud","Explica por que elegirias la empresa."]`
   - `INSINS["PER-01","1","Cuestionario de atencion","Atencion","Describi como priorizas tareas."]`
5. Cada integrante pide su `ACCESO[]` **al comenzar su bloque**, no antes: el código dura 30 minutos.
6. Ensayar el guion dos veces y guardar capturas de cada respuesta.

## Guion minuto a minuto

### 0:00–1:30 · Apertura

- Qué resuelve: Banco de Talentos operado por correo.
- Arquitectura en una frase: POP3 lee el buzón, el analizador valida el asunto, PostgreSQL guarda, SMTP responde.
- Mostrar el repo y `HELP[]` (no requiere código).

### 1:30–3:00 · CU1 · Usuarios (Marcelo)

| Paso | Correo |
|---|---|
| 1 | Asunto `ACCESO[]`, cuerpo vacío → llega el código |
| 2 | `INSUSU["candidato.demo@example.test","CANDIDATO"]` con `TOKEN:` |
| 3 | `LISUSU[]` |

Respuesta esperada: «Usuario registrado. ID: 4. Rol: CANDIDATO.» y el listado.
Nota: esa cuenta ficticia no puede autenticarse; sirve para mostrar la gestión.

### 3:00–5:00 · CU3 · Empresas, cargos e instrumentos

| Paso | Quién | Correo |
|---|---|---|
| 1 | Marcelo | `INSEMP["Nova SRL","rrhh@nova.test"]` |
| 2 | Marcelo | `LISCAR[]` y `LISINS[]` (muestra lo precargado) |
| 3 | Letizia | `ACCESO[]` y luego `INSCAR["2","Asistente administrativo","Administracion","6","excel;redaccion","Apoyar la gestión documental"]` |

### 5:00–7:00 · CU2 · Candidato, privacidad y consentimiento (Luis)

| Paso | Correo |
|---|---|
| 1 | `ACCESO[]` |
| 2 | `INSEXP["Luis Guzman","Administracion","12","excel;atencion","SI"]` |
| 3 | `CONSEN["1","EVALUACION","SI",""]` |
| 4 | `MISEXP[]` y `CONSEXP["1"]` |

Qué destacar: el expediente nace **interno**, no se publica; sin consentimiento no se evalúa; `CONSEXP` no muestra contacto de terceros.

### 7:00–9:00 · CU4 · Clasificación y plan (Letizia)

| Paso | Correo |
|---|---|
| 1 | `CLASEXP["1","APTO","Revisión documental conforme"]` |
| 2 | `ASIGPLAN["1","1,2"]` |
| 3 | `LISPLAN["1"]` → anotar el `evaluaciónId` |
| 4 | `HISEXP["1"]` |

Qué destacar: cada acción queda trazada con actor y motivo.

### 9:00–11:30 · CU5 · Pruebas y calificación

| Paso | Quién | Correo |
|---|---|---|
| 1 | Luis | `VERPRU["1"]` |
| 2 | Luis | `RESPRU["1","Mi respuesta de ejemplo"]` |
| 3 | Letizia | `CALPRU["1","80","Respuesta suficiente"]` |
| 4 | Letizia | `CONSOL["1"]` |

Si el plan tiene dos instrumentos, repetir `VERPRU`/`RESPRU`/`CALPRU` con el segundo antes de `CONSOL`.

### 11:30–13:30 · CU6 · Búsqueda y ranking

| Paso | Quién | Correo |
|---|---|---|
| 1 | Marcelo | `BUSPER["1","60"]` |
| 2 | Marcelo | `RANKING["1"]` |
| 3 | Marcelo | `MATRIZ["1"]` |

Qué destacar: la salida usa IDs, no nombres ni contactos; y `MATRIZ` muestra el desglose de los 25 puntos por factor.

### 13:30–16:00 · CU7 · Informe y despacho

| Paso | Quién | Correo |
|---|---|---|
| 1 | Letizia | `INFORME["1","Cumple el perfil del cargo"]` |
| 2 | Luis | `CONSEN["1","INFORME_EMPRESA","SI","rrhh@acme.test"]` |
| 3 | Letizia | `DESINF["1"]` |
| 4 | Letizia | `INFCON["1"]` |

Qué destacar: sin el consentimiento del candidato, `DESINF` responde `[CONSENTIMIENTO]`. Probar primero sin él si hay tiempo.

### 16:00–17:30 · CU8 · Reportes

| Paso | Quién | Correo |
|---|---|---|
| 1 | Marcelo | `ESTAD[]` |
| 2 | Marcelo | `REPCAR["1"]` |

### 17:30–19:00 · Cierre

- Repo público, README y guía.
- Límites declarados: relay de la cátedra pendiente (respuestas por Gmail), calificación y ranking `DEMO_V1`, sin adjuntos/OCR/IA.
- Preguntas.

## Plan B

| Síntoma | Qué hacer |
|---|---|
| No llega la respuesta en 30 s | Revisar spam; mirar `logs/access.log` (sin direcciones) y decir el estado real |
| `[IDENTIDAD]` | El código venció o se pidió otro; `ACCESO[]` de nuevo |
| `[ESTADO]` | El paso anterior no se completó; mostrarlo y continuar desde ahí |
| SMTP falla | `.\ejecutar.ps1 --verificar-correo`; si el relay de la cátedra no está habilitado, volver a Gmail en `.env` |
| POP3 no responde | Mostrar `logs/error.log` y continuar con capturas ya tomadas |

## Reglas de la demo

- Un solo poller. Si dos personas lo corren, el remitente recibe duplicados.
- Nada de datos reales de terceros: `Acme SRL`, `Nova SRL`, `candidato.demo@example.test` y `rrhh@*.test` son ficticios.
- El puntaje lo pone una persona; el sistema no inventa notas.
- No se muestra el contenido de `.env` ni se dictan códigos en voz alta.
