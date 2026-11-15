package com.tranqui.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Detecta cuándo se abre una de las apps elegidas y muestra la pausa.
 * No lee el contenido de la pantalla: solo el nombre de la app que pasa al frente.
 */
class PauseAccessibilityService : AccessibilityService() {
    private var lastPkg = ""
    private var lastAt = 0L
    /** La última app que estuvo al frente (sin contar teclado ni barra del sistema). */
    private var foreground = ""
    private var ignored = emptySet<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Store.init(this)
        val ime = runCatching {
            getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }
        }.getOrDefault(emptyList())
        ignored = (ime + listOf("com.android.systemui", "android")).toSet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        // El teclado, la barra de notificaciones y los diálogos del sistema no cuentan como cambiar de app.
        if (pkg in ignored) return
        val previous = foreground
        foreground = pkg
        if (pkg == packageName) return
        Store.init(this)
        if (pkg !in Store.blocked) return
        // Solo al entrar a la app desde otra: no interrumpe mientras ya la estás usando.
        if (pkg == previous) return
        if (PauseActivity.showing) return
        if (Store.isAllowed(pkg)) return
        val now = SystemClock.elapsedRealtime()
        if (pkg == lastPkg && now - lastAt < 2500) return
        lastPkg = pkg
        lastAt = now
        startActivity(
            Intent(this, PauseActivity::class.java)
                .putExtra("pkg", pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    override fun onInterrupt() {}
}

class PauseActivity : ComponentActivity() {
    companion object {
        @Volatile
        var showing = false
    }

    private var pkg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        pkg = intent.getStringExtra("pkg") ?: ""
        val name = appLabel(this, pkg)
        setContent {
            TranquiTheme {
                BackHandler { noEntrar() }
                PauseScreen(
                    appName = name,
                    windowSec = Store.windowSeconds,
                    speedDp = Store.speed,
                    onNo = { noEntrar() },
                    onEnter = {
                        Store.allow(pkg, 5)
                        Store.logInterception(pkg, "entro")
                        finish()
                    },
                    onOption = { size ->
                        Store.logInterception(pkg, "actividad")
                        startActivity(
                            Intent(this, MainActivity::class.java).putExtra("quick", size)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                        finish()
                    }
                )
            }
        }
    }

    private fun noEntrar() {
        Store.logInterception(pkg, "no")
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    override fun onStart() {
        super.onStart()
        showing = true
    }

    override fun onStop() {
        super.onStop()
        showing = false
        // Si salís con el botón de inicio, la pausa no queda guardada para otra app.
        if (!isChangingConfigurations && !isFinishing) finish()
    }
}

@Composable
fun PauseScreen(
    appName: String,
    windowSec: Int,
    speedDp: Int,
    onNo: () -> Unit,
    onEnter: () -> Unit,
    onOption: (Int) -> Unit
) {
    var gone by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().background(C.Lino)) {
        val density = LocalDensity.current
        val areaW = constraints.maxWidth.toFloat()
        val reserved = with(density) { 120.dp.toPx() }
        val areaH = constraints.maxHeight.toFloat() - reserved

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Breathing()
            Small("Ibas a abrir $appName", align = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            OptionRow("1 min · respirar o estirar") { onOption(1) }
            OptionRow("5 min · tu paso de hoy") { onOption(5) }
            OptionRow("15 min · sesión de foco") { onOption(15) }
            Spacer(Modifier.weight(1f))
            if (gone) Small("Si la necesitás, volvé a abrir la app.", align = TextAlign.Center)
            PrimaryButton("No entrar", Modifier.heightIn(min = 60.dp), onClick = onNo)
        }

        if (!gone && areaW > 0 && areaH > 0) {
            BouncingEnter(areaW, areaH, windowSec, speedDp, onEnter) { gone = true }
        }
    }
}

@Composable
private fun OptionRow(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape).background(C.Papel)
            .border(1.dp, C.Borde, shape).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Body(text, Modifier.weight(1f))
        Chevron()
    }
}

/** Bruma que respira: 4 segundos inhalar, 4 exhalar. Sin frases motivacionales. */
@Composable
fun Breathing() {
    var inhale by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            inhale = !inhale
            delay(4000)
        }
    }
    val s by animateFloatAsState(if (inhale) 1.12f else 0.82f, tween(4000, easing = FastOutSlowInEasing), label = "respirar")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(220.dp).graphicsLayer { scaleX = s; scaleY = s }) {
            Mist(Modifier.fillMaxSize(), listOf(C.Salvia, C.Musgo, C.Salvia), count = 3, alpha = 0.55f, periodMs = 45000)
        }
        SerifText(if (inhale) "Inhalá…" else "Exhalá…", 32)
    }
}

/**
 * «Entrar igual»: rebota por la pantalla con recorridos largos y direcciones al azar,
 * nunca vuelve por donde vino, y se desvanece hasta desaparecer.
 */
@Composable
private fun BouncingEnter(areaW: Float, areaH: Float, windowSec: Int, speedDp: Int, onEnter: () -> Unit, onGone: () -> Unit) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val bw = with(density) { 128.dp.toPx() }
    val bh = with(density) { 44.dp.toPx() }
    val speed = with(density) { speedDp.dp.toPx() }
    val reduceMotion = remember {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var x by remember { mutableFloatStateOf(-1f) }
    var y by remember { mutableFloatStateOf(-1f) }
    var alpha by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(areaW, areaH) {
        val w = areaW - bw
        val h = areaH - bh
        if (w <= 0f || h <= 0f) return@LaunchedEffect
        x = w * (0.2f + 0.6f * Random.nextFloat())
        y = h * (0.2f + 0.6f * Random.nextFloat())
        var ang = chooseAngle(x, y, w, h, 0, 0, null)
        val start = withFrameNanos { it }
        var last = start
        while (true) {
            val now = withFrameNanos { it }
            val dt = min(0.05f, (now - last) / 1e9f)
            last = now
            val el = (now - start) / 1e9f
            val half = windowSec / 2f
            if (el >= windowSec) {
                onGone()
                break
            }
            alpha = if (el < half) 1f else max(0f, 1f - (el - half) / half)
            if (!reduceMotion) {
                x += cos(ang) * speed * dt
                y += sin(ang) * speed * dt
                var nx = 0
                var ny = 0
                if (x <= 0f) { x = 0f; nx = 1 } else if (x >= w) { x = w; nx = -1 }
                if (y <= 0f) { y = 0f; ny = 1 } else if (y >= h) { y = h; ny = -1 }
                if (nx != 0 || ny != 0) ang = chooseAngle(x, y, w, h, nx, ny, ang)
            }
        }
    }

    if (x >= 0f) {
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .graphicsLayer { this.alpha = alpha }
                .size(128.dp, 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(C.Lino)
                .border(1.dp, C.Arena, RoundedCornerShape(22.dp))
                .clickable(onClick = onEnter),
            contentAlignment = Alignment.Center
        ) {
            Text("Entrar igual", color = C.Texto2, fontSize = 14.sp)
        }
    }
}

private fun travel(px: Float, py: Float, a: Float, w: Float, h: Float): Float {
    val cx = cos(a)
    val cy = sin(a)
    var t = Float.MAX_VALUE
    if (cx > 1e-6f) t = min(t, (w - px) / cx) else if (cx < -1e-6f) t = min(t, -px / cx)
    if (cy > 1e-6f) t = min(t, (h - py) / cy) else if (cy < -1e-6f) t = min(t, -py / cy)
    return t
}

private fun angDiff(a: Float, b: Float): Float {
    val twoPi = (2 * PI).toFloat()
    val d = abs(a - b) % twoPi
    return if (d > PI) twoPi - d else d
}

/**
 * Elige una dirección al azar que: deje el próximo choque lejos (70% del lado corto),
 * no vuelva por donde vino (40°) y no salga pegada a la pared (25°).
 */
private fun chooseAngle(px: Float, py: Float, w: Float, h: Float, nx: Int, ny: Int, prev: Float?): Float {
    val need = 0.7f * min(w, h)
    val back = prev?.plus(PI.toFloat())
    val wall = sin(25 * PI / 180).toFloat()
    val backMin = (40 * PI / 180).toFloat()
    var best: Float? = null
    var bestLen = -1f
    repeat(300) {
        val a = Random.nextFloat() * (2 * PI).toFloat()
        val cx = cos(a)
        val cy = sin(a)
        if (nx != 0 && cx * nx < wall) return@repeat
        if (ny != 0 && cy * ny < wall) return@repeat
        if (nx == 0 && ny == 0 && (abs(cx) < wall || abs(cy) < wall)) return@repeat
        if (back != null && angDiff(a, back) < backMin) return@repeat
        val len = travel(px, py, a, w, h)
        if (len >= need) return a
        if (len > bestLen) {
            bestLen = len
            best = a
        }
    }
    return best ?: atan2((if (ny == 0) 1 else ny).toFloat(), (if (nx == 0) 1 else nx).toFloat())
}
