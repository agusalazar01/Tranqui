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
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Todas las frases que dice la voz en las prácticas. «…» separa dos frases que se dicen con una pausa en el medio.
 */
fun voicePhrases(): List<String> {
    val textos = mutableListOf(VOZ_INICIO_RESPIRAR, VOZ_INICIO_GUIA, VOZ_SEGUI, VOZ_CIERRE)
    PRACTICES.forEach { p -> p.allPhases.mapNotNullTo(textos) { it.say } }
    PRACTICES.forEach { p -> textos += p.steps; if (p.cierre.isNotEmpty()) textos += p.cierre }
    textos += hipnoFrases()
    return textos.flatMap { t -> t.split("…").map { it.trim() } }.filter { it.isNotEmpty() }.distinct()
}

fun myVoiceDir(ctx: Context): File = File(ctx.filesDir, "mivoz").apply { mkdirs() }

fun myVoiceCount(ctx: Context): Int {
    val keys = voicePhrases().map { voiceKey(it) }.toSet()
    return myVoiceDir(ctx).listFiles()?.count { it.extension == "m4a" && it.nameWithoutExtension in keys } ?: 0
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
fun VoiceRecordScreen(nav: Nav) {
    val ctx = LocalContext.current
    val phrases = remember { voicePhrases() }
    val dir = remember { myVoiceDir(ctx) }
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
    var exportName by remember { mutableStateOf("agustin") }
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
        BackBar("Grabar mi voz") { nav.pop() }
        Small(
            "Grabá cada frase con tu voz y las prácticas te van a guiar con ella. " +
                "Buscá un lugar callado, sostené el celular a un palmo de la boca y hablá lento y suave, como si acompañaras a alguien."
        )
        val count = done.count { it }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(C.SalviaClara)) {
            Box(Modifier.fillMaxWidth(count / phrases.size.toFloat()).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(C.Musgo))
        }
        Small("$count de ${phrases.size} frases grabadas")

        if (count == phrases.size) {
            TCard(bg = C.SalviaClara, bordered = false) {
                SerifText("¡Listo! Grabaste todas las frases.", 20)
                Small("Elegí «Mi voz» en cualquier meditación. Podés volver acá para escuchar o regrabar.")
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

        SectionLabel("Todas las frases")
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

        SectionLabel("Incluirla en la app para todos")
        Small(
            "Exportá tus grabaciones como ZIP y subilo a la carpeta «voces» del repositorio de Tranqui en GitHub " +
                "(Add file → Upload files). Al compilar se limpian los silencios y el ruido, y la voz queda dentro de la app."
        )
        OutlinedTextField(
            value = exportName, onValueChange = { exportName = it.filter { c -> c.isLetterOrDigit() }.take(20) },
            label = { Text("Nombre de la voz") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        PrimaryButton("Exportar ZIP") {
            val zip = exportVoice(ctx, exportName.ifBlank { "mivoz" })
            if (zip == null) error = "Todavía no grabaste ninguna frase." else shareFile(ctx, zip)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Arma un ZIP con las frases grabadas y un texto que dice qué frase es cada archivo. */
fun exportVoice(ctx: Context, name: String): File? {
    val files = myVoiceDir(ctx).listFiles()?.filter { it.extension == "m4a" } ?: return null
    if (files.isEmpty()) return null
    val out = File(ctx.cacheDir, "$name.zip")
    ZipOutputStream(out.outputStream()).use { z ->
        files.forEach { f ->
            z.putNextEntry(ZipEntry(f.name)); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
        }
        z.putNextEntry(ZipEntry("frases.txt"))
        z.write(voicePhrases().joinToString("\n") { "${voiceKey(it)}  $it" }.toByteArray())
        z.closeEntry()
    }
    return out
}

fun shareFile(ctx: Context, file: File) {
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".archivos", file)
    val send = Intent(Intent.ACTION_SEND).setType("application/zip")
        .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, "Exportar mi voz").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
