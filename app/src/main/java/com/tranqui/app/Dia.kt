package com.tranqui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun ahoraMin(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
private fun hm(m: Int) = "%02d:%02d".format((m / 60) % 24, m % 60)

/**
 * «Tu día»: una línea suave con lo que tiene hora hoy (los bloques de tiempo) y una marca
 * de «ahora». Lo que ya pasó se aclara; lo que sigue se ve sin apuro. Sin números de rendimiento.
 */
@Composable
fun DiaLinea(nav: Nav) {
    Store.tick.intValue
    var ahora by remember { mutableIntStateOf(ahoraMin()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); ahora = ahoraMin() } }
    val today = Store.today()
    val bloques = Store.items("bloques").filter { it.day == today && it.time >= 0 }.sortedBy { it.time }
    val sinHora = (Store.items("bloques").filter { it.day == today && it.time < 0 && !it.done } +
        Store.items("ivy").filter { it.day == today && !it.done }.sortedBy { it.order }).take(3)
    val vence = Store.items("plazos").filter { !it.done && it.due == today }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            SerifText("Tu día", 24)
            Spacer(Modifier.weight(1f))
            Small(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d", Locale("es"))).replaceFirstChar { it.uppercase() },
                size = 12
            )
        }
        TCard(bg = C.Papel) {
            vence.forEach { p ->
                Small("Hoy vence: ${p.text}", color = C.ArcillaOscura)
            }
            if (bloques.isEmpty()) {
                Body("Tu día está libre.", color = C.Texto2)
                Small("Si querés, dale un lugar a algo. Lo demás puede esperar.")
            } else {
                var marcaPuesta = false
                bloques.forEach { b ->
                    val fin = b.time + b.minutes
                    if (!marcaPuesta && b.time > ahora) {
                        AhoraRow(ahora); marcaPuesta = true
                    }
                    val enCurso = ahora in b.time until fin
                    if (enCurso) marcaPuesta = true
                    BloqueRow(b, pasado = fin <= ahora, enCurso = enCurso, faltan = b.time - ahora, nav = nav)
                }
                if (!marcaPuesta) AhoraRow(ahora)
            }
            TextButton(onClick = { nav.push(Route.Tool("BLOQUES")) }) {
                Text(if (bloques.isEmpty()) "+ Darle un lugar a algo" else "+ Sumar o cambiar", color = C.Bosque)
            }
        }
        if (sinHora.isNotEmpty()) {
            Small("Cuando puedas", size = 12)
            sinHora.forEach { it2 ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.SalviaClara.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Body(it2.text, Modifier.weight(1f), size = 15)
                    TextButton(onClick = { nav.start(it2.text, null, if (it2.minutes > 0) it2.minutes else 25) }) {
                        Text("Empezar", color = C.Bosque)
                    }
                }
            }
        }
    }
}

/** La línea vertical con su punto, a la izquierda de cada momento. */
private fun Modifier.linea(color: androidx.compose.ui.graphics.Color, punto: androidx.compose.ui.graphics.Color, radio: Float) =
    drawBehind {
        val x = 10.dp.toPx()
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
        drawCircle(punto, radius = radio * density, center = Offset(x, size.height / 2f))
    }

@Composable
private fun AhoraRow(ahora: Int) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 30.dp).linea(C.Borde, C.Musgo, 5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(28.dp))
        Box(Modifier.weight(1f).height(1.dp).background(C.Musgo.copy(alpha = 0.5f)))
        Spacer(Modifier.width(8.dp))
        Small("Ahora · ${hm(ahora)}", color = C.Musgo, size = 12)
    }
}

@Composable
private fun BloqueRow(b: Item, pasado: Boolean, enCurso: Boolean, faltan: Int, nav: Nav) {
    Row(
        Modifier.fillMaxWidth().alpha(if (pasado) 0.45f else 1f)
            .linea(C.Borde, if (enCurso) C.Bosque else C.Salvia, if (enCurso) 6f else 4f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(28.dp))
        Column(
            Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                .background(if (enCurso) C.SalviaClara else C.Lino)
                .clickable { nav.push(Route.Tool("BLOQUES")) }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Small("${hm(b.time)} – ${hm(b.time + b.minutes)}", size = 12)
            Body(b.text, bold = enCurso)
            if (!pasado && !b.done) {
                when {
                    enCurso -> SoftButton("Empezar ahora") { nav.start(b.text, null, b.minutes) }
                    faltan in 1..60 -> Small("En $faltan min, sin apuro", color = C.Musgo, size = 12)
                }
            }
        }
    }
}

/** Desde la pausa, «Cansado/a»: opciones para descansar de verdad. */
@Composable
fun DescansoScreen(nav: Nav) {
    val noche = ahoraMin() !in (7 * 60)..(21 * 60)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar("Descansar") { nav.pop() }
        SerifText("Quizás lo que pide el cuerpo es descanso, no pantalla.", 22)
        Small("Elegí lo que te venga bien. No hay que hacer nada más.")
        if (noche) DescansoOpcion("Respiración para dormir", "La exhalación se va alargando sola") {
            nav.push(Route.PracticeRun("dormir"))
        }
        DescansoOpcion("Cerrar los ojos con un sonido", "Lluvia, río, fuego… elegí uno en Calma") { nav.tab(1) }
        DescansoOpcion("Soltar el cuerpo", "Relajación guiada, 5 minutos") { nav.push(Route.PracticeRun("relajacion", 5)) }
        DescansoOpcion("Estirar cuello y hombros", "1 minuto") { nav.start("Estirar cuello y hombros", null, 1) }
    }
}

@Composable
private fun DescansoOpcion(title: String, sub: String, onClick: () -> Unit) {
    TCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(C.Salvia))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Body(title, bold = true)
                Small(sub)
            }
            Chevron()
        }
    }
}
