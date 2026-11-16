package com.tranqui.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File

/** Las frases que dice la voz en UNA práctica. «…» separa dos frases con una pausa en el medio. */
fun practicePhrases(p: Practice): List<String> {
    val textos = mutableListOf<String>()
    if (p.allPhases.isNotEmpty()) {
        textos += VOZ_INICIO_RESPIRAR
        p.allPhases.mapNotNullTo(textos) { it.say }
        textos += VOZ_SEGUI
    } else {
        textos += VOZ_INICIO_GUIA
        textos += p.steps
    }
    textos += p.cierre.ifEmpty { VOZ_CIERRE }
    return textos.flatMap { t -> t.split("…").map { it.trim() } }.filter { it.isNotEmpty() }.distinct()
}

/**
 * Voces grabadas en este celular. Cada voz es una carpeta (filesDir/voces/<id>) con una grabación
 * por frase; las frases que se repiten entre prácticas («Inhalá», el cierre…) se graban una sola vez por voz.
 */
object Voces {
    fun list(): List<Item> = Store.items("voces")

    fun dir(ctx: Context, id: String): File = File(ctx.filesDir, "voces/$id").apply { mkdirs() }

    fun name(id: String): String = list().firstOrNull { it.id == id }?.text ?: "Voz"

    fun add(ctx: Context, nombre: String): String {
        val id = Goal.newId()
        Store.saveItems("voces", list() + Item(id, nombre.trim().ifBlank { "Mi voz" }))
        dir(ctx, id)
        return id
    }

    fun delete(ctx: Context, id: String) {
        File(ctx.filesDir, "voces/$id").deleteRecursively()
        Store.saveItems("voces", list().filter { it.id != id })
        if (Store.practiceVoice == id) Store.practiceVoice = ""
    }

    fun count(ctx: Context, id: String, phrases: List<String>): Int {
        val files = File(ctx.filesDir, "voces/$id").list()?.toSet() ?: return 0
        return phrases.count { voiceKey(it) + ".m4a" in files }
    }

    fun total(ctx: Context, id: String): Int = File(ctx.filesDir, "voces/$id").list()?.count { it.endsWith(".m4a") } ?: 0

    /** La voz elegida si sigue existiendo; si no hay ninguna, crea «Mi voz». */
    fun elegidaOCrear(ctx: Context): String {
        val l = list()
        return l.firstOrNull { it.id == Store.practiceVoice }?.id ?: l.firstOrNull()?.id ?: add(ctx, "Mi voz")
    }

    /** Las grabaciones de versiones anteriores (filesDir/mivoz) pasan a ser la voz «Mi voz». */
    fun migrar(ctx: Context) {
        val viejo = File(ctx.filesDir, "mivoz")
        if (!viejo.isDirectory || viejo.list().isNullOrEmpty()) return
        val id = add(ctx, "Mi voz")
        viejo.listFiles()?.forEach { it.renameTo(File(dir(ctx, id), it.name)) }
        viejo.deleteRecursively()
        if (Store.practiceVoice == "mia" || Store.practiceVoice.isEmpty()) Store.practiceVoice = id
    }
}

/** Graba frase por frase con el micrófono del teléfono. */
private class Grabadora(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    private var player: MediaPlayer? = null

    fun start(file: File) {
        stopPlayback()
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(48_000)
        r.setAudioEncodingBitRate(160_000)
        r.setAudioChannels(1)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        rec = r
    }

    /** Devuelve false si la grabación quedó vacía (por ejemplo, si se tocó dos veces muy rápido). */
    fun stop(): Boolean {
        val r = rec ?: return false
        rec = null
        return try {
            r.stop(); true
        } catch (_: RuntimeException) {
            false
        } finally {
            r.release()
        }
    }

    fun play(file: File, onEnd: () -> Unit) {
        stopPlayback()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { onEnd() }
            prepare()
            start()
        }
    }

    fun stopPlayback() {
        player?.release()
        player = null
    }

    fun release() {
        stop()
        stopPlayback()
    }
}

@Composable
fun VoiceRecordScreen(practiceId: String, vozId: String, nav: Nav) {
    val ctx = LocalContext.current
    val practica = remember(practiceId) { practiceById(practiceId) }
    val phrases = remember(practiceId) { practicePhrases(practica) }
    val dir = remember(vozId) { Voces.dir(ctx, vozId) }
    val grab = remember { Grabadora(ctx) }
    DisposableEffect(Unit) { onDispose { grab.release() } }

    var version by remember { mutableIntStateOf(0) } // cambia cuando se graba algo
    fun fileOf(text: String) = File(dir, voiceKey(text) + ".m4a")
    val done = remember(version) { phrases.map { fileOf(it).exists() } }
    var index by remember { mutableIntStateOf(done.indexOfFirst { !it }.coerceAtLeast(0)) }
    var recording by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var autoNext by remember { mutableStateOf(true) }
    var advance by remember { mutableIntStateOf(0) }
    // Después de grabar, pasa sola a la próxima frase que falta (si está activado).
    LaunchedEffect(advance) {
        if (advance == 0) return@LaunchedEffect
        delay(1300)
        val next = phrases.indices.firstOrNull { it > index && !fileOf(phrases[it]).exists() }
            ?: phrases.indices.firstOrNull { !fileOf(phrases[it]).exists() }
        if (next != null && !recording) index = next
    }
    var hasMic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }

    LaunchedEffect(recording) {
        seconds = 0
        while (recording) {
            delay(1000); seconds++
        }
    }

    val text = phrases[index]
    val file = fileOf(text)

    fun toggleRecord() {
        error = null
        if (!hasMic) { askMic.launch(Manifest.permission.RECORD_AUDIO); return }
        if (!recording) {
            try {
                grab.start(file); recording = true
            } catch (e: Exception) {
                error = "No se pudo usar el micrófono."
            }
        } else {
            recording = false
            if (!grab.stop()) {
                file.delete(); error = "La grabación quedó vacía. Probá de nuevo, hablando después de tocar."
            } else if (autoNext) {
                advance++
            }
            version++
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackBar(practica.name) { nav.pop() }
        Small("Voz: ${Voces.name(vozId)}", color = C.Musgo)
        Small(
            "Grabá solo las frases de esta práctica. Buscá un lugar callado, sostené el celular a un palmo de la boca " +
                "y hablá lento y suave, como si acompañaras a alguien."
        )
        val count = done.count { it }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(C.SalviaClara)) {
            Box(Modifier.fillMaxWidth(count / phrases.size.toFloat()).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(C.Musgo))
        }
        Small("$count de ${phrases.size} frases grabadas")

        if (count == phrases.size) {
            TCard(bg = C.SalviaClara, bordered = false) {
                SerifText("¡Listo! Esta práctica ya tiene tu voz.", 20)
                Small("Podés volver acá para escuchar o regrabar cualquier frase.")
                Spacer(Modifier.height(8.dp))
                SoftButton("Volver") { nav.pop() }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Body("Pasar sola a la siguiente frase", Modifier.weight(1f), size = 15)
            Switch(checked = autoNext, onCheckedChange = { autoNext = it }, colors = SwitchDefaults.colors(checkedTrackColor = C.Bosque))
        }
        TCard(bg = C.Papel) {
            Small("Frase ${index + 1}")
            Spacer(Modifier.height(6.dp))
            SerifText("«$text»", 24)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(76.dp).clip(CircleShape)
                        .background(if (recording) C.Arcilla else C.Bosque)
                        .clickable { toggleRecord() },
                    contentAlignment = Alignment.Center
                ) {
                    if (recording) Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(C.Lino))
                    else Box(Modifier.size(26.dp).clip(CircleShape).border(3.dp, C.Lino, CircleShape))
                }
                Column(Modifier.weight(1f)) {
                    Body(
                        when {
                            recording -> "Grabando… 0:%02d".format(seconds)
                            file.exists() -> "Grabada"
                            else -> "Tocá para grabar"
                        }, bold = true
                    )
                    Small(if (recording) "Tocá de nuevo cuando termines." else "Esperá un segundo antes de hablar.")
                }
            }
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Small(error!!, color = C.ArcillaOscura)
            }
            if (file.exists() && !recording) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton(if (playing) "Detener" else "Escuchar") {
                        if (playing) { grab.stopPlayback(); playing = false }
                        else { playing = true; grab.play(file) { playing = false } }
                    }
                    SoftButton("Borrar") { file.delete(); version++ }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PrimaryButton("‹ Anterior", Modifier.weight(1f), bg = C.Lino, fg = C.Bosque) {
                if (!recording && index > 0) { grab.stopPlayback(); playing = false; index-- }
            }
            PrimaryButton("Siguiente ›", Modifier.weight(1f)) {
                if (!recording && index < phrases.lastIndex) { grab.stopPlayback(); playing = false; index++ }
            }
        }

        SectionLabel("Las frases de esta práctica")
        phrases.forEachIndexed { i, t ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (i == index) C.SalviaClara else C.Lino)
                    .clickable { if (!recording) { grab.stopPlayback(); playing = false; index = i } }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(if (done[i]) C.Bosque else C.Borde),
                    contentAlignment = Alignment.Center
                ) {
                    if (done[i]) Icon(Icons.Filled.Check, contentDescription = "Grabada", tint = C.Lino, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(10.dp))
                Body(t, size = 14)
            }
        }

        SectionLabel("Tu voz es tuya")
        Small(
            "Las grabaciones quedan solo en este celular, dentro de Tranqui. No se suben a internet, no se comparten " +
                "y no se incluyen en copias de seguridad. Si borrás la app, se borran."
        )
        Small("Si grabás la voz de otra persona, hacelo solo con su permiso.")
        Spacer(Modifier.height(16.dp))
    }
}

/** Crear una voz nueva: nombre y de quién es (si es de otra persona, con su permiso). */
@Composable
fun NuevaVozDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var deOtro by remember { mutableStateOf(false) }
    var permiso by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Lino,
        title = { SerifText("Nueva voz", 22) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it.take(24) },
                    label = { Text("Nombre") }, placeholder = { Text("Por ejemplo: Mi voz, Voz de Ana") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("Es mi voz", !deOtro) { deOtro = false }
                    Choice("De otra persona", deOtro) { deOtro = true }
                }
                if (deOtro) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = permiso, onCheckedChange = { permiso = it })
                    Small("Esa persona sabe que la grabo y me dio permiso.")
                }
                Small("Queda guardada solo en este celular.")
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(nombre) }, enabled = nombre.isNotBlank() && (!deOtro || permiso)) {
                Text("Crear", color = C.Bosque)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = C.Texto2) } }
    )
}

/** Tus voces: cuántas frases tiene cada una, y la opción de borrarlas. */
@Composable
fun VocesScreen(nav: Nav) {
    val ctx = LocalContext.current
    Store.tick.intValue
    var nueva by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf<Item?>(null) }
    val voces = Voces.list()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar("Tus voces") { nav.pop() }
        Small("Cada práctica se graba por separado, desde su pantalla: elegís la voz y tocás «Grabar esta práctica». Solo grabás las frases de esa práctica.")
        if (voces.isEmpty()) Body("Todavía no hay voces grabadas.", color = C.Texto2)
        voces.forEach { v ->
            val n = Voces.total(ctx, v.id)
            TCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Body(v.text, bold = true)
                        Small(if (n == 0) "Sin grabaciones" else "$n frases grabadas")
                    }
                    TextButton(onClick = { borrar = v }) { Text("Borrar", color = C.ArcillaOscura) }
                }
            }
        }
        SoftButton("+ Nueva voz", Modifier.fillMaxWidth()) { nueva = true }
        SectionLabel("Tu voz es tuya")
        Small(
            "Las grabaciones quedan solo en este celular, dentro de Tranqui. No se suben a internet, no se comparten " +
                "y no se incluyen en copias de seguridad. Si borrás la app, se borran."
        )
        Small("Si grabás la voz de otra persona, hacelo solo con su permiso.")
    }
    if (nueva) NuevaVozDialog({ nueva = false }) { nombre ->
        Store.practiceVoice = Voces.add(ctx, nombre); nueva = false
    }
    borrar?.let { v ->
        AlertDialog(
            onDismissRequest = { borrar = null },
            containerColor = C.Lino,
            title = { SerifText("¿Borrar «${v.text}»?", 20) },
            text = { Small("Se borran todas sus grabaciones de este celular. No se puede deshacer.") },
            confirmButton = { TextButton(onClick = { Voces.delete(ctx, v.id); borrar = null }) { Text("Borrar", color = C.ArcillaOscura) } },
            dismissButton = { TextButton(onClick = { borrar = null }) { Text("Cancelar", color = C.Texto2) } }
        )
    }
}
