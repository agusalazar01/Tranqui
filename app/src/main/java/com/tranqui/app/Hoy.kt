package com.tranqui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.unit.dp

@Composable
fun HoyScreen(nav: Nav) {
    Store.tick.intValue
    val goals = Store.goals()
    val active = Store.active()

    Box(Modifier.fillMaxSize().background(C.Lino)) {
        Mist(
            Modifier.align(Alignment.TopEnd).offset(x = 110.dp, y = (-100).dp).size(300.dp),
            listOf(C.Salvia, C.Arena, C.ArcillaClara), count = 4, alpha = 0.32f, periodMs = 72000
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    SerifText("Tranqui", 27)
                    Small("Encontrá tu ritmo", size = 12)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { nav.push(Route.Settings) }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = C.Bosque)
                }
            }

            HomeWelcome()

            val ctx = LocalContext.current
            var pauseOn by remember { mutableStateOf(pauseServiceEnabled(ctx)) }
            LifecycleResumeEffect(Unit) {
                pauseOn = pauseServiceEnabled(ctx)
                onPauseOrDispose { }
            }
            if (!pauseOn) {
                TCard(onClick = { nav.push(Route.Settings) }, bg = C.ArcillaClara, bordered = false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OrganicMark(42, warm = true)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Body("Activá la pausa antes de abrir apps", color = C.ArcillaOscura, bold = true)
                            Small("Son 2 o 3 pasos, con accesos directos.", color = C.ArcillaOscura)
                        }
                        Chevron(C.ArcillaOscura)
                    }
                }
            }

            if (active != null) {
                TCard(
                    onClick = { nav.push(Route.Activity) },
                    bg = C.Bosque, bordered = false
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Mist(Modifier.size(48.dp), listOf(C.Salvia, C.Arena), count = 3, alpha = 0.55f)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Body(if (active.paused) "Tu actividad está en pausa" else "Estás en modo actividad", color = C.Lino, bold = true)
                            Small(active.label, color = C.Arena)
                        }
                        Chevron(C.Lino)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    SerifText("¿Qué necesitás hoy?", 24)
                    Spacer(Modifier.weight(1f))
                    Small("A tu ritmo", size = 12)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickTile("1", "minuto", "Una pausa", C.ArcillaClara, C.ArcillaOscura, Modifier.weight(1f)) { nav.quick(1) }
                    QuickTile("5", "minutos", "Un pequeño paso", C.SalviaClara, C.Bosque, Modifier.weight(1f)) { nav.quick(5) }
                    QuickTile("15", "minutos", "Enfocarte", C.Bosque, C.Lino, Modifier.weight(1f)) { nav.quick(15) }
                }
            }

            if (Stats.missedYesterday() && goals.isNotEmpty()) {
                TCard(bg = C.ArcillaClara, bordered = false) {
                    SerifText("Ayer no se pudo. Está bien.", 20, color = C.ArcillaOscura)
                    Small("Podés volver con dos minutos. Sin recuperar nada, sin culpa.", color = C.ArcillaOscura)
                    SoftButton("Empezar 2 minutos") {
                        val g = goals.first()
                        nav.start(g.name, g.id, 2)
                    }
                }
            }

            goals.forEach { g ->
                Stats.suggestion(g)?.let { newMin ->
                    TCard(bg = C.SalviaClara, bordered = false) {
                        val up = newMin > g.minutes
                        Body(
                            if (up) "Venís muy bien con ${g.name.lowercase()}." else "Hagamos ${g.name.lowercase()} más fácil.",
                            bold = true
                        )
                        Small(
                            if (up) "Cumpliste 6 de los últimos 7 días. ¿Subimos de ${g.minutes} a $newMin minutos?"
                            else "Esta semana costó. ¿Bajamos de ${g.minutes} a $newMin minutos? Lo importante es encontrar un ritmo posible."
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            SoftButton(if (up) "Subir" else "Bajar") {
                                Store.saveGoal(g.copy(minutes = newMin)); Store.setLastSuggest(g.id)
                            }
                            SoftButton("Dejar igual") { Store.setLastSuggest(g.id) }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SerifText("Tus pequeños pasos", 24)
                Spacer(Modifier.weight(1f))
                Small("Hoy", size = 12)
            }
            if (goals.isEmpty()) {
                TCard(onClick = { nav.push(Route.GoalEdit(null)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OrganicMark(54)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            SerifText("Empezá por algo pequeño", 20)
                            Small("Creá tu primer objetivo: leer, caminar, estudiar…")
                        }
                        Chevron()
                    }
                }
            }
            goals.forEach { g ->
                val done = Stats.doneToday(g.id)
                TCard(bg = if (done) C.SalviaClara.copy(alpha = 0.8f) else C.Papel) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(30.dp).clip(CircleShape)
                                .background(if (done) C.Bosque else Color.Transparent)
                                .border(1.4.dp, if (done) C.Bosque else C.Salvia, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (done) Icon(Icons.Filled.Check, contentDescription = "Hecho", tint = C.Lino, modifier = Modifier.size(16.dp))
                            else Box(Modifier.size(8.dp).clip(CircleShape).background(C.Salvia))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).clickable { nav.push(Route.GoalEdit(g.id)) }) {
                            Body(g.name, bold = true)
                            Small("${g.minutes} min${if (g.anchor.isNotBlank()) " · Después de ${g.anchor}" else " · Tu ritmo de hoy"}")
                        }
                        if (!done) TextButton(onClick = { nav.start(g.name, g.id, g.minutes) }) { Text("Empezar", color = C.Bosque) }
                        else Small("Listo", color = C.Musgo)
                    }
                }
            }
            TextButton(onClick = { nav.push(Route.GoalEdit(null)) }) { Text("+ Agregar un objetivo", color = C.Bosque) }

            val sapo = Store.sapo()
            TCard(onClick = { nav.sapo() }, bg = C.Papel) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OrganicMark(42, warm = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Body(if (sapo == null) "Elegí tu tarea importante" else "Tu tarea importante", bold = true)
                        Small(if (sapo == null) "La que venís postergando. Podés empezar de a poco." else sapo)
                        Store.sapoStep()?.takeIf { it.isNotBlank() }?.let { Small("Primer paso: $it", color = C.Musgo) }
                    }
                    Chevron()
                }
            }

            TCard(onClick = { nav.tab(3) }, bg = C.SalviaClara, bordered = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Body("Tu semana, sin exigencias", bold = true)
                        Small("${Stats.weekCount()} de 7 días con algún pequeño paso")
                    }
                    OrganicMark(42)
                    Chevron()
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HomeWelcome() {
    Box(
        Modifier.fillMaxWidth().height(178.dp).clip(RoundedCornerShape(30.dp))
            .background(Brush.verticalGradient(listOf(C.Papel, C.Lino, C.SalviaClara.copy(alpha = 0.8f))))
    ) {
        Hills(Modifier.fillMaxWidth().height(112.dp).align(Alignment.BottomCenter))
        Mist(Modifier.size(170.dp).align(Alignment.TopEnd).offset(x = 28.dp, y = (-42).dp), listOf(C.Arcilla, C.Salvia), count = 3, alpha = 0.24f)
        Column(Modifier.align(Alignment.CenterStart).padding(start = 22.dp, end = 18.dp)) {
            SerifText("Hola, qué bueno verte.", 27)
            Spacer(Modifier.height(5.dp))
            Body("No hace falta hacer todo.\nEmpezá por lo que importa.", color = C.Texto2, size = 14)
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 14.dp)) {
            SerifText("Un paso a la vez", 14, color = C.Lino, italic = true)
        }
    }
}

@Composable
private fun QuickTile(
    number: String, unit: String, label: String, bg: Color, fg: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(23.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SerifText(number, 29, color = fg)
        Body(unit, color = fg, size = 12, bold = true)
        Spacer(Modifier.height(5.dp))
        Small(label, color = fg.copy(alpha = 0.82f), size = 11)
    }
}

@Composable
private fun OrganicMark(size: Int, warm: Boolean = false) {
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Mist(Modifier.fillMaxSize(), if (warm) listOf(C.Arcilla, C.Arena) else listOf(C.Salvia, C.Musgo), count = 3, alpha = 0.52f, periodMs = 48000)
        Blob(if (warm) C.Arcilla.copy(alpha = 0.35f) else C.Salvia.copy(alpha = 0.45f), size / 2, 1)
    }
}
