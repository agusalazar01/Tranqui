package com.tranqui.app

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

// ---------------------------------------------------------------------------------------------
// Avisos de las herramientas (bloques de tiempo y fechas límite)
// ---------------------------------------------------------------------------------------------

class AvisoReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Notif.createChannels(ctx)
        Notif.simple(
            ctx, intent.getIntExtra("id", 300), Notif.CH_REC,
            intent.getStringExtra("title") ?: "Tranqui", intent.getStringExtra("text") ?: "",
            Notif.openApp(ctx, 40) { }
        )
    }
}

object Avisos {
    private fun pi(ctx: Context, id: Int, title: String = "", text: String = ""): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, id, Intent(ctx, AvisoReceiver::class.java).putExtra("id", id).putExtra("title", title).putExtra("text", text),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun at(ctx: Context, key: String, millis: Long, title: String, text: String) {
        if (millis <= System.currentTimeMillis()) return
        val id = 300 + (key.hashCode() and 0xFFFF)
        ctx.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi(ctx, id, title, text))
    }

    fun cancel(ctx: Context, key: String) {
        val id = 300 + (key.hashCode() and 0xFFFF)
        ctx.getSystemService(AlarmManager::class.java).cancel(pi(ctx, id))
    }
}

private fun millisOf(day: Long, minuteOfDay: Int): Long {
    val c = Calendar.getInstance()
    val d = LocalDate.ofEpochDay(day)
    c.set(d.year, d.monthValue - 1, d.dayOfMonth, minuteOfDay / 60, minuteOfDay % 60, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun hhmm(m: Int) = "%02d:%02d".format(m / 60, m % 60)

// ---------------------------------------------------------------------------------------------
// Pantalla común
// ---------------------------------------------------------------------------------------------

@Composable
fun ToolScreen(kind: String, nav: Nav) {
    val m = METHODS.firstOrNull { it.action?.name == kind }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar(m?.name ?: "Método") { nav.pop() }
        when (kind) {
            "EISENHOWER" -> Eisenhower(nav)
            "IVY" -> IvyLee(nav)
            "UNO35" -> UnoTresCinco(nav)
            "DOSMIN" -> DosMinutos(nav)
            "VACIAR" -> VaciarMente(nav)
            "PASOS" -> DividirPasos(nav)
            "BLOQUES" -> Bloques(nav)
            "PERDON" -> Perdon(nav)
            "PLAZO" -> Plazos(nav)
            "CIERRE" -> CierreDia(nav)
            "PLACER" -> PlacerAsociado(nav)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Campo para agregar una tarea con un botón. */
@Composable
private fun AddField(label: String, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text, onValueChange = { text = it }, label = { Text(label) },
            singleLine = true, modifier = Modifier.weight(1f)
        )
        SoftButton("Agregar") {
            if (text.isNotBlank()) { onAdd(text.trim()); text = "" }
        }
    }
}

@Composable
private fun CheckRow(text: String, done: Boolean, onToggle: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (done) C.Bosque else C.Borde).clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Filled.Check, contentDescription = "Hecho", tint = C.Lino, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Body(text, Modifier.weight(1f), color = if (done) C.Texto2 else C.Bosque)
        trailing()
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Filled.Close, contentDescription = "Borrar", tint = C.Texto2) }
}

/** Un mensaje de ánimo que cambia según cuánto llevás. */
@Composable
private fun Progress(done: Int, total: Int) {
    if (total == 0) return
    val f = done / total.toFloat()
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(C.SalviaClara)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(C.Musgo))
    }
    Small(
        when {
            done == total -> "¡Todo hecho! Cerralo por hoy."
            done == 0 -> "Empezá por la primera. Solo esa."
            f < 0.5f -> "$done de $total. Vas bien, una por vez."
            else -> "$done de $total. Ya pasaste la mitad."
        }
    )
}

// ---------------------------------------------------------------------------------------------
// Urgente o importante (matriz de Eisenhower)
// ---------------------------------------------------------------------------------------------

private val CUADROS = listOf(
    Triple("q1", "Hacer ahora", "Urgente e importante"),
    Triple("q2", "Agendar", "Importante, sin apuro"),
    Triple("q3", "Acotar", "Urgente, no tan importante"),
    Triple("q4", "Soltar", "Ni urgente ni importante")
)

@Composable
private fun Eisenhower(nav: Nav) {
    Store.tick.intValue
    val items = Store.items("eisen")
    var pending by remember { mutableStateOf<String?>(null) }
    Small("Antes de ubicar cada tarea, preguntate qué ganás si la hacés. Lo urgente engaña: parece más importante de lo que es.")
    AddField("Nueva tarea") { pending = it }
    pending?.let { t ->
        TCard(bg = C.SalviaClara, bordered = false) {
            Body("«$t»", bold = true)
            Small("¿Dónde va?")
            CUADROS.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    fila.forEach { (id, nombre, _) ->
                        Choice(nombre, false, Modifier.weight(1f)) {
                            Store.saveItems("eisen", items + Item(Goal.newId(), t, id, day = Store.today()))
                            pending = null
                        }
                    }
                }
            }
        }
    }
    CUADROS.forEach { (id, nombre, desc) ->
        val enCuadro = items.filter { it.slot == id && !it.done }
        TCard(bg = if (id == "q1") C.ArcillaClara else C.Papel, bordered = id != "q1") {
            SerifText(nombre, 19, color = if (id == "q1") C.ArcillaOscura else C.Bosque)
            Small(desc)
            if (enCuadro.isEmpty()) Small("Vacío", color = C.Texto2)
            enCuadro.forEach { it2 ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Body(it2.text, Modifier.weight(1f), size = 15)
                    when (id) {
                        "q1" -> TextButton(onClick = { nav.start(it2.text, null, 25) }) { Text("Empezar", color = C.Bosque) }
                        "q2" -> TextButton(onClick = {
                            Store.saveItems("bloques", Store.items("bloques") + Item(Goal.newId(), it2.text, day = Store.today(), time = -1, minutes = 45))
                            Store.saveItems("eisen", items.map { x -> if (x.id == it2.id) x.copy(done = true) else x })
                            nav.push(Route.Tool("BLOQUES"))
                        }) { Text("Darle hora", color = C.Bosque) }
                        "q3" -> TextButton(onClick = { nav.start(it2.text, null, 10) }) { Text("10 min", color = C.Bosque) }
                        else -> {}
                    }
                    DeleteButton { Store.saveItems("eisen", items.filter { x -> x.id != it2.id }) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Ivy Lee: 6 tareas en orden, una por vez
// ---------------------------------------------------------------------------------------------

@Composable
private fun IvyLee(nav: Nav) {
    Store.tick.intValue
    val today = Store.today()
    var paraManana by remember { mutableStateOf(false) }
    val dia = if (paraManana) today + 1 else today
    val todas = Store.items("ivy")
    val lista = todas.filter { it.day == dia }.sortedBy { it.order }
    fun save(nueva: List<Item>) = Store.saveItems("ivy", todas.filter { it.day != dia } + nueva.mapIndexed { i, x -> x.copy(order = i) })

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice("Hoy", !paraManana) { paraManana = false }
        Choice("Mañana", paraManana) { paraManana = true }
    }
    val actual = lista.firstOrNull { !it.done }
    if (!paraManana && actual != null) {
        TCard(bg = C.Bosque, bordered = false) {
            Small("Ahora, solo esta:", color = C.Arena)
            SerifText(actual.text, 24, color = C.Lino)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Empezar", Modifier.weight(1f), bg = C.Lino, fg = C.Bosque) { nav.start(actual.text, null, 25) }
                PrimaryButton("La terminé", Modifier.weight(1f), bg = C.Arcilla, fg = C.ArcillaOscura) {
                    save(lista.map { if (it.id == actual.id) it.copy(done = true) else it })
                }
            }
        }
    }
    Progress(lista.count { it.done }, lista.size)
    if (lista.size < 6) AddField(if (paraManana) "Tarea para mañana" else "Tarea para hoy") { t ->
        save(lista + Item(Goal.newId(), t, day = dia, order = lista.size))
    } else Small("Ya tenés 6. Si querés sumar otra, sacá una.")
    lista.forEachIndexed { i, it2 ->
        CheckRow("${i + 1}. ${it2.text}", it2.done, { save(lista.map { x -> if (x.id == it2.id) x.copy(done = !x.done) else x }) }) {
            IconButton(onClick = {
                if (i > 0) save(lista.toMutableList().also { l -> l.add(i - 1, l.removeAt(i)) })
            }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir", tint = C.Texto2) }
            IconButton(onClick = {
                if (i < lista.lastIndex) save(lista.toMutableList().also { l -> l.add(i + 1, l.removeAt(i)) })
            }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar", tint = C.Texto2) }
        }
    }
    val quedan = lista.filter { !it.done }
    if (!paraManana && quedan.isNotEmpty()) {
        TextButton(onClick = {
            val manana = todas.filter { it.day == today + 1 }
            Store.saveItems("ivy", todas.filter { it.day != today } + lista.filter { it.done } +
                quedan.mapIndexed { i, x -> x.copy(day = today + 1, order = manana.size + i) })
        }) { Text("Pasar lo que quedó a mañana", color = C.Bosque) }
    }
}

// ---------------------------------------------------------------------------------------------
// Regla 1-3-5
// ---------------------------------------------------------------------------------------------

@Composable
private fun UnoTresCinco(nav: Nav) {
    Store.tick.intValue
    val today = Store.today()
    val todas = Store.items("135")
    val hoy = todas.filter { it.day == today }
    fun save(n: List<Item>) = Store.saveItems("135", todas.filter { it.day != today } + n)
    Progress(hoy.count { it.done }, 9)
    listOf(Triple("g", "1 grande", 1), Triple("m", "3 medianas", 3), Triple("c", "5 chicas", 5)).forEach { (slot, titulo, max) ->
        val enSlot = hoy.filter { it.slot == slot }
        TCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SerifText(titulo, 19, Modifier.weight(1f))
                Small("${enSlot.size}/$max")
            }
            enSlot.forEach { it2 ->
                CheckRow(it2.text, it2.done, { save(hoy.map { x -> if (x.id == it2.id) x.copy(done = !x.done) else x }) }) {
                    if (!it2.done) TextButton(onClick = {
                        nav.start(it2.text, null, when (slot) { "g" -> 45; "m" -> 20; else -> 5 })
                    }) { Text("Empezar", color = C.Bosque) }
                    DeleteButton { save(hoy.filter { x -> x.id != it2.id }) }
                }
            }
            if (enSlot.size < max) AddField("Agregar") { t -> save(hoy + Item(Goal.newId(), t, slot, day = today)) }
        }
    }
    Small("Si querés sumar algo, primero sacá otra cosa del mismo tamaño. El límite es la gracia.")
}

// ---------------------------------------------------------------------------------------------
// Regla de los 2 minutos
// ---------------------------------------------------------------------------------------------

@Composable
private fun DosMinutos(nav: Nav) {
    Store.tick.intValue
    val items = Store.items("dosmin")
    Small("Anotá cosas chicas que venís pateando. Cada una: 2 minutos y listo. Ojo con usarlas para escapar de la importante.")
    AddField("Algo de 2 minutos") { Store.saveItems("dosmin", items + Item(Goal.newId(), it, day = Store.today())) }
    Progress(items.count { it.done }, items.size)
    items.forEach { it2 ->
        CheckRow(it2.text, it2.done, { Store.saveItems("dosmin", items.map { x -> if (x.id == it2.id) x.copy(done = !x.done) else x }) }) {
            if (!it2.done) TextButton(onClick = {
                Store.saveItems("dosmin", items.map { x -> if (x.id == it2.id) x.copy(done = true) else x })
                nav.start(it2.text, null, 2)
            }) { Text("Ya", color = C.Bosque) }
            DeleteButton { Store.saveItems("dosmin", items.filter { x -> x.id != it2.id }) }
        }
    }
    if (items.any { it.done }) TextButton(onClick = { Store.saveItems("dosmin", items.filter { !it.done }) }) {
        Text("Limpiar las hechas", color = C.Texto2)
    }
}

// ---------------------------------------------------------------------------------------------
// Vaciar la mente: capturar y procesar de a una
// ---------------------------------------------------------------------------------------------

@Composable
private fun VaciarMente(nav: Nav) {
    Store.tick.intValue
    val bandeja = Store.items("bandeja")
    var procesando by remember { mutableStateOf(false) }
    val algunDia = Store.items("algundia")

    if (!procesando) {
        SerifText("1. Sacalo de la cabeza", 20)
        Small("Anotá todo lo que te da vueltas, sin ordenar ni pensar mucho. Una cosa por renglón.")
        AddField("Algo pendiente") { Store.saveItems("bandeja", bandeja + Item(Goal.newId(), it, day = Store.today())) }
        bandeja.forEach { it2 ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body("· ${it2.text}", Modifier.weight(1f), size = 15)
                DeleteButton { Store.saveItems("bandeja", bandeja.filter { x -> x.id != it2.id }) }
            }
        }
        if (bandeja.isNotEmpty()) {
            SerifText("2. Decidí qué hacer con cada cosa", 20)
            PrimaryButton("Procesar ${bandeja.size} ${if (bandeja.size == 1) "cosa" else "cosas"}") { procesando = true }
        }
    } else {
        val actual = bandeja.firstOrNull()
        if (actual == null) {
            TCard(bg = C.SalviaClara, bordered = false) {
                SerifText("Bandeja vacía.", 22)
                Small("Todo tiene un lugar. Tu cabeza puede descansar.")
            }
            PrimaryButton("Volver") { procesando = false }
        } else {
            Small("Quedan ${bandeja.size}")
            TCard(bg = C.Papel) {
                SerifText(actual.text, 22)
                Spacer(Modifier.height(8.dp))
                Small("¿Qué hacés con esto?")
            }
            fun quitar() = Store.saveItems("bandeja", bandeja.drop(1))
            OptionCard("Lleva menos de 2 minutos", "Hacelo ya") {
                quitar(); nav.start(actual.text, null, 2)
            }
            OptionCard("Es para hoy", "Va a tu lista de hoy (Ivy Lee)") {
                val ivy = Store.items("ivy")
                Store.saveItems("ivy", ivy + Item(Goal.newId(), actual.text, day = Store.today(), order = ivy.count { it.day == Store.today() }))
                quitar()
            }
            OptionCard("Es grande, tiene pasos", "Dividilo en pasos") {
                Store.saveItems("pasos_tarea", listOf(Item("t", actual.text)))
                quitar(); nav.push(Route.Tool("PASOS"))
            }
            OptionCard("Tiene fecha", "Ponele una fecha límite") {
                Store.saveItems("plazos", Store.items("plazos") + Item(Goal.newId(), actual.text, day = Store.today(), due = Store.today() + 7))
                quitar(); nav.push(Route.Tool("PLAZO"))
            }
            OptionCard("Para más adelante", "Al tablero, en «Por hacer»") {
                Store.saveTasks(Store.tasks() + KTask(Goal.newId(), actual.text, 0)); quitar()
            }
            OptionCard("Algún día, quizás", "Lo guardo sin presión") {
                Store.saveItems("algundia", algunDia + actual); quitar()
            }
            TextButton(onClick = { quitar() }) { Text("Ya no importa: borrar", color = C.ArcillaOscura) }
        }
    }
    if (!procesando && algunDia.isNotEmpty()) {
        SectionLabel("Algún día")
        algunDia.forEach { it2 ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body(it2.text, Modifier.weight(1f), size = 15, color = C.Texto2)
                TextButton(onClick = {
                    Store.saveItems("bandeja", Store.items("bandeja") + it2)
                    Store.saveItems("algundia", algunDia.filter { x -> x.id != it2.id })
                }) { Text("Retomar", color = C.Bosque) }
            }
        }
    }
}

@Composable
private fun OptionCard(title: String, subtitle: String, onClick: () -> Unit) {
    TCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Body(title, bold = true)
                Small(subtitle)
            }
            Chevron()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dividir en pasos
// ---------------------------------------------------------------------------------------------

@Composable
private fun DividirPasos(nav: Nav) {
    Store.tick.intValue
    val tarea = Store.items("pasos_tarea").firstOrNull()?.text ?: ""
    var grande by remember(tarea) { mutableStateOf(tarea) }
    val pasos = Store.items("pasos")
    val preguntas = listOf(
        "¿Cuál es la primera acción física? (abrir, escribir, llamar, buscar…)",
        "¿Y después de eso?",
        "¿Qué más hace falta?",
        "¿Hay algo que necesites antes de empezar?"
    )
    OutlinedTextField(
        value = grande, onValueChange = { grande = it; Store.saveItems("pasos_tarea", listOf(Item("t", it))) },
        label = { Text("La tarea grande") }, modifier = Modifier.fillMaxWidth()
    )
    if (grande.isNotBlank()) {
        TCard(bg = C.SalviaClara, bordered = false) {
            Body(preguntas[pasos.size.coerceAtMost(preguntas.lastIndex)], bold = true)
            Small("Que dure menos de 10 minutos. Si dura más, partilo de nuevo.")
        }
        AddField("Paso ${pasos.size + 1}") { Store.saveItems("pasos", pasos + Item(Goal.newId(), it, order = pasos.size)) }
        pasos.forEachIndexed { i, it2 ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body("${i + 1}. ${it2.text}", Modifier.weight(1f))
                DeleteButton { Store.saveItems("pasos", pasos.filter { x -> x.id != it2.id }) }
            }
        }
        if (pasos.isNotEmpty()) {
            PrimaryButton("Empezar el paso 1 (10 min)") { nav.start(pasos.first().text, null, 10) }
            SoftButton("Mandar los pasos al tablero", Modifier.fillMaxWidth()) {
                Store.saveTasks(Store.tasks() + pasos.map { KTask(Goal.newId(), it.text, 0) })
                Store.saveItems("pasos", emptyList()); Store.saveItems("pasos_tarea", emptyList())
                nav.push(Route.Kanban)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bloques de tiempo
// ---------------------------------------------------------------------------------------------

@Composable
private fun Bloques(nav: Nav) {
    val ctx = LocalContext.current
    Store.tick.intValue
    val today = Store.today()
    val todas = Store.items("bloques")
    val hoy = todas.filter { it.day == today }.sortedBy { if (it.time < 0) 9999 else it.time }
    fun save(n: List<Item>) = Store.saveItems("bloques", todas.filter { it.day != today } + n)
    fun programar(b: Item) {
        if (b.time >= 0) Avisos.at(ctx, "bloque_${b.id}", millisOf(b.day, b.time), "Empieza: ${b.text}", "Tu bloque de ${b.minutes} minutos. Tocá para empezar.")
    }
    fun pedirHora(b: Item) {
        val ahora = Calendar.getInstance()
        TimePickerDialog(ctx, { _, h, m ->
            val nuevo = b.copy(time = h * 60 + m)
            save(hoy.map { if (it.id == b.id) nuevo else it }); programar(nuevo)
        }, if (b.time >= 0) b.time / 60 else ahora.get(Calendar.HOUR_OF_DAY) + 1, if (b.time >= 0) b.time % 60 else 0, true).show()
    }
    val totalMin = hoy.sumOf { it.minutes }
    Small("Elegí pocas cosas y dales hora. Sumá un margen: casi todo lleva más de lo que creemos.")
    AddField("¿Qué vas a hacer?") { t ->
        val b = Item(Goal.newId(), t, day = today, minutes = 45)
        save(hoy + b); pedirHora(b)
    }
    if (hoy.isNotEmpty()) Small("Hoy: ${totalMin / 60} h ${totalMin % 60} min planificados.")
    hoy.forEach { b ->
        TCard(bg = if (b.done) C.SalviaClara else C.Papel) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Body(b.text, bold = true, color = if (b.done) C.Texto2 else C.Bosque)
                    Small(if (b.time >= 0) "${hhmm(b.time)} a ${hhmm((b.time + b.minutes) % 1440)}" else "Sin hora")
                }
                DeleteButton { Avisos.cancel(ctx, "bloque_${b.id}"); save(hoy.filter { it.id != b.id }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(25, 45, 90).forEach { m ->
                    Choice("$m min", b.minutes == m) { save(hoy.map { if (it.id == b.id) it.copy(minutes = m) else it }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                SoftButton(if (b.time >= 0) "Cambiar hora" else "Darle hora") { pedirHora(b) }
                if (!b.done) SoftButton("Empezar") {
                    save(hoy.map { if (it.id == b.id) it.copy(done = true) else it }); nav.start(b.text, null, b.minutes)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Perdonarte y volver
// ---------------------------------------------------------------------------------------------

@Composable
private fun Perdon(nav: Nav) {
    var paso by remember { mutableIntStateOf(0) }
    var que by remember { mutableStateOf("") }
    var amable by remember { mutableStateOf("") }
    var chiquito by remember { mutableStateOf("") }
    val frases = listOf(
        "Le pasa a todo el mundo. No me define.",
        "Estaba cansado. Hoy puedo hacer un poco.",
        "Me lo perdono y vuelvo a intentarlo.",
        "Postergar fue una forma de evitar algo que me pesaba."
    )
    Progress(paso, 3)
    when (paso) {
        0 -> {
            SerifText("¿Qué pasó?", 22)
            Small("Contalo como lo contaría un testigo: sin adjetivos, sin castigarte.")
            OutlinedTextField(que, { que = it }, label = { Text("Por ejemplo: no estudié el martes") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton("Siguiente") { if (que.isNotBlank()) paso = 1 }
        }
        1 -> {
            SerifText("Hablate como a un amigo", 22)
            Small("¿Qué le dirías a alguien que querés si te contara esto? Elegí una frase o escribí la tuya.")
            frases.forEach { f -> Choice(f, amable == f, Modifier.fillMaxWidth()) { amable = f } }
            OutlinedTextField(amable, { amable = it }, label = { Text("Tu frase") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton("Siguiente") { if (amable.isNotBlank()) paso = 2 }
        }
        2 -> {
            SerifText("Un paso chiquito para volver", 22)
            Small("Algo que puedas hacer en 2 minutos, ahora.")
            OutlinedTextField(chiquito, { chiquito = it }, label = { Text("Por ejemplo: abrir el cuaderno") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton("Empezar 2 minutos") {
                if (chiquito.isNotBlank()) {
                    Store.addNote("perdon", listOf("Qué pasó" to que, "Me digo" to amable, "Paso" to chiquito))
                    paso = 3
                    nav.start(chiquito, null, 2)
                }
            }
        }
        else -> {
            TCard(bg = C.SalviaClara, bordered = false) {
                SerifText("Ya volviste.", 22)
                Small("Eso es lo que importa. Lo de antes ya pasó.")
            }
            SoftButton("Hacerlo de nuevo") { paso = 0; que = ""; amable = ""; chiquito = "" }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tu propia fecha límite
// ---------------------------------------------------------------------------------------------

@Composable
private fun Plazos(nav: Nav) {
    val ctx = LocalContext.current
    Store.tick.intValue
    val plazos = Store.items("plazos").sortedBy { it.due }
    val today = Store.today()
    val fmt = DateTimeFormatter.ofPattern("d/M")
    fun programar(p: Item) {
        Avisos.cancel(ctx, "plazo_a_${p.id}"); Avisos.cancel(ctx, "plazo_b_${p.id}")
        if (p.done || p.due < 0) return
        Avisos.at(ctx, "plazo_a_${p.id}", millisOf(p.due - 1, 19 * 60), "Mañana vence: ${p.text}", "¿Qué paso chico podés hacer hoy?")
        Avisos.at(ctx, "plazo_b_${p.id}", millisOf(p.due, 9 * 60), "Hoy vence: ${p.text}", "Tocá para empezar.")
    }
    fun elegirFecha(p: Item) {
        val d = LocalDate.ofEpochDay(if (p.due >= 0) p.due else today + 7)
        DatePickerDialog(ctx, { _, y, mo, dd ->
            val nuevo = p.copy(due = LocalDate.of(y, mo + 1, dd).toEpochDay())
            Store.saveItems("plazos", Store.items("plazos").map { if (it.id == p.id) nuevo else it }); programar(nuevo)
        }, d.year, d.monthValue - 1, d.dayOfMonth).show()
    }
    Small("Te aviso el día anterior a la tarde y el mismo día a la mañana.")
    AddField("Algo importante sin fecha") { t ->
        val p = Item(Goal.newId(), t, day = today, due = today + 7)
        Store.saveItems("plazos", Store.items("plazos") + p); programar(p); elegirFecha(p)
    }
    plazos.forEach { p ->
        val faltan = p.due - today
        TCard(bg = when { p.done -> C.SalviaClara; faltan <= 1 -> C.ArcillaClara; else -> C.Papel }) {
            CheckRow(p.text, p.done, {
                val nuevo = p.copy(done = !p.done)
                Store.saveItems("plazos", Store.items("plazos").map { if (it.id == p.id) nuevo else it }); programar(nuevo)
            }) { DeleteButton { Avisos.cancel(ctx, "plazo_a_${p.id}"); Avisos.cancel(ctx, "plazo_b_${p.id}"); Store.saveItems("plazos", Store.items("plazos").filter { it.id != p.id }) } }
            if (!p.done) {
                Small(
                    when {
                        faltan < 0 -> "Venció el ${LocalDate.ofEpochDay(p.due).format(fmt)}. ¿Le ponemos otra fecha?"
                        faltan == 0L -> "Vence hoy"
                        faltan == 1L -> "Vence mañana"
                        else -> "Faltan $faltan días (${LocalDate.ofEpochDay(p.due).format(fmt)})"
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    SoftButton("Cambiar fecha") { elegirFecha(p) }
                    SoftButton("10 min ahora") { nav.start(p.text, null, 10) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Cierre del día
// ---------------------------------------------------------------------------------------------

@Composable
private fun CierreDia(nav: Nav) {
    Store.tick.intValue
    var paso by remember { mutableIntStateOf(0) }
    val today = Store.today()
    val sesiones = Store.sessions().filter { it.day == today }
    val ivyHoy = Store.items("ivy").filter { it.day == today && it.done }
    Progress(paso, 4)
    when (paso) {
        0 -> {
            SerifText("1. Lo que hiciste hoy", 22)
            if (sesiones.isEmpty() && ivyHoy.isEmpty()) Small("Hoy no registraste actividades. Igual cuenta lo que hiciste fuera de la app.")
            sesiones.forEach { Body("✓ ${it.label} · ${it.minutes} min", size = 15) }
            ivyHoy.forEach { Body("✓ ${it.text}", size = 15) }
            PrimaryButton("Siguiente") { paso = 1 }
        }
        1 -> {
            SerifText("2. Lo que quedó dando vueltas", 22)
            Small("Anotalo para no llevártelo a la cama. Mañana lo procesás en «Vaciar la mente».")
            val bandeja = Store.items("bandeja")
            AddField("Algo pendiente") { Store.saveItems("bandeja", bandeja + Item(Goal.newId(), it, day = today)) }
            bandeja.takeLast(6).forEach { Body("· ${it.text}", size = 15) }
            PrimaryButton("Siguiente") { paso = 2 }
        }
        2 -> {
            SerifText("3. Las 3 primeras de mañana", 22)
            Small("Cuando arranques, ya vas a saber qué hacer.")
            val ivy = Store.items("ivy")
            val manana = ivy.filter { it.day == today + 1 }
            if (manana.size < 3) AddField("Tarea ${manana.size + 1}") { t ->
                Store.saveItems("ivy", ivy + Item(Goal.newId(), t, day = today + 1, order = manana.size))
            }
            manana.sortedBy { it.order }.forEachIndexed { i, x -> Body("${i + 1}. ${x.text}") }
            PrimaryButton("Siguiente") { paso = 3 }
        }
        else -> {
            TCard(bg = C.Bosque, bordered = false) {
                SerifText("Listo por hoy.", 28, color = C.Lino)
                Small("Lo pendiente tiene lugar. Ya podés soltar.", color = C.Arena)
            }
            PrimaryButton("Cerrar") {
                Store.addNote("cierre", listOf("Hecho" to "${sesiones.size + ivyHoy.size} cosas"))
                nav.pop()
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Placer asociado
// ---------------------------------------------------------------------------------------------

@Composable
private fun PlacerAsociado(nav: Nav) {
    Store.tick.intValue
    val goals = Store.goals()
    Small("Elegí algo que te guste mucho (un podcast, una serie, una playlist) y disfrutalo solo mientras hacés tu objetivo. Tranqui te lo recuerda al empezar.")
    if (goals.isEmpty()) {
        PrimaryButton("Crear un objetivo") { nav.push(Route.GoalEdit(null)) }
    }
    goals.forEach { g ->
        TCard(onClick = { nav.push(Route.GoalEdit(g.id)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Body(g.name, bold = true)
                    Small(if (g.pleasure.isBlank()) "Sin placer asociado: tocá para elegir uno" else "Solo mientras: ${g.pleasure}")
                }
                Chevron()
            }
        }
    }
}

@Suppress("unused")
private val sinUso = Color.Transparent
