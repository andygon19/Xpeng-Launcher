# XPENG G3i Home

Proyecto base de un launcher para el sistema de infoentretenimiento del XPENG G3i con Android 9 (API 28). Se puede abrir como una app o elegir como Home si el firmware ofrece el selector de Android.

| Diseño | Distribución |
|---|---|
| Cabina | En la pantalla vertical de 15,6″, el vehículo ocupa la parte superior, seguido por los botones de proyección, los widgets y el dock. |
| Proyección | CarPlay y Android Auto aparecen arriba con botones más altos. Debajo quedan una vista compacta del vehículo, los widgets y el dock. |

Desde Ajustes se cambia el diseño. Desde Apps se pueden abrir actividades instaladas y editar hasta cuatro accesos directos. La hora del sistema se muestra en ambos diseños. Los indicadores de audio y micrófono son espacios reservados y se marcan como pendientes de integración.

## Compilar

En **Ajustes → Elegir foto del G3i** y **Ajustes → Elegir logo XPENG** puedes seleccionar imágenes desde el almacenamiento del equipo. La aplicación conserva el acceso a los archivos seleccionados.

Instala JDK 17, Gradle 9.8.2 y Android SDK Platform 35. Desde la raíz:

```powershell
gradle :app:assembleDebug :app:lintDebug
```

GitHub Actions compila y publica la APK de prueba como artefacto de la ejecución. La firma de depuración de una ejecución de CI puede diferir de la de otra. Para actualizar APK entre versiones sin desinstalar, configura una firma persistente antes de publicar Releases.

## Receptores

El Home abre DiPlay (`com.shihab.diplay`) y Headunit Revived (`com.andrerinas.headunitrevived`) si están instalados. Son aplicaciones separadas. Prueba el Home primero como una aplicación normal y verifica las funciones OEM antes de seleccionarlo como Home.

## Integración pendiente en el G3i

- Hotspot. Obtener el estado real del AP y la interfaz del firmware, conservar un AP activo, iniciar el AP si está apagado y restaurar el Wi-Fi cliente ante fallo. Verificar la transición con el carro estacionado y una vía de recuperación de ADB, ya que la sesión ADB por Wi-Fi puede perderse durante el cambio.
- Micrófono. Confirmar captura PCM en cada receptor. Validar voz y llamadas por separado en iPhone y Android. La opción de micrófono en los receptores requerirá validar su API o modificar su propio código.
- Incorporar las imágenes originales de XPENG y el G3i. Verificar accesos OEM y compatibilidad real del selector Home.

Esta versión base solamente abre la pantalla de red de Android desde Ajustes. Los receptores gestionan sus propias sesiones de proyección.
