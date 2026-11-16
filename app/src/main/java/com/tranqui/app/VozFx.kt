package com.tranqui.app

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import kotlin.math.pow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

/**
 * Cómo suena una voz: tono (semitonos, 0 = como se grabó), eco suave, volumen extra y limpieza
 * de graves (saca el retumbe y el ruido grave del ambiente). Se guarda por voz.
 */
data class AjusteVoz(val tono: Float = 0f, val eco: Float = 0.25f, val volumenDb: Int = 4, val limpia: Boolean = true) {
    fun texto() = "t=$tono;e=$eco;v=$volumenDb;l=${if (limpia) 1 else 0}"

    companion object {
        fun de(s: String?): AjusteVoz {
            if (s.isNullOrBlank()) return AjusteVoz()
            val m = s.split(";").mapNotNull { p -> p.split("=").takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
            return AjusteVoz(
                m["t"]?.toFloatOrNull() ?: 0f, m["e"]?.toFloatOrNull() ?: 0.25f,
                m["v"]?.toIntOrNull() ?: 4, m["l"] != "0"
            )
        }

        fun de(vozId: String): AjusteVoz = de(Store.items("vozaj").firstOrNull { it.id == vozId }?.text)

        fun guardar(vozId: String, a: AjusteVoz) =
            Store.saveItems("vozaj", Store.items("vozaj").filter { it.id != vozId } + Item(vozId, a.texto()))
    }
}

/**
 * Reproduce las frases de una voz con su ajuste. El eco son dos copias de la misma frase,
 * un poco más tarde y más bajas (110 y 240 ms): suena a sala amplia sin perder claridad.
 * Funciona en cualquier celular, sin depender de efectos del fabricante.
 */
class VozFx(ctx: Context, private val vozId: String, private val ajuste: AjusteVoz = AjusteVoz.de(vozId)) {
    private val app = ctx.applicationContext
    private val dir = File(app.filesDir, "voces/$vozId")
    private val main = ExoPlayer.Builder(app).build()
    private val ecos = listOf(110L to 0.42f, 240L to 0.2f).map { (ms, g) -> Triple(ExoPlayer.Builder(app).build(), ms, g) }
    private val handler = Handler(Looper.getMainLooper())
    private val efectos = mutableListOf<android.media.audiofx.AudioEffect>()

    init {
        val tono = PlaybackParameters(1f, 2f.pow(ajuste.tono / 12f))
        (listOf(main) + ecos.map { it.first }).forEach { p ->
            p.playbackParameters = tono
            runCatching {
                if (ajuste.volumenDb > 0) efectos += LoudnessEnhancer(p.audioSessionId).apply {
                    setTargetGain(ajuste.volumenDb * 100); enabled = true
                }
            }
            runCatching {
                if (ajuste.limpia) efectos += Equalizer(0, p.audioSessionId).apply {
                    // La banda más grave (≈60 Hz) al mínimo: saca retumbe y ruido de fondo grave.
                    setBandLevel(0, bandLevelRange[0]); enabled = true
                }
            }
        }
        ecos.forEach { (p, _, g) -> p.volume = (ajuste.eco * g).coerceIn(0f, 1f) }
    }

    fun archivo(text: String): File? = File(dir, voiceKey(text) + ".m4a").takeIf { it.exists() }

    fun hablando(): Boolean = main.isPlaying || main.playbackState == Player.STATE_BUFFERING

    /** Devuelve true si hay grabación para esa frase. */
    fun decir(text: String): Boolean = archivo(text)?.let { decirArchivo(it); true } ?: false

    fun decirArchivo(f: File) {
        val item = MediaItem.fromUri(Uri.fromFile(f))
        handler.removeCallbacksAndMessages(null)
        main.setMediaItem(item); main.prepare(); main.play()
        if (ajuste.eco > 0.01f) ecos.forEach { (p, ms, _) ->
            p.setMediaItem(item); p.prepare()
            handler.postDelayed({ p.play() }, ms)
        }
    }

    fun parar() {
        handler.removeCallbacksAndMessages(null)
        main.stop(); ecos.forEach { it.first.stop() }
    }

    fun release() {
        parar()
        efectos.forEach { runCatching { it.release() } }
        main.release(); ecos.forEach { it.first.release() }
    }
}

/** «Cómo suena»: ajustar tono, eco, volumen y limpieza de una voz, y probarlo con una frase grabada. */
@androidx.compose.runtime.Composable
fun VozAjusteScreen(vozId: String, nav: Nav) {
    androidx.compose.runtime.key(vozId) { VozAjusteContenido(vozId, nav) }
}

@androidx.compose.runtime.Composable
private fun VozAjusteContenido(vozId: String, nav: Nav) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var a by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(AjusteVoz.de(vozId)) }
    var fx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<VozFx?>(null) }
    var fondoP by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ExoPlayer?>(null) }
    var conFondo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Store.practiceMusic.isNotEmpty()) }
    val frases = androidx.compose.runtime.remember(vozId) {
        File(ctx.filesDir, "voces/$vozId").listFiles()?.filter { it.extension == "m4a" }?.sortedBy { it.lastModified() } ?: emptyList()
    }
    var cual by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    fun pararTodo() { fx?.release(); fx = null; fondoP?.release(); fondoP = null }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { pararTodo() } }
    fun cambiar(n: AjusteVoz) { a = n; AjusteVoz.guardar(vozId, n) }
    fun probar() {
        if (frases.isEmpty()) return
        pararTodo()
        if (conFondo && Store.practiceMusic.isNotEmpty()) {
            fondoP = ExoPlayer.Builder(ctx).build().apply {
                setMediaItem(MediaItem.fromUri("asset:///soundscape/sounds/${Store.practiceMusic}.ogg"))
                repeatMode = Player.REPEAT_MODE_ONE; volume = Store.fondoVolumen * 0.4f; prepare(); play()
            }
        }
        val f = frases[cual % frases.size]; cual++
        fx = VozFx(ctx, vozId, a).also { v ->
            // VozFx busca por texto; acá se prueba directo con el archivo.
            v.decirArchivo(f)
        }
    }

    androidx.compose.foundation.layout.Column(
        androidx.compose.ui.Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        BackBar("Cómo suena: ${Voces.name(vozId)}") { nav.pop() }
        Small("No se modifica la grabación: el ajuste se aplica al escuchar, así podés cambiarlo cuando quieras.")
        Ajuste("Tono", if (a.tono == 0f) "Como se grabó" else "%.1f semitonos más grave".format(-a.tono), a.tono, -4f..0f, 7) { cambiar(a.copy(tono = it)) }
        Ajuste("Eco", when { a.eco < 0.05f -> "Sin eco"; a.eco < 0.35f -> "Suave"; a.eco < 0.7f -> "Medio"; else -> "Amplio" }, a.eco, 0f..1f, 0) { cambiar(a.copy(eco = it)) }
        Ajuste("Volumen de la voz", if (a.volumenDb == 0) "Normal" else "+${a.volumenDb} dB", a.volumenDb.toFloat(), 0f..12f, 11) { cambiar(a.copy(volumenDb = it.toInt())) }
        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.weight(1f)) {
                Body("Limpiar graves")
                Small("Saca el retumbe y el ruido grave del ambiente.")
            }
            androidx.compose.material3.Switch(checked = a.limpia, onCheckedChange = { cambiar(a.copy(limpia = it)) },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = C.Bosque))
        }
        if (Store.practiceMusic.isNotEmpty()) androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Body("Probar con el fondo elegido", androidx.compose.ui.Modifier.weight(1f))
            androidx.compose.material3.Switch(checked = conFondo, onCheckedChange = { conFondo = it },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = C.Bosque))
        }
        if (frases.isEmpty()) Small("Grabá al menos una frase de alguna práctica para poder probar.")
        else {
            PrimaryButton(if (fx == null) "Probar con una frase" else "Probar otra frase") { probar() }
            if (fx != null) SoftButton("Detener", androidx.compose.ui.Modifier.fillMaxWidth()) { pararTodo() }
        }
        androidx.compose.material3.TextButton(onClick = { cambiar(AjusteVoz()) }) { androidx.compose.material3.Text("Volver a lo recomendado", color = C.Texto2) }
    }
}

@androidx.compose.runtime.Composable
private fun Ajuste(titulo: String, valor: String, v: Float, rango: ClosedFloatingPointRange<Float>, pasos: Int, onChange: (Float) -> Unit) {
    var x by androidx.compose.runtime.remember(v) { androidx.compose.runtime.mutableFloatStateOf(v) }
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Row {
            Body(titulo, androidx.compose.ui.Modifier.weight(1f), bold = true)
            Small(valor, color = C.Musgo)
        }
        androidx.compose.material3.Slider(
            value = x, onValueChange = { x = it }, onValueChangeFinished = { onChange(x) },
            valueRange = rango, steps = pasos,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = C.Bosque, activeTrackColor = C.Musgo)
        )
    }
}
