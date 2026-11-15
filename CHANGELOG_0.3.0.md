# Tranqui 0.3.0

## Incluido
- Se eliminó el concepto de Bruma/Agua como paisajes seleccionables y se reorganizó Calma en siete ambientes: Lluvia, Bosque, Océano, Viento, Cuenco, Fuego y Ruido blanco.
- Se rediseñó el reproductor de ambientes: selector compacto, visualización orgánica animada, nombres y descripciones por ambiente, control de volumen y temporizador.
- Se rearmó el motor Web Audio con capas de ruido filtrado, modulación lenta, paneo estéreo, compresión y limitación de salida.
- Calma permanece compuesta al cambiar entre pestañas para evitar que el WebView se destruya al salir de la pestaña.
- El «sapo del día» ahora permite definir una primera acción concreta de dos minutos y empezar esa acción desde el diálogo.
- Se actualizó la versión de Android a 0.3.0 y compile/target SDK 35.

## Aclaraciones
- Los paisajes ambientales de esta versión son texturas generadas y procesadas en tiempo real, no grabaciones de campo. El cuenco utiliza parciales armónicos sintetizados. Se dejó preparada la carpeta de assets para incorporar grabaciones con licencia compatible.
- El audio se mantiene al cambiar de pestaña. Si se navega a una pantalla de detalle que desmonta la pantalla principal, o se cierra la app, la continuidad no está garantizada todavía; para eso hace falta migrar el motor a un servicio nativo de audio.
- No se añadió una sección de lectura digital, según la decisión del producto.
- La compilación no pudo verificarse en este entorno porque Gradle no pudo descargar su distribución al no haber conexión con services.gradle.org.
