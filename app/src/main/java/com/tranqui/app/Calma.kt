package com.tranqui.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.util.Locale
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CalmaScreen(nav: Nav, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenTitle("Calma", "Sonidos y prácticas para bajar un cambio")
        SectionLabel("Paisajes sonoros")
        SoundscapeView("index.html", Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)))
        SectionLabel("Prácticas guiadas")
        PRACTICES.forEach { p ->
            TCard(onClick = { nav.push(Route.PracticeRun(p.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SerifText(p.name, 19)
                        Small(p.durationLabel)
                    }
                    Tag(p.tag)
                }
            }
        }
        Small("Las prácticas son para el bienestar diario y no reemplazan un tratamiento.")
        Spacer(Modifier.height(12.dp))
    }
}

/** Puente entre la interfaz animada y el reproductor nativo persistente. */
class SoundBridge(private val ctx: Context, private val onHeight: (Int) -> Unit = {}) {
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    /** Grabaciones que están dentro de la app (sin extensión). */
    @JavascriptInterface fun available(): String =
        (ctx.assets.list("soundscape/sounds") ?: emptyArray()).filter { it.endsWith(".ogg") }
            .joinToString(",") { it.removeSuffix(".ogg") }

    /** La página avisa cuánto mide, en px de CSS (equivalen a dp). */
    @JavascriptInterface fun height(px: Int) {
        main.post { onHeight(px) }
    }

    @JavascriptInterface fun setPitch(semis: Int) {
        send(SoundPlaybackService.ACTION_PITCH) { putExtra(SoundPlaybackService.EXTRA_PITCH, semis.coerceIn(-6, 6)) }
    }
    @JavascriptInterface fun currentPitch(): Int = prefs.getInt(SoundPlaybackService.KEY_PITCH, 0)

    private val prefs get() = ctx.getSharedPreferences(SoundPlaybackService.PREFS, Context.MODE_PRIVATE)

    @JavascriptInterface fun play(soundId: String) {
        val intent = android.content.Intent(ctx, SoundPlaybackService::class.java)
            .setAction(SoundPlaybackService.ACTION_PLAY)
            .putExtra(SoundPlaybackService.EXTRA_SOUND, soundId)
        androidx.core.content.ContextCompat.startForegroundService(ctx, intent)
    }

    @JavascriptInterface fun pause() = send(SoundPlaybackService.ACTION_PAUSE)
    @JavascriptInterface fun stop() = send(SoundPlaybackService.ACTION_STOP)
    @JavascriptInterface fun select(soundId: String) {
        prefs.edit().putString(SoundPlaybackService.KEY_SELECTED, soundId).apply()
    }
    @JavascriptInterface fun setVolume(value: Float) {
        send(SoundPlaybackService.ACTION_VOLUME) { putExtra(SoundPlaybackService.EXTRA_VOLUME, value.coerceIn(0f, 1f)) }
    }
    @JavascriptInterface fun setTimer(minutes: Int) {
        send(SoundPlaybackService.ACTION_TIMER) { putExtra(SoundPlaybackService.EXTRA_MINUTES, minutes.coerceIn(0, 180)) }
    }
    @JavascriptInterface fun isPlaying(): Boolean = prefs.getBoolean(SoundPlaybackService.KEY_PLAYING, false)
    @JavascriptInterface fun currentSound(): String = prefs.getString(SoundPlaybackService.KEY_SELECTED, SoundPlaybackService.DEFAULT) ?: SoundPlaybackService.DEFAULT
    @JavascriptInterface fun currentVolume(): Float = prefs.getFloat(SoundPlaybackService.KEY_VOLUME, 0.62f)
    @JavascriptInterface fun currentTimer(): Int = prefs.getInt(SoundPlaybackService.KEY_TIMER, 0)
    @JavascriptInterface fun lastError(): String = prefs.getString(SoundPlaybackService.KEY_ERROR, "") ?: ""

    private fun send(action: String, extras: android.content.Intent.() -> Unit = {}) {
        val intent = android.content.Intent(ctx, SoundPlaybackService::class.java).setAction(action).apply(extras)
        if (action == SoundPlaybackService.ACTION_PLAY) androidx.core.content.ContextCompat.startForegroundService(ctx, intent)
        else ctx.startService(intent)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SoundscapeView(page: String, modifier: Modifier) {
    val ctx = LocalContext.current
    var pageHeight by remember { mutableIntStateOf(760) }
    val web = remember {
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
            .build()
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            setBackgroundColor(0xFFF3EFE5.toInt())
            addJavascriptInterface(SoundBridge(ctx) { h -> if (h > 200) pageHeight = h }, "TranquiApp")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    loader.shouldInterceptRequest(request.url)
            }
            loadUrl("https://appassets.androidplatform.net/assets/soundscape/$page")
        }
    }
    DisposableEffect(Unit) {
        onDispose { web.destroy() }
    }
    AndroidView(factory = { web }, modifier = modifier.height(pageHeight.dp))
}

/** Música de fondo para las prácticas: grabaciones del catálogo que acompañan sin distraer. */
private val FONDOS = listOf(
    "" to "Sin música",
    "piano-satie" to "Piano (Satie)",
    "meditacion-1" to "Meditación I",
    "meditacion-2" to "Meditación II",
    "alma-serena" to "Piano cálido",
    "tanpura" to "Tanpura",
    "campanas" to "Campanas",
    "cuenco-grave" to "Cuenco",
    "lluvia-bosque" to "Lluvia",
    "arroyo" to "Arroyo",
    "olas-suaves" to "Olas",
    "ruido-marron" to "Ruido suave"
)

/** Voces disponibles: las incluidas en la app, la grabada en este teléfono y «sin voz». */
private fun voces(ctx: Context): List<Pair<String, String>> {
    val incluidas = runCatching { ctx.assets.list("voz")?.toList() }.getOrNull().orEmpty().filter {
        runCatching { ctx.assets.list("voz/$it")?.isNotEmpty() }.getOrNull() == true
    }
    val lista = mutableListOf<Pair<String, String>>()
    incluidas.filter { it !in listOf("dora", "alex") }.forEach { lista += it to "Voz de ${it.replaceFirstChar { c -> c.uppercase() }}" }
    if (myVoiceCount(ctx) > 0) lista += "mia" to "Mi voz"
    if ("dora" in incluidas) lista += "dora" to "Voz de mujer (IA)"
    if ("alex" in incluidas) lista += "alex" to "Voz de hombre (IA)"
    if (incluidas.isEmpty()) lista += "dora" to "Voz del teléfono"
    lista += "" to "Sin voz"
    return lista
}

private fun fondos(ctx: Context): List<Pair<String, String>> {
    val hay = runCatching { ctx.assets.list("soundscape/sounds")?.toSet() }.getOrNull().orEmpty()
    return FONDOS.filter { (id, _) -> id.isEmpty() || "$id.ogg" in hay }
}

@Composable
fun PracticeScreen(id: String, nav: Nav, presetMinutes: Int? = null) {
    val p = PRACTICES.firstOrNull { it.id == id } ?: PRACTICES.first()
    var minutes by remember { mutableIntStateOf(presetMinutes?.takeIf { it in p.options } ?: p.defaultMinutes) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var vibrate by remember { mutableStateOf(true) }
    val ctx = LocalContext.current
    val vocesDisponibles = remember { voces(ctx) }
    val fondosDisponibles = remember { fondos(ctx) }
    var voz by remember { mutableStateOf(Store.practiceVoice.takeIf { v -> vocesDisponibles.any { it.first == v } } ?: vocesDisponibles.first().first) }
    var fondo by remember { mutableStateOf(Store.practiceMusic.takeIf { f -> fondosDisponibles.any { it.first == f } } ?: "") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackBar(p.name) { nav.pop() }
        when {
            running -> {
                PracticeRunner(p, minutes, vibrate, voz, fondo) {
                    Store.addSession(Session(Store.today(), null, p.name, minutes))
                    running = false
                    done = true
                }
                TextButton(onClick = { running = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Terminar antes", color = C.Texto2)
                }
            }
            done -> {
                Spacer(Modifier.height(40.dp))
                SerifText("Listo", 34, Modifier.align(Alignment.CenterHorizontally))
                Body("$minutes minutos de ${p.name.lowercase()}.", Modifier.align(Alignment.CenterHorizontally))
                PrimaryButton("Volver") { nav.pop() }
            }
            else -> {
                Tag(p.tag)
                Body(p.que)
                Small(p.evidencia)
                SectionLabel("Cómo se hace")
                p.pasos.forEachIndexed { i, s -> Body("${i + 1}. $s") }
                SectionLabel("Duración")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.options.forEach { m -> Choice("$m min", m == minutes) { minutes = m } }
                }
                SectionLabel("Voz")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vocesDisponibles.forEach { (v, nombre) -> Choice(nombre, v == voz) { voz = v; Store.practiceVoice = v } }
                }
                TextButton(onClick = { nav.push(Route.VoiceRecord) }) { Text("Grabar mi voz", color = C.Bosque) }
                SectionLabel("Música de fondo")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fondosDisponibles.forEach { (f, nombre) -> Choice(nombre, f == fondo) { fondo = f; Store.practiceMusic = f } }
                }
                Small("La música baja sola cuando habla la voz.")
                if (p.phases.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Body("Vibrar en cada cambio", Modifier.weight(1f))
                        Switch(
                            checked = vibrate, onCheckedChange = { vibrate = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = C.Bosque)
                        )
                    }
                }
                PrimaryButton("Empezar") { running = true }
            }
        }
    }
}

@Composable
private fun PracticeRunner(p: Practice, minutes: Int, vibrate: Boolean, voz: String, fondo: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    // La pantalla no se apaga durante la práctica: si se apagara, la guía se frenaría.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val audio = remember { PracticeAudio(ctx, voz, fondo) }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    val scale = remember { Animatable(0.8f) }
    var label by remember { mutableStateOf("Acomodate") }
    var remaining by remember { mutableIntStateOf(minutes * 60) }

    LaunchedEffect(Unit) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        val clock = launch {
            while (true) {
                remaining = ((end - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                delay(500)
            }
        }
        val mixer = launch { audio.run() }
        delay(1500) // la música entra sola antes de que hable nadie
        if (p.phases.isNotEmpty()) {
            audio.sayAndWait(VOZ_INICIO_RESPIRAR)
            delay(1000)
            var cycle = 0
            while (System.currentTimeMillis() < end) {
                for (ph in p.phases) {
                    label = ph.label
                    // La voz acompaña las primeras vueltas y después solo de vez en cuando:
                    // el círculo y la vibración alcanzan para seguir el ritmo.
                    val say = ph.say
                    if (say != null && (cycle < 3 || cycle % 5 == 0)) audio.say(say)
                    if (vibrate && say != null) buzz(ctx, 25)
                    scale.animateTo(ph.scale, tween((ph.seconds * 1000).toInt(), easing = FastOutSlowInEasing))
                }
                cycle++
                if (cycle == 3) audio.say(VOZ_SEGUI)
            }
        } else {
            val breathing = launch {
                while (true) {
                    scale.animateTo(1.04f, tween(6000, easing = FastOutSlowInEasing))
                    scale.animateTo(0.86f, tween(6000, easing = FastOutSlowInEasing))
                }
            }
            audio.sayAndWait(VOZ_INICIO_GUIA)
            delay(2000)
            val per = (end - System.currentTimeMillis()).coerceAtLeast(10_000L) / p.steps.size
            for (s in p.steps) {
                val stepEnd = System.currentTimeMillis() + per
                label = s
                // «…» = pausa: primero se tensa, unos segundos después la voz dice cuándo soltar.
                val parts = s.split("…").map { it.trim() }.filter { it.isNotEmpty() }
                parts.forEachIndexed { i, part ->
                    audio.sayAndWait(part)
                    if (i < parts.lastIndex) delay(5000)
                }
                delay((stepEnd - System.currentTimeMillis()).coerceAtLeast(0))
            }
            breathing.cancel()
        }
        clock.cancel()
        label = "Muy bien"
        VOZ_CIERRE.split("…").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
            audio.sayAndWait(it)
            delay(1500)
        }
        audio.fadeOut()
        mixer.cancel()
        onDone()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.size(280.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
            Mist(Modifier.fillMaxSize(), listOf(C.Salvia, C.Musgo, C.Salvia), count = 3, alpha = 0.5f, periodMs = 50000)
        }
        Spacer(Modifier.height(20.dp))
        // Las frases cambian con un fundido lento, sin saltos.
        Crossfade(targetState = label, animationSpec = tween(1400), label = "frase") { t ->
            SerifText(t, 24, Modifier.fillMaxWidth(), align = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
        Small("%d:%02d restantes".format(remaining / 60, remaining % 60))
    }
}

/**
 * Audio de las prácticas: voz y música de fondo.
 * - Voz: grabaciones generadas con Kokoro (Apache 2.0) en assets/voz/<voz>/; si no están,
 *   usa la voz del teléfono, más lenta y grave de lo normal.
 * - Música: una grabación del catálogo en bucle, que baja sola mientras habla la voz.
 * Todo se usa desde el hilo principal (ExoPlayer lo necesita así).
 */
class PracticeAudio(ctx: Context, private val voz: String, private val fondo: String) {
    private val app = ctx.applicationContext
    private fun assetSet(path: String): Set<String> = runCatching { app.assets.list(path)?.toSet() }.getOrNull() ?: emptySet()
    private val voiceFiles: Set<String> = if (voz.isEmpty() || voz == "mia") emptySet() else assetSet("voz/$voz")
    /** Si falta una frase en «Mi voz», se usa la voz incluida (primero una grabada por una persona). */
    private val fallbackVoice: String? = if (voz != "mia") null else
        assetSet("voz").firstOrNull { it !in listOf("dora", "alex") && assetSet("voz/$it").isNotEmpty() }
            ?: "dora".takeIf { assetSet("voz/dora").isNotEmpty() }
    private val fallbackFiles: Set<String> = fallbackVoice?.let { assetSet("voz/$it") } ?: emptySet()
    private val myDir = java.io.File(app.filesDir, "mivoz")
    private val music: ExoPlayer? = if (fondo.isEmpty()) null else ExoPlayer.Builder(app).build().apply {
        setMediaItem(MediaItem.fromUri("asset:///soundscape/sounds/$fondo.ogg"))
        repeatMode = Player.REPEAT_MODE_ONE
        volume = 0f
        prepare()
    }
    private val voice: ExoPlayer? = if (voz.isEmpty()) null else ExoPlayer.Builder(app).build()

    private fun uriFor(key: String): String? = when {
        voz == "mia" && java.io.File(myDir, "$key.m4a").exists() -> android.net.Uri.fromFile(java.io.File(myDir, "$key.m4a")).toString()
        "$key.ogg" in voiceFiles -> "asset:///voz/$voz/$key.ogg"
        "$key.ogg" in fallbackFiles -> "asset:///voz/$fallbackVoice/$key.ogg"
        else -> null
    }
    private var tts: TextToSpeech? = null
    @Volatile private var ttsOk = false
    private var fading = false

    init {
        // Si había un sonido de Calma sonando, se pausa para no mezclar.
        val prefs = app.getSharedPreferences(SoundPlaybackService.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(SoundPlaybackService.KEY_PLAYING, false)) {
            runCatching { app.startService(Intent(app, SoundPlaybackService::class.java).setAction(SoundPlaybackService.ACTION_PAUSE)) }
        }
        if (voz.isNotEmpty()) {
            // La voz del teléfono queda de respaldo, por si falta alguna frase grabada.
            tts = TextToSpeech(app) { status ->
                val t = tts ?: return@TextToSpeech
                if (status != TextToSpeech.SUCCESS) return@TextToSpeech
                // La voz en español de mejor calidad que funcione sin internet.
                val best = runCatching {
                    t.voices.filter { it.locale.language == "es" && !it.isNetworkConnectionRequired }
                        .maxByOrNull { it.quality + if (it.locale.country in listOf("AR", "UY")) 1000 else 0 }
                }.getOrNull()
                if (best != null) t.voice = best else t.language = Locale("es")
                t.setSpeechRate(0.78f)
                t.setPitch(0.92f)
                ttsOk = true
            }
        }
        music?.play()
    }

    private fun speaking(): Boolean =
        (voice?.let { it.isPlaying || it.playbackState == Player.STATE_BUFFERING } ?: false) || (tts?.isSpeaking ?: false)

    /** Mezcla suave: la música entra despacio, baja cuando habla la voz y vuelve a subir. */
    suspend fun run() {
        val m = music ?: return
        while (true) {
            val target = when {
                fading -> 0f
                speaking() -> 0.12f
                else -> 0.3f
            }
            m.volume += (target - m.volume) * 0.08f
            delay(100)
        }
    }

    fun say(text: String) {
        val key = voiceKey(text)
        val v = voice
        val uri = uriFor(key)
        if (v != null && uri != null) {
            v.setMediaItem(MediaItem.fromUri(uri))
            v.prepare()
            v.play()
        } else if (ttsOk) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, key)
        }
    }

    /** Dice la frase y espera a que termine (como mucho 20 segundos). */
    suspend fun sayAndWait(text: String) {
        if (voz.isEmpty()) {
            delay(1500L + text.length * 45L) // sin voz: tiempo para leerla
            return
        }
        say(text)
        delay(400)
        withTimeoutOrNull(20_000) { while (speaking()) delay(120) }
    }

    /** Baja la música despacio hasta silenciarla. */
    suspend fun fadeOut() {
        fading = true
        val m = music ?: return
        withTimeoutOrNull(7000) { while (m.volume > 0.01f) { m.volume *= 0.9f; delay(100) } }
        m.pause()
    }

    fun release() {
        music?.release()
        voice?.release()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

/** La misma clave que usa tools/generar_voz.py para nombrar cada archivo de voz. */
fun voiceKey(text: String): String =
    MessageDigest.getInstance("SHA-1").digest(text.trim().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }.take(12)
