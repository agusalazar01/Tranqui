# Tranqui

App para Android que ayuda a dejar de procrastinar y a cumplir objetivos de a poco, con métodos que tienen evidencia. Todo se guarda en el teléfono: sin cuentas, sin servidor y sin internet.

## Qué tiene la versión 0.3.0

- **Hoy:** tres botones (1, 5 y 15 minutos), objetivos diarios, el «sapo» con desglose de primer paso de 2 minutos y acceso a la constancia semanal.
- **Objetivos con paso mínimo:** elegís una plantilla (leer, caminar, inglés…) y tu nivel; Tranqui te propone cuántos minutos hacer por día y te sugiere subir o bajar según cómo te va. Incluye el plan «si pasa X, hago Y», recordatorio diario y placer asociado.
- **Modo actividad:** guarda solo la hora de inicio, así que casi no gasta batería. Notificación con cronómetro y aviso cuando volvés a agarrar el celular.
- **Pausa antes de abrir apps:** respiración, «No entrar» fijo, opciones de 1, 5 y 15 minutos y «Entrar igual» que rebota y desaparece a los 14 segundos (configurable).
- **Calma:** siete ambientes (Lluvia, Bosque, Océano, Viento, Cuenco, Fuego y Ruido blanco), con capas de audio filtradas, compresión, limitación, volumen y temporizador; visualización orgánica animada y cuatro prácticas guiadas. El audio de ambientes se genera por capas de ruido procesado; no son todavía grabaciones de campo.
- **Métodos:** 13 fichas con su nivel de evidencia y un botón para usarlas (WOOP, Kanban, Hansei, Ikigai, Pomodoro, Kaizen…).
- **Progreso:** días de la semana, camino a los 66 días por objetivo (no vuelve a cero), minutos del mes y veces que elegiste no entrar.

## Cómo instalarla

1. Entrá a la pestaña **Releases** del repositorio y descargá `Tranqui-vX.Y.Z.apk`.
2. Abrilo en el teléfono y aceptá instalar desde esta fuente.
3. En Tranqui, tocá el engranaje (Ajustes) → **Activar en Ajustes de Android** y prendé «Tranqui: pausa antes de abrir apps». Después elegí qué apps muestran la pausa.

Cada versión nueva se instala encima de la anterior sin perder datos, porque todas se firman con la misma clave.

## Cómo se compila

GitHub Actions compila el APK solo en cada cambio que se sube a `main` (queda en la pestaña **Actions**, como artefacto). Para publicar una versión:

1. Subí `versionCode` y `versionName` en `app/build.gradle.kts`.
2. Subí el cambio a `main`. Actions compila y, como la versión es nueva, crea la versión en **Releases** con el APK.

## Firma y secretos

La clave de firma (`.jks`) y su contraseña **no están en el repositorio**:

- En GitHub Actions llegan como secretos: `TRANQUI_KEYSTORE_BASE64`, `TRANQUI_KEYSTORE_PASSWORD` y `TRANQUI_KEY_ALIAS` (Settings → Secrets and variables → Actions).
- Para compilar en una compu, copiá `keystore.properties.example` como `keystore.properties` y completalo. Ese archivo y cualquier `.jks` están en `.gitignore`.
- Sin secretos, Actions igual compila para comprobar el código, pero no publica el APK.
- Guardá una copia de la clave fuera de GitHub: si se pierde, las versiones nuevas no se podrán instalar encima.

## Diseño

El diseño actual está descrito en `REVISION_DISENO.md` (paleta Lino, Salvia, Bosque, Arcilla y Arena; títulos con serifa; brumas y paisajes orgánicos). Los cambios nuevos siguen esa línea.

## Dónde está cada cosa

| Archivo | Qué contiene |
| --- | --- |
| `app/src/main/java/com/tranqui/app/Ui.kt` | Colores, tipografía y componentes (tarjetas, bruma, colinas) |
| `Hoy.kt` | Pantalla principal |
| `Goal.kt` | Crear y editar objetivos |
| `Actividad.kt` | Modo actividad y celebración |
| `Pausa.kt` | Servicio de accesibilidad, pantalla de pausa y botón que rebota |
| `Calma.kt` | Pestaña Calma y prácticas guiadas |
| `Metodos.kt` | Biblioteca de métodos, WOOP/Hansei/Ikigai y Kanban |
| `Progreso.kt` | Progreso |
| `Ajustes.kt` | Permiso de pausa, apps elegidas y ajustes del botón |
| `Content.kt` | Textos: plantillas, opciones rápidas, métodos y prácticas |
| `Data.kt` | Guardado en el teléfono y cálculos de progreso |
| `Timer.kt` | Servicio del cronómetro, notificaciones y recordatorios |
| `app/src/main/assets/soundscape/index.html` | Motor de los paisajes sonoros y la bruma |

## Grabaciones reales

La carpeta `app/src/main/assets/soundscape/sounds/` queda preparada para incorporar grabaciones de campo con licencia compatible. La versión 0.3.0 utiliza capas de ruido procesado y síntesis ambiental para los siete paisajes; el cuenco usa parciales armónicos. Antes de publicar conviene sustituir lluvia, bosque, océano, viento y fuego por grabaciones reales con licencia documentada.

## Privacidad

El permiso de accesibilidad se usa solo para saber qué app pasa al frente y mostrar la pausa. Tranqui no lee ni guarda el contenido de la pantalla y no se conecta a internet.


## Audio ambiental y reproducción en segundo plano (v0.4.0)

El reproductor utiliza AndroidX Media3 (`MediaSessionService` + ExoPlayer). El motor vive en un servicio independiente de la pantalla Calma, por lo que la navegación Compose/WebView no detiene el audio. Android gestiona la notificación multimedia y los controles del sistema/pantalla de bloqueo.

Los paisajes grabados son CC0 y sus autores/fuentes están documentados en `app/src/main/assets/soundscape/CREDITS.md`. La primera reproducción requiere conexión; Media3 mantiene una caché local limitada (120 MB), pero los audios ambientales no se descargan íntegramente para uso offline en esta versión. El ruido blanco y la pausa del cuenco están incluidos como WAV locales.

Permisos añadidos: Internet, foreground service de tipo mediaPlayback y wake lock.
