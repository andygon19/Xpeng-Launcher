# XPENG G3i Home

Proyecto base de un launcher para el sistema de infoentretenimiento del XPENG G3i con Android 9 (API 28). Se puede abrir como una app o elegir como Home si el firmware ofrece el selector de Android.

| Dise�o | Distribuci�n |
|---|---|
| Cabina | En la pantalla vertical de 15,6?, el veh�culo ocupa la parte superior, seguido por los botones de proyecci�n, los widgets y el dock. |
| Proyecci�n | CarPlay y Android Auto aparecen arriba con botones m�s altos. Debajo quedan una vista compacta del veh�culo, los widgets y el dock. |

Desde Ajustes se cambia el dise�o. Desde Apps se pueden abrir actividades instaladas y editar hasta cuatro accesos directos. La hora del sistema se muestra en ambos dise�os. Los indicadores de audio y micr�fono son espacios reservados y se marcan como pendientes de integraci�n.

## Compilar

Instala JDK 17, Gradle 9.8.2 y Android SDK Platform 35. Desde la ra�z:

```powershell
gradle :app:assembleDebug :app:lintDebug
```

GitHub Actions compila y publica la APK de prueba como artefacto de la ejecuci�n. La firma de depuraci�n de una ejecuci�n de CI puede diferir de la de otra. Para actualizar APK entre versiones sin desinstalar, configura una firma persistente antes de publicar Releases.

## Receptores

El Home abre DiPlay (`com.shihab.diplay`) y Headunit Revived (`com.andrerinas.headunitrevived`) si est�n instalados. Son aplicaciones separadas. Prueba el Home primero como una aplicaci�n normal y verifica las funciones OEM antes de seleccionarlo como Home.

## Integraci�n pendiente en el G3i

- Hotspot. Obtener el estado real del AP y la interfaz del firmware, conservar un AP activo, iniciar el AP si est� apagado y restaurar el Wi-Fi cliente ante fallo. Verificar la transici�n con el carro estacionado y una v�a de recuperaci�n de ADB, ya que la sesi�n ADB por Wi-Fi puede perderse durante el cambio.
- Micr�fono. Confirmar captura PCM en cada receptor. Validar voz y llamadas por separado en iPhone y Android. La opci�n de micr�fono en los receptores requerir� validar su API o modificar su propio c�digo.
- Incorporar las im�genes originales de XPENG y el G3i. Verificar accesos OEM y compatibilidad real del selector Home.

Esta versi�n base solamente abre la pantalla de red de Android desde Ajustes. Los receptores gestionan sus propias sesiones de proyecci�n.
