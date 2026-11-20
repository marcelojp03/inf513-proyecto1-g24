# Guía personal · dos cuentas

Solo mis dos correos. El admin cubre también el rol de reclutador; el candidato es Pedro.

| Rol | Cuenta | Uso |
|---|---|---|
| Administrador (y reclutador) | `marcelojp03@gmail.com` | Todo el personal: usuarios, empresas, cargos, planes, calificación, informes, reportes |
| Candidato | `elproo16399@gmail.com` | Pedro Peña, expediente `4`, pruebas `7` y `8` pendientes |

No se usan las cuentas de Luis ni de Letizia. La cuenta `mjimenezp126@ficct.uagrm.edu.bo` quedó registrada como reclutadora, pero no es necesaria.

## Antes de empezar

```powershell
cd "C:\UAGRM\Tecnologia Web\proyecto-grupo18b\Primer parcial"
.\ejecutar.ps1 --verificar-correo
.\ejecutar.ps1
```

Debe decir `POP3 OK ... SMTP OK`. La segunda terminal queda abierta mostrando `[ENTRADA #]` y `[SALIDA #]`.

Cada comando es un **correo nuevo** a `grupo24sa@tecnoweb.org.bo`. El asunto es el comando; el cuerpo es `TOKEN: <código>`. El código dura 30 minutos.

## Secuencia completa

### 1. Admin pide código

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `ACCESO[]` | vacío | Manda el código del admin |

### 2. Admin consulta

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `LISUSU[]` | `TOKEN: <código admin>` | Lista usuarios con rol y estado |
| `LISCAR[]` | `TOKEN: <código admin>` | Lista el cargo y sus requisitos |
| `LISINS[]` | `TOKEN: <código admin>` | Lista los instrumentos disponibles |
| `RANKING["1"]` | `TOKEN: <código admin>` | Ordena candidatos del cargo |

### 3. Pedro pide código

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `ACCESO[]` | vacío | Manda el código de Pedro |

### 4. Pedro revisa y responde

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `MISEXP[]` | `TOKEN: <código Pedro>` | Lista sus expedientes |
| `CONSEXP["4"]` | `TOKEN: <código Pedro>` | Muestra perfil, estado, plan y consentimientos |
| `LISPLAN["4"]` | `TOKEN: <código Pedro>` | Muestra las pruebas 7 y 8 asignadas |
| `VERPRU["7"]` | `TOKEN: <código Pedro>` | Muestra la consigna de la prueba técnica |
| `RESPRU["7","Automatizaria el reporte y mediria el impacto."]` | `TOKEN: <código Pedro>` | Registra la respuesta de la prueba 7 |
| `RESPRU["8","Priorizo por impacto y plazo."]` | `TOKEN: <código Pedro>` | Registra la respuesta de la prueba 8 |

### 5. Admin califica y consolida

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `CALPRU["7","85","Respuesta aplicada"]` | `TOKEN: <código admin>` | Puntaje manual de la prueba 7 |
| `CALPRU["8","78","Criterio claro"]` | `TOKEN: <código admin>` | Puntaje manual de la prueba 8 |
| `CONSOL["4"]` | `TOKEN: <código admin>` | Promedia y consolida el resultado de Pedro |
| `RANKING["1"]` | `TOKEN: <código admin>` | Pedro ya aparece junto a los demás |
| `MATRIZ["1"]` | `TOKEN: <código admin>` | Desglose de los 25 puntos por factor |

### 6. Pedro autoriza el informe

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `CONSEN["4","INFORME_EMPRESA","SI","marcelojp03@gmail.com"]` | `TOKEN: <código Pedro>` | Autoriza el envío al contacto de la empresa |

### 7. Admin emite y despacha

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `INFORME["4","Cumple el perfil del cargo"]` | `TOKEN: <código admin>` | Emite el informe de Pedro |

Anotá el ID del informe (normalmente `1`).

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `DESINF["1"]` | `TOKEN: <código admin>` | Encola el envío del informe |
| `INFCON["1"]` | `TOKEN: <código admin>` | Muestra el informe y el estado del despacho |

### 8. Admin cierra con reportes

| Asunto | Cuerpo | Qué hace |
|---|---|---|
| `ESTAD[]` | `TOKEN: <código admin>` | Resumen general de expedientes y evaluaciones |
| `REPCAR["1"]` | `TOKEN: <código admin>` | Reporte del cargo 1 con promedios |
| `REPCAR["2"]` | `TOKEN: <código admin>` | Reporte del cargo 2 con promedios |

## Si algo falla

| Respuesta | Qué hacer |
|---|---|
| `[IDENTIDAD]` | El código venció o se pidió otro: `ACCESO[]` de nuevo |
| `[PERMISO]` | Ese comando es del otro rol: `INSEXP`, `MISEXP`, `ACTEXP`, `CONSEN` y `RESPRU` son solo del candidato |
| `[ESTADO]` | Ya está respondido, calificado o consolidado |
| `[CANTIDAD]` | Faltan o sobran parámetros |
| No llega nada | Revisar spam y la terminal; mirar `logs/access.log` |

## Reglas

- Un correo nuevo por comando.
- El código va solo en el cuerpo.
- No uses las cuentas de Luis ni de Letizia para esta guía.
- El admin no puede responder pruebas ni otorgar consentimientos: eso es de Pedro.
