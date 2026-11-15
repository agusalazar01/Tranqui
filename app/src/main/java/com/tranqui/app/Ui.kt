package com.tranqui.app

import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Paleta de Tranqui: lino, salvia, bosque, arcilla y arena. */
object C {
    val Lino = Color(0xFFF3EFE5)
    val Papel = Color(0xFFFAF7EF)
    val Borde = Color(0xFFE4DDCF)
    val Bruma = Color(0xFFECE8DC)
    val Salvia = Color(0xFFA7B39B)
    val SalviaClara = Color(0xFFE3E6D8)
    val Musgo = Color(0xFF6F8774)
    val Bosque = Color(0xFF344C40)
    val BosqueOscuro = Color(0xFF263A30)
    val Texto2 = Color(0xFF55655A)
    val Arcilla = Color(0xFFD5A080)
    val ArcillaClara = Color(0xFFEFDCCB)
    val ArcillaOscura = Color(0xFF3B2A1E)
    val Arena = Color(0xFFD9D2C5)
}

val Serif: FontFamily = FontFamily.Serif

private val scheme = lightColorScheme(
    primary = C.Bosque, onPrimary = C.Lino,
    secondary = C.Salvia, onSecondary = C.BosqueOscuro,
    tertiary = C.Arcilla, onTertiary = C.ArcillaOscura,
    background = C.Lino, onBackground = C.Bosque,
    surface = C.Papel, onSurface = C.Bosque,
    surfaceVariant = C.SalviaClara, onSurfaceVariant = C.Texto2,
    outline = C.Arena
)

@Composable
fun TranquiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun SerifText(
    text: String, size: Int, modifier: Modifier = Modifier, color: Color = C.Bosque,
    italic: Boolean = false, align: TextAlign? = null
) {
    Text(
        text, modifier, color = color, fontFamily = Serif, fontSize = size.sp,
        lineHeight = (size * 1.22).sp, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        textAlign = align
    )
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = C.Bosque, size: Int = 16, bold: Boolean = false, align: TextAlign? = null) {
    Text(
        text, modifier, color = color, fontSize = size.sp, lineHeight = (size * 1.4).sp,
        fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal, textAlign = align
    )
}

@Composable
fun Small(text: String, modifier: Modifier = Modifier, color: Color = C.Texto2, align: TextAlign? = null, size: Int = 14) {
    Text(text, modifier, color = color, fontSize = size.sp, lineHeight = (size * 1.42).sp, textAlign = align)
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = C.Texto2, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SerifText(title, 32)
        if (subtitle != null) Body(subtitle, color = C.Texto2, size = 15)
    }
}

@Composable
fun AccentLine() {
    Box(Modifier.width(64.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(C.Arcilla))
}

/** Tarjeta redondeada de Tranqui. */
@Composable
fun TCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    bg: Color = C.Papel,
    bordered: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    var m = modifier.fillMaxWidth().clip(shape).background(bg)
    if (bordered) m = m.border(1.dp, C.Borde, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(horizontal = 18.dp, vertical = 17.dp), verticalArrangement = Arrangement.spacedBy(5.dp), content = content)
}

@Composable
fun Chevron(color: Color = C.Bosque) {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = color)
}

/** Etiqueta de evidencia: Fuerte, Mixta, Tradición o Sin estudios. */
@Composable
fun Tag(text: String) {
    val (bg, fg, border) = when (text) {
        "Fuerte" -> Triple(C.Bosque, C.Lino, null)
        "Mixta" -> Triple(Color(0xFFCBD3C2), C.BosqueOscuro, null)
        "Tradición" -> Triple(C.Lino, C.BosqueOscuro, C.Salvia)
        else -> Triple(C.ArcillaClara, C.ArcillaOscura, null)
    }
    var m = Modifier.clip(RoundedCornerShape(12.dp)).background(bg)
    if (border != null) m = m.border(1.dp, border, RoundedCornerShape(12.dp))
    Text(
        if (text == "Fuerte") "Evidencia fuerte" else text,
        m.padding(horizontal = 10.dp, vertical = 4.dp), color = fg, fontSize = 12.sp
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, bg: Color = C.Bosque, fg: Color = C.Lino, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(28.dp)).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = fg, fontSize = 17.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(C.Papel)
            .border(1.dp, C.Borde, RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = C.Bosque, fontSize = 15.sp) }
}

/** Opción seleccionable (chip). */
@Composable
fun Choice(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.heightIn(min = 44.dp).clip(shape).background(if (selected) C.Bosque else C.Papel)
            .border(1.dp, if (selected) C.Bosque else C.Borde, shape).clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (selected) C.Lino else C.Bosque, fontSize = 15.sp, textAlign = TextAlign.Center) }
}

@Composable
fun BackBar(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE9E4D8)).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = C.Bosque) }
        Spacer(Modifier.width(14.dp))
        SerifText(title, 24)
    }
}

/** Forma orgánica estática (mancha). */
@Composable
fun Blob(color: Color, size: Int, variant: Int = 0) {
    val shapes = listOf(
        RoundedCornerShape(55, 45, 60, 40),
        RoundedCornerShape(45, 55, 42, 58),
        RoundedCornerShape(60, 40, 50, 50)
    )
    Box(Modifier.size(size.dp).clip(shapes[variant % 3]).background(color))
}

/** Bruma: manchas difusas que se mueven lento, sin centro quemado. */
@Composable
fun Mist(modifier: Modifier, colors: List<Color>, count: Int = 4, alpha: Float = 0.4f, periodMs: Int = 60000) {
    // Movimiento lento a ~15 cuadros por segundo: se ve igual de suave y gasta mucha menos batería.
    // Con «Quitar animaciones» activado en Android, la bruma queda quieta.
    val ctx = LocalContext.current
    val still = remember {
        android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val seed = remember { (Math.random() * periodMs).toLong() }
    val t by produceState(initialValue = (seed % periodMs) / periodMs.toFloat() * (2 * PI).toFloat(), still, periodMs) {
        if (still) return@produceState
        while (true) {
            withFrameMillis { ms -> value = ((ms + seed) % periodMs) / periodMs.toFloat() * (2 * PI).toFloat() }
            delay(66)
        }
    }
    Canvas(modifier) {
        val m = minOf(size.width, size.height)
        for (i in 0 until count) {
            val a = t + i * (2 * PI.toFloat() / count)
            val cx = size.width / 2 + cos(a) * size.width * 0.13f
            val cy = size.height / 2 + sin(2 * a) * size.height * 0.08f
            val r = m * (0.3f + 0.04f * (i % 3)) * (1f + 0.05f * sin(t * 3 + i))
            val c = colors[i % colors.size]
            drawCircle(
                Brush.radialGradient(
                    listOf(c.copy(alpha = alpha), c.copy(alpha = alpha * 0.55f), c.copy(alpha = 0f)),
                    center = Offset(cx, cy), radius = r
                ),
                radius = r, center = Offset(cx, cy)
            )
        }
    }
}

/** Colinas en capas, como en las referencias de diseño. */
@Composable
fun Hills(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun layer(y0: Float, y1: Float, y2: Float, color: Color) {
            val p = Path().apply {
                moveTo(0f, h * y0)
                cubicTo(w * 0.25f, h * (y0 - 0.2f), w * 0.45f, h * y1, w * 0.62f, h * (y1 - 0.08f))
                cubicTo(w * 0.8f, h * (y1 - 0.15f), w * 0.9f, h * y2, w, h * y2)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(p, color)
        }
        layer(0.42f, 0.36f, 0.38f, C.Salvia.copy(alpha = 0.45f))
        layer(0.62f, 0.58f, 0.6f, C.Musgo.copy(alpha = 0.6f))
        layer(0.8f, 0.82f, 0.74f, C.Bosque)
    }
}

fun fmtClock(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
