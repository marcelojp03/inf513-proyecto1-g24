# Flujo completo para copiar y pegar

Un solo archivo con el recorrido de la demo. Cada comando es un **correo nuevo** a:

```text
Para: grupo24sa@tecnoweb.org.bo
```

El **asunto** es el comando. El **cuerpo** es `TOKEN: <código>` cuando corresponde.

## Antes de empezar

En la laptop que corre el sistema:

```powershell
cd "C:\UAGRM\Tecnologia Web\proyecto-grupo18b\Primer parcial"
.\ejecutar.ps1 --verificar-correo
.\ejecutar.ps1
```

`--verificar-correo` debe decir `POP3 OK ... SMTP OK`. Dejá la segunda terminal abierta: ahí se ve `[ENTRADA #]` y `[SALIDA #]` de cada correo. **Un solo equipo** corre el poller.

Datos ya cargados:

| Dato | ID |
|---|---|
| Empresa FICCT | 1 |
| Cargo Analista de datos | 1 |
| Cargo Asistente administrativo | 2 |
| Instrumentos TEC-01 y PER-01 | 1 y 2 |
| Expediente B de Luis | 1 |
| Expedientes de Ana y Carlos Demo | 2 y 3 |
| Expedientes A de Ana y Carlos | 5 y 6 |
| Expedientes B del cargo 2 | 7 (Ana), 8 (Carlos, OBSERVADO) y 9 (Luis, NO_APTO) |
| Evaluaciones de Luis | 1 y 2 |
| Consolidado de Luis | 79 |
| Pedro, candidato de prueba | usuario 6, expediente 4, evaluaciones 7 y 8 sin responder |

Si un ID no coincide, usá el que devuelva el comando anterior.

---

## Bloque 0 · Cualquiera, sin código

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `HELP[]` | vacío | Devuelve el catálogo y demuestra que el sistema lee el buzón |

---

## Bloque 1 · Administrador (Marcelo, `marcelojp03@gmail.com`)

### 1.1 Pedir código

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `ACCESO[]` | vacío | Envía un código de sesión de 30 minutos a tu correo |

### 1.2 Consultas de administración

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `LISUSU[]` | `TOKEN: <código>` | Lista usuarios con rol y estado |
| `LISEMP[]` | `TOKEN: <código>` | Lista empresas y su contacto de informes |
| `LISCAR[]` | `TOKEN: <código>` | Lista perfiles de cargo con requisitos |
| `LISINS[]` | `TOKEN: <código>` | Lista instrumentos con código y versión |

### 1.3 Ranking y reportes

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `RANKING["1"]` | `TOKEN: <código>` | Ordena candidatos del cargo por compatibilidad `DEMO_V1` |
| `RANKING["2"]` | `TOKEN: <código>` | Ordena candidatos del segundo cargo |
| `MATRIZ["1"]` | `TOKEN: <código>` | Igual que ranking, con el desglose de los cuatro factores |
| `BUSPER["1","70"]` | `TOKEN: <código>` | Filtra candidatos con puntaje mayor o igual a 70 |
| `ESTAD[]` | `TOKEN: <código>` | Cuenta expedientes A y B, consentimientos, planes y consolidados |
| `REPCAR["1"]` | `TOKEN: <código>` | Métricas y promedio consolidado del cargo 1 |
| `REPCAR["2"]` | `TOKEN: <código>` | Métricas y promedio consolidado del cargo 2 |

### 1.4 Altas en vivo (opcional)

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `INSUSU["candidato.demo@example.test","CANDIDATO"]` | `TOKEN: <código>` | Registra una cuenta nueva con rol |
| `INSEMP["Nova SRL","novedades@example.test"]` | `TOKEN: <código>` | Registra una empresa y su correo de informes |

---

## Bloque 2 · Reclutador (Letizia o la cuenta FICCT)

Cualquiera de las dos cuentas sirve para este bloque: `mendietaletizia@gmail.com` o `mjimenezp126@ficct.uagrm.edu.bo`. Cada una pide su propio `ACCESO[]` y usa su propio código.

### 2.1 Pedir código

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `ACCESO[]` | vacío | Envía el código de sesión del reclutador |

### 2.2 Trazabilidad y plan

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `HISEXP["1"]` | `TOKEN: <código>` | Muestra el historial de acciones con actor y motivo |
| `LISPLAN["1"]` | `TOKEN: <código>` | Muestra el plan, sus pruebas y el estado de cada una |
| `VERPRU["1"]` | `TOKEN: <código>` | Muestra consigna, respuesta y puntaje de una prueba |

### 2.3 Informe y despacho

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `INFORME["1","Cumple el perfil del cargo"]` | `TOKEN: <código>` | Emite el informe individual con resultados y recomendación |

Anotá el ID del informe que devuelve (normalmente `1`).

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `DESINF["1"]` | `TOKEN: <código>` | Encola el envío del informe al contacto autorizado |
| `INFCON["1"]` | `TOKEN: <código>` | Muestra el informe y el estado de su despacho |

### 2.4 Reportes del rol

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `ESTAD[]` | `TOKEN: <código>` | Resumen general de expedientes y evaluaciones |
| `REPCAR["1"]` | `TOKEN: <código>` | Reporte del cargo con promedios |

---

## Bloque 3 · Candidato (Luis, `guzmanluisdavid12@gmail.com`)

### 3.1 Pedir código

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `ACCESO[]` | vacío | Envía el código de sesión de Luis |

### 3.2 Consultar su expediente

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `MISEXP[]` | `TOKEN: <código>` | Lista solo los expedientes del titular |
| `CONSEXP["1"]` | `TOKEN: <código>` | Muestra perfil, estado, plan, puntaje y consentimientos |
| `VERPRU["1"]` | `TOKEN: <código>` | Muestra la consigna y su respuesta registrada |
| `LISINF["1"]` | `TOKEN: <código>` | Lista los informes del expediente y su despacho |

### 3.3 Alta universitaria en vivo (opcional)

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `INSEXP["Luis Guzman","Administracion","18","excel;atencion","SI"]` | `TOKEN: <código>` | Crea un expediente A interno, sin publicarlo |

Anotá el ID nuevo (normalmente `4`) y autorizá la evaluación:

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `CONSEN["4","EVALUACION","SI",""]` | `TOKEN: <código>` | Otorga consentimiento de evaluación para ese expediente |

---

## Bloque 4 · Flujo B completo en vivo (opcional, 10 correos)

Hacerlo solo si sobra tiempo. Crea un cargo nuevo para no chocar con el expediente B ya cargado.

### 4.1 Administrador

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `INSCAR["1","Asistente administrativo","Administracion","6","excel;redaccion","Apoyar la gestion documental"]` | `TOKEN: <código admin>` | Crea un perfil de cargo nuevo |

Anotá el ID del cargo (normalmente `2`).

### 4.2 Reclutadora

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `VINCEXP["2","2"]` | `TOKEN: <código de Letizia>` | Crea el expediente B del candidato para ese cargo |

Anotá el ID del expediente nuevo (normalmente `4`).

### 4.3 Candidato

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `ACTEXP["4","Luis Guzman","Administracion","18","excel;atencion","SI"]` | `TOKEN: <código de Luis>` | Completa el perfil y lo devuelve a revisión |
| `CONSEN["4","EVALUACION","SI",""]` | `TOKEN: <código de Luis>` | Autoriza la evaluación de ese expediente |

### 4.4 Reclutadora

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `CLASEXP["4","APTO","Revision conforme"]` | `TOKEN: <código de Letizia>` | Clasifica el expediente por revisión humana |
| `ASIGPLAN["4","1,2"]` | `TOKEN: <código de Letizia>` | Asigna las pruebas 1 y 2 al expediente |
| `LISPLAN["4"]` | `TOKEN: <código de Letizia>` | Devuelve los IDs de evaluación asignados |

`LISPLAN` devuelve los IDs de evaluación (normalmente `7` y `8`).

### 4.5 Candidato

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `RESPRU["7","Automatizaria el reporte y mediria el impacto."]` | `TOKEN: <código de Luis>` | Registra la respuesta de la primera prueba |
| `RESPRU["8","Priorizo por impacto y plazo."]` | `TOKEN: <código de Luis>` | Registra la respuesta de la segunda prueba |

### 4.6 Reclutadora

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `CALPRU["7","85","Respuesta aplicada"]` | `TOKEN: <código de Letizia>` | Registra el puntaje manual de la primera prueba |
| `CALPRU["8","78","Criterio claro"]` | `TOKEN: <código de Letizia>` | Registra el puntaje manual de la segunda prueba |
| `CONSOL["4"]` | `TOKEN: <código de Letizia>` | Promedia las pruebas calificadas y consolida |

### 4.7 Administrador

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `RANKING["2"]` | `TOKEN: <código admin>` | Ordena los candidatos del cargo nuevo |
| `MATRIZ["2"]` | `TOKEN: <código admin>` | Muestra el desglose de puntajes del cargo nuevo |

---

## Bloque 5 · Pruebas con Pedro (candidato de prueba)

Cuenta: `elproo16399@gmail.com` (usuario 6). Expediente B `4`, plan `4`, evaluaciones `7` y `8` sin responder, consentimiento de evaluación vigente y **sin consolidado**.

### 5.1 Pedro

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `ACCESO[]` | vacío | Envía el código de sesión de Pedro |
| `MISEXP[]` | `TOKEN: <código de Pedro>` | Lista sus expedientes |
| `CONSEXP["4"]` | `TOKEN: <código de Pedro>` | Muestra perfil, estado y consentimientos |
| `LISPLAN["4"]` | `TOKEN: <código de Pedro>` | Muestra las pruebas asignadas y su estado |
| `VERPRU["7"]` | `TOKEN: <código de Pedro>` | Muestra la consigna de la prueba técnica |
| `RESPRU["7","Automatizaria el reporte y mediria el impacto."]` | `TOKEN: <código de Pedro>` | Registra la respuesta de la prueba 7 |
| `RESPRU["8","Priorizo por impacto y plazo."]` | `TOKEN: <código de Pedro>` | Registra la respuesta de la prueba 8 |

### 5.2 Reclutadora

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `CALPRU["7","85","Respuesta aplicada"]` | `TOKEN: <código de Letizia>` | Califica la prueba 7 |
| `CALPRU["8","78","Criterio claro"]` | `TOKEN: <código de Letizia>` | Califica la prueba 8 |
| `CONSOL["4"]` | `TOKEN: <código de Letizia>` | Consolida el resultado de Pedro |
| `RANKING["1"]` | `TOKEN: <código de Letizia>` | Muestra a Pedro junto a los demás candidatos |

Después de `CONSOL`, Pedro aparece en el ranking del cargo 1 junto a Luis, Ana y Carlos.

### 5.3 Autorización y despacho

| Quién | Asunto | Cuerpo | Qué hace |
|---|---|---|---|
| Pedro | `CONSEN["4","INFORME_EMPRESA","SI","marcelojp03@gmail.com"]` | `TOKEN: <código de Pedro>` | Autoriza el informe para el contacto de la empresa |
| Letizia | `INFORME["4","Cumple el perfil del cargo"]` | `TOKEN: <código de Letizia>` | Emite el informe de Pedro |
| Letizia | `DESINF["<informeId>"]` | `TOKEN: <código de Letizia>` | Encola el despacho del informe |

El administrador no puede ejecutar los pasos de Pedro: `INSEXP`, `MISEXP`, `ACTEXP`, `CONSEN` y `RESPRU` son exclusivos del candidato.

## Permisos por rol

| Grupo | Comandos | Quién |
|---|---|---|
| Administración | `INSUSU`, `LISUSU`, `ACTUSU`, `INSEMP`, `ACTEMP` | Solo administrador |
| Personal | `VINCEXP`, `LISEMP`, `INSCAR`, `ACTCAR`, `LISCAR`, `INSINS`, `LISINS`, `CLASEXP`, `ASIGPLAN`, `HISEXP`, `CALPRU`, `CONSOL`, `BUSPER`, `RANKING`, `MATRIZ`, `INFORME`, `DESINF`, `ESTAD`, `REPCAR` | Administrador y reclutador |
| Solo candidato | `INSEXP`, `MISEXP`, `ACTEXP`, `CONSEN`, `RESPRU` | Candidato |
| Consulta abierta | `CONSEXP`, `LISPLAN`, `VERPRU`, `LISINF`, `INFCON`, `HELP`, `ACCESO` | Los tres roles |

El administrador **no** puede ejecutar los comandos de candidato. El candidato solo ve y modifica sus propios registros; el personal requiere consentimiento de evaluación vigente.

## Si algo falla

| Respuesta | Qué hacer |
|---|---|
| `[IDENTIDAD]` | El código venció (30 min) o se pidió otro: `ACCESO[]` de nuevo |
| `[CANTIDAD]` | Faltan o sobran parámetros: comparar con `COMANDOS.md` |
| `[ESTADO]` | El expediente ya está APTO, ya tiene plan o ya está consolidado |
| `[DUPLICADO]` | Ese usuario ya tiene ese expediente para ese cargo |
| `[NO_ENCONTRADO]` | El ID no existe o no pertenece al candidato |
| No llega nada | Revisar spam, mirar la terminal y `logs/access.log` |
| SMTP falla | `.\ejecutar.ps1 --probar-relay` y `--verificar-correo` |

## Reglas

- Un correo nuevo por comando; no responder el del código.
- El código va solo en el cuerpo y no se dicta en voz alta.
- Un solo poller.
- Los IDs se toman de las respuestas, no se inventan.
