# Revisión de diseño y código — Tranqui

## Cambios aplicados

- Nueva pantalla **Hoy** con una bienvenida cálida, un paisaje abstracto en capas y mensajes menos exigentes.
- Accesos rápidos de 1, 5 y 15 minutos reorganizados en tarjetas compactas con colores de la paleta de marca.
- Objetivos diarios presentados como pequeños pasos, con estados visuales más suaves.
- Se conserva la lógica existente de objetivos, sugerencias adaptativas, tarea importante, actividad en curso y progreso semanal.
- Se redondearon ligeramente más las tarjetas y se refinó el espaciado general.
- Nuevo icono vectorial adaptativo: sol arcilla + bruma salvia + ondas verde bosque. Se mantiene nítido en distintos tamaños y no depende de una imagen rasterizada.

## Dirección visual

- Lino: `#F3EFE5`
- Salvia: `#A7B39B`
- Bosque: `#344C40`
- Arcilla: `#D5A080`
- Arena: `#D9D2C5`

La intención es un punto medio entre claridad funcional y abstracción orgánica: las formas dan identidad, pero las acciones siguen siendo reconocibles.

## Mejoras recomendadas para próximas iteraciones

1. **Audio persistente:** el reproductor de paisajes sonoros vive dentro de una WebView en la pestaña Calma. Según los pendientes del proyecto, el sonido se corta al cambiar de pestaña o entrar en una actividad. Conviene mover la reproducción a un servicio/controlador de audio compartido.
2. **Espacio de lectura:** el código actual tiene métodos, objetivos, calma y progreso, pero todavía no aparece una sección de lectura dedicada. Se puede sumar con temporizador opcional, modo sin distracciones y textos guardados localmente.
3. **Pausa contextual:** hoy la pausa aparece al abrir las apps elegidas, y ofrece “Entrar igual”. Es coherente con el bloqueo suave. Más adelante se puede ofrecer un modo opcional que muestre la pausa solo durante una sesión de concentración, sin convertirlo en un bloqueo duro.
4. **Accesibilidad visual:** revisar contraste de textos secundarios, escalado de fuente del sistema, áreas táctiles y compatibilidad con “reducir movimiento”. Las brumas animadas deberían tener una alternativa estática.
5. **Pruebas de ciclo de vida:** probar reproducción al apagar pantalla, volver desde otra app, rotación, cierre forzado y reinicio del teléfono; revisar también notificaciones y alarmas en Android 13+.

## Seguridad de publicación — importante

El ZIP original contiene `keystore/tranqui.jks` y el archivo Gradle tiene las contraseñas de firma escritas directamente. No publiques ese archivo ni el repositorio con esas credenciales expuestas. Para futuras publicaciones, conviene mover contraseñas y material de firma a secretos de GitHub Actions. No cambié la configuración de firma en esta revisión para no romper la posibilidad de instalar actualizaciones encima de una versión ya instalada. Si la clave ya se publicó en un repositorio público, hay que evaluar el riesgo antes de distribuir nuevas versiones; cambiarla puede impedir actualizar instalaciones existentes.

## Verificación

Se validó la estructura XML de los recursos. No se pudo ejecutar una compilación Gradle en este entorno porque el wrapper necesita descargar Gradle 8.9 y no hubo conexión al servidor de distribución. Por eso, el APK debe compilarse y probarse en Android Studio o en GitHub Actions antes de instalarlo.
