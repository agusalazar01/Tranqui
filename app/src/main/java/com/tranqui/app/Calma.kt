package com.tranqui.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.animation.core.LinearEasing
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import android.webkit.JavascriptInterface
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.LifecycleResumeEffect
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
        SectionLabel("Respiraciones y prácticas: ¿para qué?")
        PRACTICE_GROUPS.forEach { g ->
            val best = PRACTICES.firstOrNull { it.id == g.best }
            TCard(onClick = { nav.push(Route.PracticeGroupRoute(g.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SerifText(g.title, 19)
                        Small(g.cuando)
                        if (best != null) Small("La mejor: ${best.name}", color = C.Musgo)
                    }
                    Chevron()
                }
            }
        }
        TCard(onClick = { nav.push(Route.Hipnosis) }, bg = C.SalviaClara, bordered = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SerifText("Autohipnosis", 19)
                    Small("Guiones para completar con tus palabras y grabar con tu voz")
                }
                Chevron()
            }
        }
        var todas by rememberSaveable { mutableStateOf(false) }
        TextButton(onClick = { todas = !todas }) {
            Text(if (todas) "Ocultar la lista completa" else "Ver todas (${PRACTICES.size})", color = C.Bosque)
        }
        if (todas) PRACTICES.forEach { PracticeRow(it, nav) }
        Small("Si te mareás, volvé a respirar normal. No incluimos respiraciones rápidas tipo Wim Hof: pueden hacer desmayar sin aviso y nunca deben hacerse en el agua.")
        RecordVoiceCard(nav)
        Small("Las prácticas son para el bienestar diario y no reemplazan un tratamiento.")
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PracticeRow(p: Practice, nav: Nav) {
    TCard(onClick = { nav.push(Route.PracticeRun(p.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SerifText(p.name, 19)
                Small(if (p.ritmo.isNotEmpty()) "${p.ritmo} · ${p.durationLabel}" else p.durationLabel)
            }
            Tag(p.tag)
        }
    }
}

@Composable
fun PracticeGroupScreen(id: String, nav: Nav) {
    val g = PRACTICE_GROUPS.firstOrNull { it.id == id } ?: PRACTICE_GROUPS.first()
    val best = PRACTICES.firstOrNull { it.id == g.best }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar(g.title) { nav.pop() }
        Small(g.cuando)
        if (best != null) {
            TCard(bg = C.Bosque, bordered = false) {
                Small("La mejor para esto", color = C.Arena)
                SerifText(best.name, 26, color = C.Lino)
                if (best.ritmo.isNotEmpty()) Small(best.ritmo, color = C.Arena)
                Spacer(Modifier.height(6.dp))
                Body(g.porQue, color = C.Lino, size = 15)
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Empezar", bg = C.Lino, fg = C.Bosque) { nav.push(Route.PracticeRun(best.id)) }
            }
        }
        SectionLabel("También te sirven")
        g.otros.mapNotNull { o -> PRACTICES.firstOrNull { it.id == o } }.forEach { PracticeRow(it, nav) }
        Spacer(Modifier.height(16.dp))
    }
}

/** Puente entre la interfaz animada y el reproductor nativo persistente. */
class SoundBridge(private val ctx: Context, private val onHeight: (Int) -> Unit = {}, private val onLibrary: (Boolean) -> Unit = {}) {
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    /** Grabaciones que están dentro de la app (sin extensión). */
    @JavascriptInterface fun available(): String =
        (ctx.assets.list("soundscape/sounds") ?: emptyArray()).filter { it.endsWith(".ogg") }
            .joinToString(",") { it.removeSuffix(".ogg") }

    /** La página avisa cuánto mide, en px de CSS (equivalen a dp). */
    @JavascriptInterface fun height(px: Int) {
        main.post { onHeight(px) }
    }

    /** La página avisa si la biblioteca está abierta, para que «atrás» la cierre. */
    @JavascriptInterface fun libraryOpen(open: Boolean) {
        main.post { onLibrary(open) }
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
    var libraryOpen by remember { mutableStateOf(false) }
    val web = remember {
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
            .build()
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            setBackgroundColor(0xFFF3EFE5.toInt())
            addJavascriptInterface(
                SoundBridge(ctx, onHeight = { h -> if (h > 200) pageHeight = h }, onLibrary = { libraryOpen = it }),
                "TranquiApp"
            )
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
    BackHandler(enabled = libraryOpen) { web.evaluateJavascript("closeLibrary()", null) }
    AndroidView(factory = { web }, modifier = modifier.height(pageHeight.dp))
}

/** Música de fondo para las prácticas: grabaciones del catálogo que acompañan sin distraer. */
private val FONDOS = listOf(
    "" to "Sin sonido",
    "tanpura" to "Tanpura",
    "campanas" to "Campanas",
    "cuenco-grave" to "Cuenco",
    "lluvia-bosque" to "Lluvia",
    "arroyo" to "Arroyo",
    "olas-suaves" to "Olas",
    "ruido-marron" to "Ruido suave",
    "tambor-trance" to "Tambor (4 por segundo)",
    "tambor-lento" to "Tambor lento",
    "tambor-chaman" to "Tambor chamánico",
    "didgeridoo" to "Didgeridoo",
    "didgeridoo-dron" to "Didgeridoo grave",
    "didgeridoo-ritmo" to "Didgeridoo con ritmo",
    "zumbador-sonajero" to "Zumbador y sonajero",
    "mix-viaje" to "Viaje chamánico",
    "mix-descenso" to "Descenso profundo",
    "mix-bosque-ritual" to "Bosque ritual",
    "mix-mar-interior" to "Mar interior",
    "mix-fogon" to "Noche de fogón",
    "mix-tierra" to "Tierra que late"
)

/** Voz de las prácticas: «Silencio» (solo texto en pantalla) o «Mi voz» (grabada por vos). */
private val VOCES = listOf("" to "Silencio", "mia" to "Mi voz")

private fun fondos(ctx: Context): List<Pair<String, String>> {
    val hay = runCatching { ctx.assets.list("soundscape/sounds")?.toSet() }.getOrNull().orEmpty()
    return FONDOS.filter { (id, _) -> id.isEmpty() || "$id.ogg" in hay }
}

@Composable
fun PracticeScreen(id: String, nav: Nav, presetMinutes: Int? = null) {
    val p = practiceById(id)
    val hipno = id.startsWith("hip_")
    var salirSuave by remember { mutableStateOf(false) }
    var minutes by remember { mutableIntStateOf(presetMinutes?.takeIf { it in p.options } ?: p.defaultMinutes) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var vibrate by remember { mutableStateOf(true) }
    val ctx = LocalContext.current
    val fondosDisponibles = remember { fondos(ctx) }
    val frases = remember(id) { practicePhrases(p) }
    val total = frases.size
    Store.tick.intValue
    val voces = Voces.list()
    var voz by remember { mutableStateOf(Store.practiceVoice.takeIf { v -> voces.any { it.id == v } } ?: "") }
    var nuevaVoz by remember { mutableStateOf(false) }
    // Se vuelve a contar al regresar de grabar.
    var grabadas by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(voz) {
        grabadas = if (voz.isEmpty()) 0 else Voces.count(ctx, voz, frases)
        onPauseOrDispose { }
    }
    var fondo by remember { mutableStateOf(Store.practiceMusic.takeIf { f -> fondosDisponibles.any { it.first == f } } ?: "") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackBar(p.name) { nav.pop() }
        when {
            running -> {
                PracticeRunner(p, minutes, vibrate, voz, fondo, salir = { salirSuave }) {
                    Store.addSession(Session(Store.today(), null, p.name, minutes))
                    running = false
                    done = true
                }
                // En la hipnosis no se corta de golpe: se salta a la cuenta para volver.
                TextButton(
                    onClick = { if (hipno) salirSuave = true else running = false },
                    enabled = !salirSuave,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (salirSuave) "Volviendo despacio…" else "Terminar antes", color = C.Texto2)
                }
            }
            done -> {
                Spacer(Modifier.height(40.dp))
                SerifText("Listo", 34, Modifier.align(Alignment.CenterHorizontally))
                Body("$minutes minutos de ${p.name.lowercase()}.", Modifier.align(Alignment.CenterHorizontally))
                PrimaryButton("Volver") { nav.pop() }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(p.tag)
                    if (p.ritmo.isNotEmpty()) Small(p.ritmo, color = C.Musgo)
                }
                Body(p.que)
                Small(p.evidencia)
                p.cuidado?.let {
                    TCard(bg = C.ArcillaClara, bordered = false) { Small(it, color = C.ArcillaOscura) }
                }
                SectionLabel("Cómo se hace")
                p.pasos.forEachIndexed { i, s -> Body("${i + 1}. $s") }
                SectionLabel("Duración")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.options.forEach { m -> Choice("$m min", m == minutes) { minutes = m } }
                }
                SectionLabel("Voz")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("Silencio", voz.isEmpty()) { voz = ""; Store.practiceVoice = "" }
                    voces.forEach { v -> Choice(v.text, v.id == voz) { voz = v.id; Store.practiceVoice = v.id } }
                    Choice("+ Nueva voz", false) { nuevaVoz = true }
                }
                if (voz.isNotEmpty()) {
                    if (grabadas < total) {
                        TCard(onClick = { nav.push(Route.VoiceRecord(p.id, voz)) }, bg = C.ArcillaClara, bordered = false) {
                            Body(
                                if (grabadas == 0) "Grabar esta práctica con «${Voces.name(voz)}»" else "Te faltan ${total - grabadas} de $total frases",
                                color = C.ArcillaOscura, bold = true
                            )
                            Small("Son $total frases, solo de esta práctica. Las que falten se leen en pantalla.", color = C.ArcillaOscura)
                        }
                    } else {
                        TextButton(onClick = { nav.push(Route.VoiceRecord(p.id, voz)) }) { Text("Escuchar o regrabar las frases", color = C.Bosque) }
                    }
                }
                SectionLabel("Música de fondo")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fondosDisponibles.forEach { (f, nombre) -> Choice(nombre, f == fondo) { fondo = f; Store.practiceMusic = f } }
                }
                if (fondo.isNotEmpty()) {
                    var vol by remember { mutableFloatStateOf(Store.fondoVolumen) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Small("Volumen del fondo", Modifier.width(130.dp))
                        Slider(
                            value = vol, onValueChange = { vol = it }, onValueChangeFinished = { Store.fondoVolumen = vol },
                            valueRange = 0.1f..1f, modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = C.Bosque, activeTrackColor = C.Musgo)
                        )
                    }
                }
                Small(if (voz.isNotEmpty()) "El fondo baja solo cuando habla la voz. La voz se ajusta en «Tus voces» → «Cómo suena»." else "El fondo baja solo cuando habla la voz.")
                if (p.allPhases.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Body("Vibrar en cada cambio", Modifier.weight(1f))
                        Switch(
                            checked = vibrate, onCheckedChange = { vibrate = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = C.Bosque)
                        )
                    }
                }
                PrimaryButton("Empezar") { running = true }
                if (nuevaVoz) NuevaVozDialog({ nuevaVoz = false }) { nombre ->
                    val nid = Voces.add(ctx, nombre)
                    Store.practiceVoice = nid; voz = nid; nuevaVoz = false
                    nav.push(Route.VoiceRecord(p.id, nid))
                }
            }
        }
    }
}

@Composable
private fun PracticeRunner(p: Practice, minutes: Int, vibrate: Boolean, voz: String, fondo: String, salir: () -> Boolean = { false }, onDone: () -> Unit) {
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
    // Cuánto queda de la fase (1 = recién empieza, 0 = terminó): dibuja la barrita alrededor de la frase.
    val resto = remember { Animatable(0f) }

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
        if (p.allPhases.isNotEmpty()) {
            audio.sayAndWait(VOZ_INICIO_RESPIRAR)
            delay(1000)
            var cycle = 0
            val inicio = System.currentTimeMillis()
            var tramo = p.phasesAt(0f)
            while (System.currentTimeMillis() < end) {
                // En las prácticas por tramos, el ritmo cambia al empezar cada vuelta.
                val ahora = p.phasesAt((System.currentTimeMillis() - inicio).toFloat() / (end - inicio).coerceAtLeast(1))
                if (ahora !== tramo) { tramo = ahora; cycle = 0 }
                for (ph in tramo) {
                    label = ph.label
                    val cuenta = launch {
                        resto.snapTo(1f)
                        resto.animateTo(0f, tween((ph.seconds * 1000).toInt(), easing = LinearEasing))
                    }
                    // La voz acompaña las primeras vueltas y después solo de vez en cuando:
                    // el círculo y la vibración alcanzan para seguir el ritmo.
                    val say = ph.say
                    if (say != null && (cycle < 3 || cycle % 5 == 0)) audio.say(say)
                    if (vibrate && say != null) buzz(ctx, 25)
                    scale.animateTo(ph.scale, tween((ph.seconds * 1000).toInt(), easing = FastOutSlowInEasing))
                    cuenta.cancel()
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
                if (salir()) break
                val stepEnd = System.currentTimeMillis() + per
                label = s
                // «…» = pausa: primero se tensa, unos segundos después la voz dice cuándo soltar.
                val parts = s.split("…").map { it.trim() }.filter { it.isNotEmpty() }
                parts.forEachIndexed { i, part ->
                    audio.sayAndWait(part)
                    if (i < parts.lastIndex) delay(5000)
                }
                while (System.currentTimeMillis() < stepEnd && !salir()) delay(200)
            }
            breathing.cancel()
        }
        clock.cancel()
        label = "Muy bien"
        p.cierre.ifEmpty { VOZ_CIERRE }.split("…").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
            audio.sayAndWait(it)
            delay(1500)
        }
        audio.fadeOut()
        mixer.cancel()
        onDone()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
                Mist(Modifier.fillMaxSize(), listOf(C.Salvia, C.Musgo, C.Salvia), count = 3, alpha = 0.5f, periodMs = 50000)
            }
        }
        Spacer(Modifier.height(20.dp))
        // Las frases cambian con un fundido lento, sin saltos.
        val respira = p.allPhases.isNotEmpty()
        val trazo = C.Musgo
        val pista = C.Borde
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                .then(if (!respira) Modifier else Modifier.drawBehind {
                    // Un rectángulo redondeado alrededor de la frase que se va desarmando
                    // con los segundos de la fase, empezando por arriba al centro.
                    val w = 2.dp.toPx()
                    val r = size.height / 2f
                    val camino = Path().apply {
                        addRoundRect(RoundRect(w / 2, w / 2, size.width - w / 2, size.height - w / 2, CornerRadius(r, r)))
                    }
                    drawPath(camino, pista.copy(alpha = 0.5f), style = Stroke(w))
                    val medida = PathMeasure().apply { setPath(camino, false) }
                    val largo = medida.length
                    val tramo = Path()
                    // addRoundRect empieza en la esquina de arriba a la izquierda: corremos el inicio
                    // a la mitad del lado de arriba para que se vacíe desde el centro.
                    val inicio = size.width / 2f - r
                    val fin = inicio + largo * resto.value
                    if (resto.value > 0.001f) {
                        if (fin <= largo) medida.getSegment(inicio, fin, tramo, true)
                        else { medida.getSegment(inicio, largo, tramo, true); medida.getSegment(0f, fin - largo, tramo, true) }
                        drawPath(tramo, trazo, style = Stroke(w, cap = StrokeCap.Round))
                    }
                })
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = label, animationSpec = tween(1400), label = "frase") { t ->
                SerifText(t, 24, Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(8.dp))
        Small("%d:%02d restantes".format(remaining / 60, remaining % 60))
    }
}

/**
 * Audio de las prácticas: voz y música de fondo.
 * - Voz: una de tus voces grabadas, guardada solo en este celular (filesDir/voces/<id>).
 * - Música: una grabación del catálogo en bucle, que baja sola mientras habla la voz.
 * Todo se usa desde el hilo principal (ExoPlayer lo necesita así).
 */
class PracticeAudio(ctx: Context, private val voz: String, private val fondo: String) {
    private val app = ctx.applicationContext
    private val music: ExoPlayer? = if (fondo.isEmpty()) null else ExoPlayer.Builder(app).build().apply {
        setMediaItem(MediaItem.fromUri("asset:///soundscape/sounds/$fondo.ogg"))
        repeatMode = Player.REPEAT_MODE_ONE
        volume = 0f
        prepare()
    }
    private val voice: VozFx? = if (voz.isEmpty()) null else VozFx(app, voz)
    /** Volumen del fondo elegido por vos (0 a 1); baja a menos de la mitad mientras habla la voz. */
    private val fondoVol = Store.fondoVolumen
    private var fading = false

    init {
        // Si había un sonido de Calma sonando, se pausa para no mezclar.
        val prefs = app.getSharedPreferences(SoundPlaybackService.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(SoundPlaybackService.KEY_PLAYING, false)) {
            runCatching { app.startService(Intent(app, SoundPlaybackService::class.java).setAction(SoundPlaybackService.ACTION_PAUSE)) }
        }
        music?.play()
    }

    private fun speaking(): Boolean = voice?.hablando() ?: false

    /** Mezcla suave: el fondo entra despacio, baja cuando habla la voz y vuelve a subir. */
    suspend fun run() {
        val m = music ?: return
        while (true) {
            val target = when {
                fading -> 0f
                speaking() -> fondoVol * 0.4f
                else -> fondoVol
            }
            m.volume += (target - m.volume) * 0.08f
            delay(100)
        }
    }

    /** Devuelve true si hay una grabación para esa frase y empezó a sonar. */
    fun say(text: String): Boolean {
        // Solo tu grabación, guardada en este celular. Si falta una frase, queda escrita en pantalla.
        return voice?.decir(text) ?: false
    }

    /** Dice la frase y espera a que termine; sin grabación, deja tiempo para leerla. */
    suspend fun sayAndWait(text: String) {
        if (!say(text)) {
            delay(1500L + text.length * 45L)
            return
        }
        delay(400)
        withTimeoutOrNull(20_000) { while (speaking()) delay(120) }
    }

    /** Baja el fondo despacio hasta silenciarlo. */
    suspend fun fadeOut() {
        fading = true
        val m = music ?: return
        withTimeoutOrNull(7000) { while (m.volume > 0.01f) { m.volume *= 0.9f; delay(100) } }
        m.pause()
    }

    fun release() {
        music?.release()
        voice?.release()
    }
}

/** Acceso a tus voces, visible en Calma. */
@Composable
fun RecordVoiceCard(nav: Nav) {
    Store.tick.intValue
    val n = Voces.list().size
    TCard(onClick = { nav.push(Route.Voices) }, bg = C.SalviaClara, bordered = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Body("Tus voces", bold = true)
                Small(if (n == 0) "Cada práctica se puede grabar con tu voz, o con la de alguien que te dé permiso." else "$n ${if (n == 1) "voz" else "voces"} en este celular. Tocá para verlas o borrarlas.")
            }
            Chevron()
        }
    }
}

/** Clave de cada frase: nombra el archivo de tu grabación. */
fun voiceKey(text: String): String =
    MessageDigest.getInstance("SHA-1").digest(text.trim().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }.take(12)
