package com.tranqui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Modo actividad: un cronómetro que no gasta. Podés apagar la pantalla. */
@Composable
fun ActivityScreen(nav: Nav) {
    val ctx = LocalContext.current
    Store.tick.intValue
    val a = Store.active()
    var finished by remember { mutableStateOf<Pair<String, Int>?>(null) }

    finished?.let { (label, mins) ->
        Celebration(label, mins) { nav.pop() }
        return
    }
    if (a == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(a.paused) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val el = a.elapsed(now)
    val goal = Store.goal(a.goalId)
    val reached = el >= a.minMinutes * 60_000L

    Box(Modifier.fillMaxSize()) {
        Hills(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(230.dp))
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE9E4D8)).clickable { nav.pop() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = C.Bosque) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    SerifText(a.label, 22, align = TextAlign.Center)
                    if (goal != null && goal.anchor.isNotBlank()) {
                        Small("Después de ${goal.anchor}, ${goal.minutes} minutos", align = TextAlign.Center)
                    }
                }
                Spacer(Modifier.width(44.dp))
            }

            Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
                Mist(Modifier.matchParentSize(), listOf(C.Salvia, C.Salvia, C.Musgo), count = 3, alpha = 0.5f, periodMs = 40000)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SerifText(fmtClock(el), 60, color = C.BosqueOscuro)
                    SerifText(if (a.paused) "En pausa" else "Un paso a la vez", 17, color = C.BosqueOscuro, italic = true)
                }
            }

            if (reached) {
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(C.SalviaClara).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = C.Bosque, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Body("Mínimo de hoy cumplido: ${a.minMinutes} min", size = 15)
                }
            } else {
                Small("Mínimo: ${a.minMinutes} min. Después podés seguir todo lo que quieras.", align = TextAlign.Center)
            }
            if (goal != null && goal.pleasure.isNotBlank()) {
                Small("Tu placer asociado: ${goal.pleasure}", align = TextAlign.Center)
            }
            Small("Podés apagar la pantalla. Cuando vuelvas al celular, te aviso cuánto llevás.", align = TextAlign.Center)

            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(if (a.paused) "Seguir" else "Pausar", Modifier.weight(1f), bg = C.Lino, fg = C.Bosque) {
                    if (a.paused) resumeActivityMode(ctx) else pauseActivityMode(ctx)
                }
                PrimaryButton("Terminar", Modifier.weight(1f), bg = C.Arcilla, fg = C.ArcillaOscura) {
                    val label = a.label
                    val mins = finishActivityMode(ctx)
                    finished = label to mins
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun Celebration(label: String, minutes: Int, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            Mist(Modifier.matchParentSize(), listOf(C.Salvia, C.Arcilla, C.Salvia), count = 3, alpha = 0.45f, periodMs = 20000)
            SerifText("Bien hecho", 34, color = C.BosqueOscuro)
        }
        Spacer(Modifier.height(12.dp))
        Body("$minutes ${if (minutes == 1) "minuto" else "minutos"}: $label", align = TextAlign.Center)
        Small("Suma a tu constancia de la semana.", align = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Listo", onClick = onDone)
    }
}
