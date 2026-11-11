# Reto 10: Consumiendo Webservices

Proyecto educativo Android en Java y XML, desarrollado para estudiar el consumo de datos abiertos colombianos. Contexto académico indicado por el solicitante: maestría de la Universidad Nacional de Colombia. No es una aplicación oficial de la Universidad ni de INVIMA.

## Relación con el enunciado

| Requisito | Implementación |
| --- | --- |
| Consumir un servicio del catálogo colombiano | SODA3 mediante POST con token; SODA2 público mediante GET sin token, conjunto `kwki-6kd6` |
| Enviar parámetros desde la interfaz | Nombre parcial, departamento y ciudad; cláusulas SoQL enviadas al servidor |
| Presentar la respuesta | Listado y detalle de cada banco |
| Procesar con objetos JSON del SDK | `JSONObject` para solicitud/registros y `JSONArray` para respuesta |

El ejemplo de puntos posconsumo del PDF no es obligatorio. Se utiliza el conjunto de bancos de sangre elegido por el solicitante.

## Herramientas

- Android Studio, SDK 36 y JDK compatible con Android Gradle Plugin 9.1.0.
- Gradle 9.5.0 mediante el wrapper del proyecto.
- Android 8.0 (API 26) o superior.
- Sin bibliotecas externas para HTTP ni JSON.

## Configuración local

1. Abrir la carpeta del proyecto en Android Studio.
2. Copiar `local.properties.example` a `local.properties`.
3. Ajustar `sdk.dir`. Dejar `socrata.app.token` vacío para SODA2 público, o completar un token válido para SODA3.
4. Sincronizar Gradle y ejecutar `app` en un emulador o dispositivo.

`local.properties` está excluido de Git. El token se incorpora al APK a través de BuildConfig: un APK puede inspeccionarse, así que debe usarse un token de aplicación apropiado para clientes públicos, nunca una contraseña ni un secreto de cuenta. Cambiar el token requiere recompilar. No registrar el token en capturas ni documentación.

Documentación para registrar tokens: https://dev.socrata.com/docs/app-tokens.html

## Flujo de la consulta

`MainActivity` recoge filtros y construye SoQL mediante `BloodBankService.query`. Un `ExecutorService` ejecuta la petición fuera del hilo de interfaz. Con token, el servicio serializa un `JSONObject` y envía POST al endpoint SODA3 solicitado. Sin token, envía GET con `$query` codificado a SODA2 del mismo conjunto. Ambos caminos procesan el arreglo con `JSONArray` y cada registro con `JSONObject`. La actividad muestra el resultado en el hilo principal. Cada registro abre un diálogo desplazable con el detalle.

La alternativa SODA2 fue autorizada por el solicitante después de comprobar que SODA3 responde `403 authentication_required` sin token. La interfaz identifica la versión utilizada. No se cambia a SODA2 ante un token inválido: se informa el rechazo para que el error de configuración sea visible. La app funciona con datos reales sin registrar una cuenta.

Nombre admite coincidencia parcial. Departamento y ciudad usan igualdad sin distinguir mayúsculas; los acentos deben coincidir con la fuente. Los apóstrofes se escapan como literales SoQL. Los campos y el orden de consulta son fijos, no se acepta SQL arbitrario desde la interfaz.

Ejemplo del cuerpo de consulta SODA3 (el token se envía en la cabecera `X-App-Token`):

```json
{
  "query": "SELECT * WHERE upper(departamento) = upper('ANTIOQUIA') ORDER BY bancodesangre, numero_de_consecutivo",
  "page": {"pageNumber": 1, "pageSize": 1000},
  "includeSynthetic": false
}
```

Sin token se utiliza `https://www.datos.gov.co/resource/kwki-6kd6.json?$query=...`, codificando la misma consulta con `URLEncoder` y agregando `LIMIT 1000`.

La consulta limita la página a 1000 filas; la interfaz lo informa si se alcanza ese tamaño. No se implementa caché ni se presentan datos simulados como reales. Se informa ausencia de resultados, errores de red, tiempos de espera y rechazo HTTP.

## Verificación académica

- Consultar sin filtros y abrir un detalle.
- Filtrar por departamento y ciudad con valores existentes y contrastar con el catálogo.
- Buscar una cadena inexistente y comprobar el estado sin resultados.
- Desactivar Internet y verificar el mensaje de error y la posibilidad de reintentar.
- Compilar sin token y verificar el indicador de SODA2 público y la consulta real.
- Usar un token inválido y comprobar el rechazo del servicio.
- Girar el dispositivo durante una consulta y comprobar que no se produce un cierre inesperado. Una recreación de la actividad permite repetir la consulta; no se conserva la respuesta anterior.

Estas son instrucciones de verificación, no evidencia de que las pruebas ya se hayan ejecutado. El resultado real se registra en `VALIDACION.md`.

### Pruebas automatizadas en un emulador

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w co.edu.unal.reto10.test/co.edu.unal.reto10.EducationalTestRunner
```

El runner usa `Instrumentation` del SDK y comprueba filtros, escape de apóstrofes, inicio de la actividad y consultas reales con/sin resultados. Requiere Internet y acceso al servicio. El modo SODA3 requiere configurar el token antes de compilar.

## Fuentes

- Catálogo: https://www.datos.gov.co/Salud-y-Protecci-n-Social/BANCOS-DE-SANGRE-EN-FUNCIONAMIENTO-EN-EL-PA-S/kwki-6kd6/about_data
- Endpoint solicitado: https://www.datos.gov.co/api/v3/views/kwki-6kd6/query.json
- Metadatos de campos: https://www.datos.gov.co/api/views/kwki-6kd6.json
- Documentación del conjunto: https://dev.socrata.com/foundry/www.datos.gov.co/kwki-6kd6
- SODA3: https://dev.socrata.com/docs/queries/
- Endpoint público SODA2 autorizado: https://www.datos.gov.co/resource/kwki-6kd6.json

Los datos pertenecen a la fuente pública. La aplicación no verifica disponibilidad de unidades de sangre ni reemplaza la consulta directa con los establecimientos.
