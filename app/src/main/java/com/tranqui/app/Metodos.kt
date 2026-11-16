package com.tranqui.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MetodosScreen(nav: Nav) {
    Box(Modifier.fillMaxSize()) {
        Mist(Modifier.offset(x = (-110).dp, y = (-90).dp).size(300.dp), listOf(C.Arcilla, C.Arena), count = 3, alpha = 0.25f)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScreenTitle("Métodos", "Agrupados por para qué te sirven")
            Spacer(Modifier.height(4.dp))
            METHOD_GROUPS.forEach { (gid, info) ->
              val delGrupo = METHODS.filter { it.group == gid }
              if (delGrupo.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                SerifText(info.first, 21)
                Small(info.second)
              }
              delGrupo.forEach { m ->
                TCard(onClick = { nav.push(Route.MethodDetail(m.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SerifText(m.name, 19)
                            Small(m.short)
                        }
                        Spacer(Modifier.width(8.dp))
                        Tag(m.tag)
                    }
                    if (m.action != null && m.action.name in TOOL_ACTIONS) Small("Con herramienta guiada", color = C.Musgo)
                }
              }
            }
            Small(
                "Fuerte: metaanálisis o varios ensayos. Mixta: estudios con resultados distintos. " +
                    "Tradición: sin estudios propios, pero coherente con métodos que sí los tienen."
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun MethodScreen(id: String, nav: Nav) {
    val m = METHODS.firstOrNull { it.id == id } ?: METHODS.first()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar(m.name) { nav.pop() }
        if (m.jp != null) Small(m.jp)
        Tag(m.tag)
        SectionLabel("Qué es")
        Body(m.que)
        SectionLabel("Por qué funciona")
        Body(m.porque)
        SectionLabel("Qué dice la evidencia")
        Body(m.evidencia, color = C.Texto2, size = 15)
        SectionLabel("Cómo se hace")
        m.pasos.forEachIndexed { i, s -> Body("${i + 1}. $s") }
        Spacer(Modifier.height(8.dp))
        when (m.action) {
            MethodAction.NEW_GOAL -> PrimaryButton("Usar ahora: crear un objetivo") { nav.push(Route.GoalEdit(null)) }
            MethodAction.WOOP -> PrimaryButton("Usar ahora") { nav.push(Route.Notes("woop")) }
            MethodAction.HANSEI -> PrimaryButton("Hacer la revisión") { nav.push(Route.Notes("hansei")) }
            MethodAction.IKIGAI -> PrimaryButton("Usar ahora") { nav.push(Route.Notes("ikigai")) }
            MethodAction.KANBAN -> PrimaryButton("Abrir mi tablero") { nav.push(Route.Kanban) }
            MethodAction.POMODORO -> {
                PrimaryButton("Pomodoro de 25 minutos") { nav.start("Pomodoro", null, 25) }
                SoftButton("Bloque corto de 15 minutos", Modifier.fillMaxWidth()) { nav.start("Foco", null, 15) }
            }
            MethodAction.MICRO -> PrimaryButton("Ver pausas de 1 minuto") { nav.quick(1) }
            MethodAction.KAIZEN -> PrimaryButton("Empezar 1 minuto") { nav.start("Un minuto de la tarea que evitás", null, 1) }
            MethodAction.SAPO -> PrimaryButton("Elegir el sapo de hoy") { nav.sapo() }
            null -> {}
            else -> PrimaryButton(
                when (m.action) {
                    MethodAction.EISENHOWER -> "Ordenar mis tareas"
                    MethodAction.IVY -> "Armar mi lista de 6"
                    MethodAction.UNO35 -> "Armar mi día 1-3-5"
                    MethodAction.DOSMIN -> "Mi lista de 2 minutos"
                    MethodAction.VACIAR -> "Vaciar la mente ahora"
                    MethodAction.PASOS -> "Dividir una tarea"
                    MethodAction.BLOQUES -> "Planificar mis bloques"
                    MethodAction.PERDON -> "Hacerlo ahora (3 pasos)"
                    MethodAction.PLAZO -> "Ponerle fecha a algo"
                    MethodAction.CIERRE -> "Cerrar el día"
                    MethodAction.PLACER -> "Asociarlo a un objetivo"
                    else -> "Usar ahora"
                }
            ) { nav.push(Route.Tool(m.action!!.name)) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private val NOTE_FORMS = mapOf(
    "woop" to Triple(
        "WOOP", "Tomate 2 minutos. Escribí corto y concreto.",
        listOf("Deseo", "Mejor resultado", "Obstáculo interno", "Plan: si aparece…, voy a…")
    ),
    "hansei" to Triple(
        "Revisión de la semana", "Con honestidad y sin culparte. Sirve para la semana que viene.",
        listOf("¿Qué salió bien?", "¿Qué te frenó?", "¿Qué vas a cambiar?")
    ),
    "ikigai" to Triple(
        "Ikigai", "Lo que aparece en más de una respuesta puede ser un buen objetivo.",
        listOf("Lo que te gusta", "En lo que sos bueno", "Lo que el mundo necesita", "Lo que te pueden pagar")
    )
)

@Composable
fun NotesScreen(kind: String, nav: Nav) {
    Store.tick.intValue
    val form = NOTE_FORMS[kind] ?: NOTE_FORMS.getValue("woop")
    val values = remember(kind) { mutableStateListOf(*Array(form.third.size) { "" }) }
    val notes = Store.notes(kind)
    val fmt = DateTimeFormatter.ofPattern("d/M")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar(form.first) { nav.pop() }
        Small(form.second)
        form.third.forEachIndexed { i, label ->
            OutlinedTextField(
                value = values[i], onValueChange = { values[i] = it }, label = { Text(label) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        PrimaryButton("Guardar") {
            if (values.any { it.isNotBlank() }) {
                Store.addNote(kind, form.third.mapIndexed { i, l -> l to values[i].trim() })
                for (i in values.indices) values[i] = ""
            }
        }
        if (notes.isNotEmpty()) {
            SectionLabel("Anteriores")
            notes.take(10).forEach { n ->
                TCard {
                    Small(LocalDate.ofEpochDay(n.day).format(fmt))
                    n.fields.filter { it.second.isNotBlank() }.forEach { (k, v) -> Body("$k: $v", size = 15) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun KanbanScreen(nav: Nav) {
    Store.tick.intValue
    val tasks = Store.tasks()
    var text by remember { mutableStateOf("") }
    var warn by remember { mutableStateOf(false) }
    val cols = listOf("Por hacer", "Haciendo", "Hecho")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar("Mi tablero") { nav.pop() }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, label = { Text("Nueva tarea") },
                singleLine = true, modifier = Modifier.weight(1f)
            )
            SoftButton("Agregar") {
                if (text.isNotBlank()) {
                    Store.saveTasks(tasks + KTask(Goal.newId(), text.trim(), 0))
                    text = ""
                }
            }
        }
        if (warn) Small("Ya tenés 3 en «Haciendo». Terminá una antes de empezar otra.", color = C.ArcillaOscura)
        cols.forEachIndexed { ci, title ->
            SectionLabel("$title (${tasks.count { it.col == ci }})")
            tasks.filter { it.col == ci }.forEach { t ->
                TCard(bg = if (ci == 1) C.SalviaClara else C.Papel) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Body(t.text, Modifier.weight(1f))
                        if (ci < 2) {
                            TextButton(onClick = {
                                if (ci == 0 && tasks.count { it.col == 1 } >= 3) {
                                    warn = true
                                } else {
                                    warn = false
                                    Store.saveTasks(tasks.map { if (it.id == t.id) it.copy(col = ci + 1) else it })
                                }
                            }) { Text(if (ci == 0) "Empezar" else "Hecho", color = C.Bosque) }
                        }
                        IconButton(onClick = { Store.saveTasks(tasks.filter { it.id != t.id }) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Borrar", tint = C.Texto2)
                        }
                    }
                }
            }
        }
        Small("Kanban viene de las fábricas de Toyota. Limitar lo que hacés a la vez evita dispersarte.", align = TextAlign.Start)
        Spacer(Modifier.height(16.dp))
    }
}

/** Acciones que abren una herramienta guiada propia (Herramientas.kt). */
val TOOL_ACTIONS = setOf("EISENHOWER", "IVY", "UNO35", "DOSMIN", "VACIAR", "PASOS", "BLOQUES", "PERDON", "PLAZO", "CIERRE", "PLACER")
