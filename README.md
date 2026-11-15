# Tranqui

App para Android que ayuda a dejar de procrastinar y a cumplir objetivos de a poco, con métodos que tienen evidencia. Todo se guarda en el teléfono: sin cuentas, sin servidor y sin internet.

## Qué tiene la versión 0.1

- **Hoy:** tres botones (1, 5 y 15 minutos) con opciones que se pueden hacer en cualquier lugar, tus objetivos del día, el sapo del día y la constancia de la semana.
- **Objetivos con paso mínimo:** elegís una plantilla (leer, caminar, inglés…) y tu nivel; Tranqui te propone cuántos minutos hacer por día y te sugiere subir o bajar según cómo te va. Incluye el plan «si pasa X, hago Y», recordatorio diario y placer asociado.
- **Modo actividad:** guarda solo la hora de inicio, así que casi no gasta batería. Notificación con cronómetro y aviso cuando volvés a agarrar el celular.
- **Pausa antes de abrir apps:** respiración, «No entrar» fijo, opciones de 1, 5 y 15 minutos y «Entrar igual» que rebota y desaparece a los 14 segundos (configurable).
- **Calma:** cinco paisajes sonoros (Bruma, Cuencos, Agua, Bosque, Fuego) con control de tono, afinación opcional a 432 Hz y temporizador, más cuatro prácticas guiadas (suspiro cíclico, respiración en caja, meditación y relajación muscular).
- **Métodos:** 13 fichas con su nivel de evidencia y un botón para usarlas (WOOP, Kanban, Hansei, Ikigai, Pomodoro, Kaizen…).
- **Progreso:** días de la semana, camino a los 66 días por objetivo (no vuelve a cero), minutos del mes y veces que elegiste no entrar.

## Cómo instalarla

1. Entrá a la pestaña **Releases** del repositorio y descargá `Tranqui-vX.Y.Z.apk`.
2. Abrilo en el teléfono y aceptá instalar desde esta fuente.
3. En Tranqui, tocá el engranaje (Ajustes) → **Activar en Ajustes de Android** y prendé «Tranqui: pausa antes de abrir apps». Después elegí qué apps muestran la pausa.

Cada versión nueva se instala encima de la anterior sin perder datos, porque todas se firman con la misma clave.

## Cómo se compila

GitHub Actions compila el APK solo en cada cambio que se sube a `main` (queda en la pestaña **Actions**, como artefacto). Para publicar una versión:

1. Subí `versionCode` (por ejemplo de 1 a 2) y `versionName` (de "0.1.0" a "0.2.0") en `app/build.gradle.kts`.
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

Poné archivos `agua`, `bosque`, `fuego` y `cuenco` (`.ogg`, `.mp3` o `.wav`) en `app/src/main/assets/soundscape/sounds/`. Si están, se usan en lugar del sonido generado. Más detalles en el `LEEME.txt` de esa carpeta.

## Privacidad

El permiso de accesibilidad se usa solo para saber qué app pasa al frente y mostrar la pausa. Tranqui no lee ni guarda el contenido de la pantalla y no se conecta a internet.
