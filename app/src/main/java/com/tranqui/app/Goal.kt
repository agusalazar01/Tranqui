package com.tranqui.app

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GoalEditScreen(id: String?, nav: Nav) {
    val ctx = LocalContext.current
    val existing = remember(id) { Store.goal(id) }
    var templateId by remember { mutableStateOf(existing?.templateId ?: "leer") }
    var name by remember { mutableStateOf(existing?.name ?: "Leer") }
    var level by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(existing?.minutes ?: 5) }
    var anchor by remember { mutableStateOf(existing?.anchor ?: "") }
    var rh by remember { mutableIntStateOf(existing?.reminderHour ?: -1) }
    var rm by remember { mutableIntStateOf(existing?.reminderMinute ?: 0) }
    var pleasure by remember { mutableStateOf(existing?.pleasure ?: "") }
    val template = TEMPLATES.find { it.id == templateId } ?: TEMPLATES.last()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackBar(if (existing == null) "Nuevo objetivo" else "Editar objetivo") { nav.pop() }

        if (existing == null) {
            SectionLabel("¿Qué querés hacer un poco cada día?")
            TEMPLATES.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { t ->
                        Choice(t.name, t.id == templateId, Modifier.weight(1f)) {
                            templateId = t.id
                            name = if (t.id == "propio") "" else t.name
                            minutes = t.levels[level]
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        OutlinedTextField(
            value = name, onValueChange = { name = it }, label = { Text("Nombre") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        if (existing == null) {
            SectionLabel("¿Cuánto lo hacés hoy?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LEVELS.forEachIndexed { i, l ->
                    Choice(l, i == level, Modifier.weight(1f)) { level = i; minutes = template.levels[i] }
                }
            }
        }

        TCard(bg = C.SalviaClara, bordered = false) {
            Small("Paso diario")
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton("−") { if (minutes > 1) minutes-- }
                SerifText("$minutes min", 30, Modifier.weight(1f).padding(horizontal = 12.dp))
                RoundButton("+") { if (minutes < 180) minutes++ }
            }
            Small(
                if (existing == null) "Te proponemos ${template.levels[level]} minutos por día. Si te parece mucho, bajalo: lo importante es empezar."
                else "Podés cambiarlo cuando quieras."
            )
        }

        SectionLabel("Si pasa X, hago Y")
        OutlinedTextField(
            value = anchor, onValueChange = { anchor = it },
            label = { Text("Después de… (algo que ya hacés)") },
            placeholder = { Text("cenar") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        if (anchor.isNotBlank() && name.isNotBlank()) {
            Body("«Después de ${anchor.trim()}, voy a ${name.trim().lowercase()} $minutes minutos.»", color = C.Texto2)
        }

        SectionLabel("Recordatorio")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton(if (rh < 0) "Sin recordatorio" else "%02d:%02d".format(rh, rm)) {
                TimePickerDialog(ctx, { _, h, m -> rh = h; rm = m }, if (rh < 0) 21 else rh, rm, true).show()
            }
            if (rh >= 0) TextButton(onClick = { rh = -1 }) { Text("Quitar", color = C.Texto2) }
        }

        SectionLabel("Placer asociado (opcional)")
        OutlinedTextField(
            value = pleasure, onValueChange = { pleasure = it },
            label = { Text("Algo que solo disfrutás mientras lo hacés") },
            placeholder = { Text("Un podcast, una playlist…") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        PrimaryButton("Guardar") {
            if (name.isBlank()) return@PrimaryButton
            val g = Goal(
                existing?.id ?: Goal.newId(), name.trim(), templateId, minutes, anchor.trim(), rh, rm,
                existing?.startDay ?: Store.today(), pleasure.trim()
            )
            Store.saveGoal(g)
            Reminders.schedule(ctx, g)
            nav.pop()
        }
        if (existing != null) {
            TextButton(onClick = {
                Reminders.cancel(ctx, existing.id)
                Store.deleteGoal(existing.id)
                nav.pop()
            }) { Text("Eliminar objetivo", color = C.ArcillaOscura) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun RoundButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(C.Papel).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = C.Bosque, fontSize = 22.sp) }
}
