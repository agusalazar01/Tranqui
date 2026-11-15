package com.tranqui.app

import android.annotation.SuppressLint
import android.content.Context
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
fun CalmaScreen(nav: Nav) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenTitle("Calma", "Sonidos y prácticas para bajar un cambio")
        SectionLabel("Paisajes sonoros")
        SoundscapeView(Modifier.fillMaxWidth().height(600.dp).clip(RoundedCornerShape(28.dp)))
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

/** Puente para que la página de sonidos sepa qué grabaciones reales hay en la app. */
class SoundBridge(private val ctx: Context) {
    @JavascriptInterface
    fun sounds(): String = (ctx.assets.list("soundscape/sounds") ?: emptyArray()).joinToString(",")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SoundscapeView(modifier: Modifier) {
    val ctx = LocalContext.current
    val web = remember {
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
            .build()
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            setBackgroundColor(0xFFF3EFE5.toInt())
            addJavascriptInterface(SoundBridge(ctx), "TranquiApp")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    loader.shouldInterceptRequest(request.url)
            }
            loadUrl("https://appassets.androidplatform.net/assets/soundscape/index.html")
        }
    }
    DisposableEffect(Unit) {
        onDispose { web.destroy() }
    }
    AndroidView(factory = { web }, modifier = modifier)
}

@Composable
fun PracticeScreen(id: String, nav: Nav, presetMinutes: Int? = null) {
    val p = PRACTICES.firstOrNull { it.id == id } ?: PRACTICES.first()
    var minutes by remember { mutableIntStateOf(presetMinutes?.takeIf { it in p.options } ?: p.defaultMinutes) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var vibrate by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackBar(p.name) { nav.pop() }
        when {
            running -> {
                PracticeRunner(p, minutes, vibrate) {
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
                PrimaryButton("Empezar") { running = true }
            }
        }
    }
}

@Composable
private fun PracticeRunner(p: Practice, minutes: Int, vibrate: Boolean, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
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
        if (p.phases.isNotEmpty()) {
            while (System.currentTimeMillis() < end) {
                for (ph in p.phases) {
                    label = ph.label
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
                delay(per)
            }
            breathing.cancel()
        }
        clock.cancel()
        buzz(ctx, 200)
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
