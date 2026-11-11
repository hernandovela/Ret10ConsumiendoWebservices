# Validación realizada · 11 de noviembre de 2026

## Entorno

Android Studio, SDK de compilación 36, Android Gradle Plugin 9.1.0 y Gradle wrapper 9.5.0. Emulador `Small_Phone`, dispositivo ADB `emulator-5554`. Aplicación `co.edu.unal.reto10`, versión debug 1.0.

## Resultados comprobados

| Verificación | Resultado |
| --- | --- |
| `assembleDebug assembleDebugAndroidTest lintDebug --offline --no-daemon` | Correcto |
| Android Lint | `No issues found.` |
| Instalación de APK y APK de pruebas | `Success` en ambos |
| Runner de instrumentación en Small_Phone | Correcto |
| Consulta real sin filtros mediante SODA2 | 85 registros |
| Consulta real con departamento y ciudad del primer registro | 4 registros, todos correspondientes a los filtros |
| Búsqueda inexistente con apóstrofe | 0 registros; consulta aceptada por el servidor |
| Inicio de actividad y controles de consulta/listado | Correcto |
| Inspección visual del listado | 85 resultados visibles |
| Inspección visual del detalle | Nombre, código, ubicación y demás campos visibles |
| SODA3 sin token | 403 `authentication_required`, comprobado por petición HTTP |
| Sincronización del proyecto en Android Studio | Correcta; módulo app reconocido y Small Phone seleccionado |
| Run desde Android Studio (Shift+F10) | Instalación finalizada correctamente y MainActivity iniciada |

Salida del runner:

```text
OK: filtros, escape de literales, actividad y API real. Total=85; filtrados=4; inexistentes=0
```

## Evidencia visual

![Listado real en Small_Phone](docs/evidencias/01-listado.png)

![Detalle de ASUNCIÓN LTDA](docs/evidencias/02-detalle.png)

## Alcance y límites

La alternativa SODA2 fue autorizada por el solicitante. Se usa el mismo conjunto `kwki-6kd6`, sin datos simulados. SODA3 permanece implementado y se selecciona cuando hay un token local; su ejecución autenticada no ha sido validada porque no se dispone de un token de la cuenta del solicitante.

No se ejecutaron pruebas de pérdida de conectividad, rotación durante una petición ni pruebas en dispositivos físicos. La cantidad de registros puede cambiar con las actualizaciones de la fuente.

Las advertencias de Gradle sobre APIs obsoletas del complemento y las notas de Java sobre compatibilidad no son incidencias de Android Lint ni fallos de compilación.
