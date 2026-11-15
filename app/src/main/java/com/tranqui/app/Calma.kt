package com.tranqui.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
    val ctx = LocalContext.current
    var synthetic by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenTitle("Calma", "Sonidos y prácticas para bajar un cambio")
        SectionLabel("Paisajes sonoros")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice("Grabaciones", !synthetic) { synthetic = false }
            Choice("Sintéticos · prueba", synthetic) {
                // Para no mezclar: pausa la grabación antes de abrir los sintéticos.
                ctx.startService(Intent(ctx, SoundPlaybackService::class.java).setAction(SoundPlaybackService.ACTION_PAUSE))
                synthetic = true
            }
        }
        if (synthetic) {
            Small("Versión en prueba: estos sonidos se generan en el momento y suenan solo mientras estás en Calma.")
            key("sinteticos") {
                SoundscapeView("sinteticos.html", Modifier.fillMaxWidth().height(600.dp).clip(RoundedCornerShape(28.dp)), fixedHeight = true)
            }
        } else {
            key("grabaciones") {
                SoundscapeView("index.html", Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)))
            }
        }
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
fun SoundscapeView(page: String, modifier: Modifier, fixedHeight: Boolean = false) {
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
    AndroidView(factory = { web }, modifier = if (fixedHeight) modifier else modifier.height(pageHeight.dp))
}

@Composable
fun PracticeScreen(id: String, nav: Nav, presetMinutes: Int? = null) {
    val p = PRACTICES.firstOrNull { it.id == id } ?: PRACTICES.first()
    var minutes by remember { mutableIntStateOf(presetMinutes?.takeIf { it in p.options } ?: p.defaultMinutes) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var vibrate by remember { mutableStateOf(true) }
    var voice by remember { mutableStateOf(Store.voiceGuide) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackBar(p.name) { nav.pop() }
        when {
            running -> {
                PracticeRunner(p, minutes, vibrate, voice) {
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
                if (p.phases.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Body("Vibrar en cada cambio", Modifier.weight(1f))
                        Switch(
                            checked = vibrate, onCheckedChange = { vibrate = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = C.Bosque)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Body("Guía por voz")
                        Small("Usa la voz de tu teléfono, sin internet. Así podés seguirla con los ojos cerrados.")
                    }
                    Switch(
                        checked = voice, onCheckedChange = { voice = it; Store.voiceGuide = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = C.Bosque)
                    )
                }
                PrimaryButton("Empezar") { running = true }
            }
        }
    }
}

@Composable
private fun PracticeRunner(p: Practice, minutes: Int, vibrate: Boolean, voice: Boolean, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val guide = remember { if (voice) VoiceGuide(ctx) else null }
    DisposableEffect(Unit) { onDispose { guide?.close() } }
    // La pantalla no se apaga durante la práctica: si se apagara, la guía se frenaría.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val scale = remember { Animatable(0.75f) }
    var label by remember { mutableStateOf("Preparate…") }
    var remaining by remember { mutableIntStateOf(minutes * 60) }

    LaunchedEffect(Unit) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        val clock = launch {
            while (true) {
                remaining = ((end - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                delay(500)
            }
        }
        guide?.awaitReady()
        if (p.phases.isNotEmpty()) {
            guide?.say("Vamos a respirar juntos. Seguí mi voz.")
            delay(if (guide != null) 3500 else 0)
            while (System.currentTimeMillis() < end) {
                for (ph in p.phases) {
                    label = ph.label
                    guide?.say(ph.say)
                    if (vibrate) buzz(ctx, 40)
                    scale.animateTo(ph.scale, tween((ph.seconds * 1000).toInt(), easing = FastOutSlowInEasing))
                }
            }
        } else {
            val breathing = launch {
                while (true) {
                    scale.animateTo(1.05f, tween(5000, easing = FastOutSlowInEasing))
                    scale.animateTo(0.85f, tween(5000, easing = FastOutSlowInEasing))
                }
            }
            val per = minutes * 60_000L / p.steps.size
            for (s in p.steps) {
                label = s
                guide?.say(s)
                delay(per)
            }
            breathing.cancel()
        }
        clock.cancel()
        buzz(ctx, 200)
        guide?.say("Listo. Cuando quieras, volvé despacio.")
        delay(if (guide != null) 3000 else 0)
        onDone()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.size(280.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
            Mist(Modifier.fillMaxSize(), listOf(C.Salvia, C.Musgo, C.Salvia), count = 3, alpha = 0.5f, periodMs = 50000)
        }
        Spacer(Modifier.height(20.dp))
        SerifText(label, 26, align = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Small("%d:%02d restantes".format(remaining / 60, remaining % 60))
    }
}

/**
 * Guía por voz con el texto a voz del teléfono: no incluye grabaciones, funciona sin internet
 * y no trae problemas de licencias. Habla lento y con un tono un poco más grave.
 */
class VoiceGuide(ctx: Context) {
    private val ready = kotlinx.coroutines.CompletableDeferred<Boolean>()
    private var tts: android.speech.tts.TextToSpeech? = null
    @Volatile private var ok = false

    init {
        tts = android.speech.tts.TextToSpeech(ctx.applicationContext) { status ->
            val t = tts
            if (status == android.speech.tts.TextToSpeech.SUCCESS && t != null) {
                val wanted = listOf(java.util.Locale("es", "AR"), java.util.Locale("es", "UY"), java.util.Locale("es", "US"), java.util.Locale("es", "MX"), java.util.Locale("es", "ES"), java.util.Locale("es"))
                val ok = wanted.firstOrNull { t.isLanguageAvailable(it) >= android.speech.tts.TextToSpeech.LANG_AVAILABLE }
                if (ok != null) t.language = ok
                t.setSpeechRate(0.82f)
                t.setPitch(0.95f)
                this.ok = ok != null
                ready.complete(ok != null)
            } else {
                ready.complete(false)
            }
        }
    }

    /** Espera a que la voz esté lista (como mucho 3 segundos). */
    suspend fun awaitReady() {
        kotlinx.coroutines.withTimeoutOrNull(3000) { ready.await() }
    }

    fun say(text: String) {
        if (ok) {
            // Saca los «…» para que no los lea como pausas raras.
            tts?.speak(text.replace("…", ","), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "tranqui")
        }
    }

    fun close() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
