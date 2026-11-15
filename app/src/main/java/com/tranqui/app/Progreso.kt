package com.tranqui.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProgresoScreen(nav: Nav) {
    Store.tick.intValue
    val goals = Store.goals()
    val week = Stats.week()
    val today = Store.today()
    val fmt = DateTimeFormatter.ofPattern("d/M")

    Box(Modifier.fillMaxSize()) {
        Mist(Modifier.offset(x = 190.dp, y = 30.dp).size(300.dp), listOf(C.Salvia, C.Arena), count = 3, alpha = 0.35f)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenTitle("Tu progreso", "Constancia, no perfección")

            TCard {
                SerifText("${week.count { it.second }} de 7 días esta semana", 22)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    week.forEachIndexed { i, (letter, done) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (done) Blob(C.Bosque, 30, i) else DashedCircle()
                            Spacer(Modifier.height(6.dp))
                            Small(letter)
                        }
                    }
                }
            }

            goals.forEach { g ->
                val days = Stats.goalDays(g)
                TCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Body("${g.name}: camino a los 66 días", Modifier.weight(1f), bold = true)
                        Small("Día $days")
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(C.SalviaClara)) {
                        Box(
                            Modifier.fillMaxWidth((days / 66f).coerceIn(0f, 1f)).fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp)).background(C.Musgo)
                        )
                    }
                    Small("${Stats.monthMinutes(g.id)} min este mes · ${Stats.last7(g.id)} de los últimos 7 días")
                }
            }
            if (goals.isNotEmpty()) Small("Estas barras no vuelven a cero si fallás un día.")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("${Stats.monthMinutes()} min", "de actividad este mes", C.SalviaClara, C.Bosque, Modifier.weight(1f))
                StatBox("${Stats.monthNoEntrar()} veces", "elegiste no entrar", C.ArcillaClara, C.ArcillaOscura, Modifier.weight(1f))
            }

            if (Stats.missedYesterday() && goals.isNotEmpty()) {
                TCard(bg = C.Bosque, bordered = false) {
                    SerifText("Ayer no pudiste, pasa. ¿Hacemos 2 minutos hoy?", 20, color = C.Lino)
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Empezar 2 minutos", bg = C.Lino, fg = C.Bosque) {
                        val g = goals.first()
                        nav.start(g.name, g.id, 2)
                    }
                }
            }

            TCard(onClick = { nav.push(Route.Notes("hansei")) }) {
                Body("Revisión de la semana", bold = true)
                Small("Qué salió, qué te frenó y qué vas a cambiar")
            }

            val recent = Store.sessions().takeLast(12).reversed()
            if (recent.isNotEmpty()) {
                SectionLabel("Últimas actividades")
                recent.forEach { s ->
                    val day = when (s.day) {
                        today -> "hoy"
                        today - 1 -> "ayer"
                        else -> LocalDate.ofEpochDay(s.day).format(fmt)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Body(s.label, Modifier.weight(1f), size = 15)
                        Small("${s.minutes} min · $day")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatBox(big: String, small: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(bg).padding(16.dp)) {
        SerifText(big, 26, color = fg)
        Small(small, color = fg)
    }
}

@Composable
private fun DashedCircle() {
    Canvas(Modifier.size(30.dp)) {
        drawCircle(
            C.Musgo, radius = size.minDimension / 2 - 2f,
            style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        )
    }
}
