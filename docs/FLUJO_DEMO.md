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

Datos ya cargados en la base:

| Dato | ID |
|---|---|
| Empresa FICCT | 1 |
| Cargo Analista de datos | 1 |
| Instrumentos TEC-01 y PER-01 | 1 y 2 |
| Expediente B de Luis | 1 |
| Expedientes de Ana y Carlos Demo | 2 y 3 |
| Evaluaciones de Luis | 1 y 2 |
| Consolidado de Luis | 79 |

Si un ID no coincide, usá el que devuelva el comando anterior.

---

## Bloque 0 · Cualquiera, sin código

| Asunto | Cuerpo |
|---|---|
| `HELP[]` | vacío |

Muestra el catálogo y demuestra que el sistema lee el buzón.

---

## Bloque 1 · Administrador (Marcelo, `marcelojp03@gmail.com`)

### 1.1 Pedir código

| Asunto | Cuerpo |
|---|---|
| `ACCESO[]` | vacío |

Guardá el código que llega.

### 1.2 Consultas de administración

| Asunto | Cuerpo |
|---|---|
| `LISUSU[]` | `TOKEN: <código>` |
| `LISEMP[]` | `TOKEN: <código>` |
| `LISCAR[]` | `TOKEN: <código>` |
| `LISINS[]` | `TOKEN: <código>` |

### 1.3 Ranking y reportes

| Asunto | Cuerpo |
|---|---|
| `RANKING["1"]` | `TOKEN: <código>` |
| `MATRIZ["1"]` | `TOKEN: <código>` |
| `BUSPER["1","70"]` | `TOKEN: <código>` |
| `ESTAD[]` | `TOKEN: <código>` |
| `REPCAR["1"]` | `TOKEN: <código>` |

### 1.4 Altas en vivo (opcional)

| Asunto | Cuerpo | Qué muestra |
|---|---|---|
| `INSUSU["candidato.demo@example.test","CANDIDATO"]` | `TOKEN: <código>` | CU1 |
| `INSEMP["Nova SRL","novedades@example.test"]` | `TOKEN: <código>` | CU3 |

---

## Bloque 2 · Reclutadora (Letizia, `mendietaletizia@gmail.com`)

### 2.1 Pedir código

| Asunto | Cuerpo |
|---|---|
| `ACCESO[]` | vacío |

### 2.2 Trazabilidad y plan

| Asunto | Cuerpo |
|---|---|
| `HISEXP["1"]` | `TOKEN: <código>` |
| `LISPLAN["1"]` | `TOKEN: <código>` |
| `VERPRU["1"]` | `TOKEN: <código>` |

### 2.3 Informe y despacho

| Asunto | Cuerpo |
|---|---|
| `INFORME["1","Cumple el perfil del cargo"]` | `TOKEN: <código>` |

Anotá el ID del informe que devuelve (normalmente `1`).

| Asunto | Cuerpo |
|---|---|
| `DESINF["1"]` | `TOKEN: <código>` |
| `INFCON["1"]` | `TOKEN: <código>` |

### 2.4 Reportes del rol

| Asunto | Cuerpo |
|---|---|
| `ESTAD[]` | `TOKEN: <código>` |
| `REPCAR["1"]` | `TOKEN: <código>` |

---

## Bloque 3 · Candidato (Luis, `guzmanluisdavid12@gmail.com`)

### 3.1 Pedir código

| Asunto | Cuerpo |
|---|---|
| `ACCESO[]` | vacío |

### 3.2 Consultar su expediente

| Asunto | Cuerpo |
|---|---|
| `MISEXP[]` | `TOKEN: <código>` |
| `CONSEXP["1"]` | `TOKEN: <código>` |
| `VERPRU["1"]` | `TOKEN: <código>` |
| `LISINF["1"]` | `TOKEN: <código>` |

### 3.3 Alta universitaria en vivo (opcional)

| Asunto | Cuerpo |
|---|---|
| `INSEXP["Luis Guzman","Administracion","18","excel;atencion","SI"]` | `TOKEN: <código>` |

Anotá el ID nuevo (normalmente `4`) y autorizá la evaluación:

| Asunto | Cuerpo |
|---|---|
| `CONSEN["4","EVALUACION","SI",""]` | `TOKEN: <código>` |

---

## Bloque 4 · Flujo empresarial completo en vivo (opcional, 10 correos)

Hacerlo solo si sobra tiempo. Crea un cargo nuevo para no chocar con el expediente B ya cargado.

### 4.1 Administrador

| Asunto | Cuerpo |
|---|---|
| `INSCAR["1","Asistente administrativo","Administracion","6","excel;redaccion","Apoyar la gestion documental"]` | `TOKEN: <código admin>` |

Anotá el ID del cargo (normalmente `2`).

### 4.2 Reclutadora

| Asunto | Cuerpo |
|---|---|
| `VINCEXP["2","2"]` | `TOKEN: <código de Letizia>` |

Anotá el ID del expediente nuevo (normalmente `4`).

### 4.3 Candidato

| Asunto | Cuerpo |
|---|---|
| `ACTEXP["4","Luis Guzman","Administracion","18","excel;atencion","SI"]` | `TOKEN: <código de Luis>` |
| `CONSEN["4","EVALUACION","SI",""]` | `TOKEN: <código de Luis>` |

### 4.4 Reclutadora

| Asunto | Cuerpo |
|---|---|
| `CLASEXP["4","APTO","Revision conforme"]` | `TOKEN: <código de Letizia>` |
| `ASIGPLAN["4","1,2"]` | `TOKEN: <código de Letizia>` |
| `LISPLAN["4"]` | `TOKEN: <código de Letizia>` |

`LISPLAN` devuelve los IDs de evaluación (normalmente `7` y `8`).

### 4.5 Candidato

| Asunto | Cuerpo |
|---|---|
| `RESPRU["7","Automatizaria el reporte y mediria el impacto."]` | `TOKEN: <código de Luis>` |
| `RESPRU["8","Priorizo por impacto y plazo."]` | `TOKEN: <código de Luis>` |

### 4.6 Reclutadora

| Asunto | Cuerpo |
|---|---|
| `CALPRU["7","85","Respuesta aplicada"]` | `TOKEN: <código de Letizia>` |
| `CALPRU["8","78","Criterio claro"]` | `TOKEN: <código de Letizia>` |
| `CONSOL["4"]` | `TOKEN: <código de Letizia>` |

### 4.7 Administrador

| Asunto | Cuerpo |
|---|---|
| `RANKING["2"]` | `TOKEN: <código admin>` |
| `MATRIZ["2"]` | `TOKEN: <código admin>` |

---

## Permisos por rol

| Grupo | Comandos | Quién |
|---|---|---|
| Administración | `INSUSU`, `LISUSU`, `ACTUSU`, `INSEMP`, `ACTEMP` | Solo administrador |
| Personal | `VINCEXP`, `LISEMP`, `INSCAR`, `ACTCAR`, `LISCAR`, `INSINS`, `LISINS`, `CLASEXP`, `ASIGPLAN`, `HISEXP`, `CALPRU`, `CONSOL`, `BUSPER`, `RANKING`, `MATRIZ`, `INFORME`, `DESINF`, `ESTAD`, `REPCAR` | Administrador y reclutador |
| Solo candidato | `INSEXP`, `MISEXP`, `ACTEXP`, `CONSEN`, `RESPRU` | Candidato |
| Consulta abierta | `CONSEXP`, `LISPLAN`, `VERPRU`, `LISINF`, `INFCON`, `HELP`, `ACCESO` | Los tres roles |

El administrador **no** puede ejecutar los comandos de candidato: `INSEXP`, `MISEXP`, `ACTEXP`, `CONSEN` y `RESPRU` son del titular del expediente. El candidato solo ve y modifica sus propios registros; el personal requiere consentimiento de evaluación vigente.

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
