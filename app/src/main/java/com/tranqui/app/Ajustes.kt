package com.tranqui.app

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.clip
import androidx.core.app.NotificationManagerCompat
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val SUGERIDAS = setOf(
    "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
    "com.facebook.katana", "com.twitter.android", "com.google.android.youtube",
    "com.reddit.frontpage", "com.snapchat.android", "com.pinterest"
)

fun pauseServiceEnabled(ctx: Context): Boolean {
    val s = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    return s.split(':').any { it.startsWith(ctx.packageName + "/") }
}

fun notificationsEnabled(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

fun batteryUnrestricted(ctx: Context): Boolean =
    ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) == true

private fun launchSettings(ctx: Context, i: Intent) {
    try {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Abre la lista de Accesibilidad e intenta resaltar a Tranqui (funciona en muchos teléfonos). */
fun openAccessibility(ctx: Context) {
    val comp = ComponentName(ctx, PauseAccessibilityService::class.java).flattenToString()
    val args = Bundle().apply { putString(":settings:fragment_args_key", comp) }
    launchSettings(
        ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(":settings:fragment_args_key", comp)
            .putExtra(":settings:show_fragment_args", args)
    )
}

fun openAppInfo(ctx: Context) =
    launchSettings(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName)))

fun openNotifications(ctx: Context) =
    launchSettings(ctx, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))

@SuppressLint("BatteryLife")
fun openBattery(ctx: Context) =
    launchSettings(ctx, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)))

/** Guía paso a paso para dejar la pausa funcionando. Cada paso abre directo el ajuste que hace falta. */
@Composable
fun PermissionGuide(pauseOn: Boolean, notifOn: Boolean, batteryOn: Boolean) {
    val ctx = LocalContext.current
    val restricted = Build.VERSION.SDK_INT >= 33
    var disclosure by remember { mutableStateOf(false) }
    if (disclosure) {
        // Aviso claro antes de pedir el permiso, como exige Google para el servicio de accesibilidad.
        AlertDialog(
            onDismissRequest = { disclosure = false },
            containerColor = C.Lino,
            title = { SerifText("Antes de activar la pausa", 22) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Body("Tranqui usa el servicio de accesibilidad de Android para una sola cosa: saber qué app se abre, y mostrar la pausa si es una de las que elegiste.", size = 15)
                    Body("No lee lo que hay en tu pantalla, no ve lo que escribís, no guarda tu actividad y no envía nada a internet.", size = 15)
                    Body("Lo podés apagar cuando quieras desde Ajustes de Android.", size = 15, color = C.Texto2)
                }
            },
            confirmButton = {
                TextButton(onClick = { disclosure = false; openAccessibility(ctx) }) { Text("Entendido, ir a Accesibilidad", color = C.Bosque) }
            },
            dismissButton = {
                TextButton(onClick = { disclosure = false }) { Text("Ahora no", color = C.Texto2) }
            }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Para que la pausa funcione")
        GuideStep(
            1, "Activá la pausa en Accesibilidad",
            "Buscá «Tranqui: pausa antes de abrir apps» en Aplicaciones instaladas y activalo." +
                if (restricted) " Si aparece «Configuración restringida», tocá Aceptar y seguí con el paso 2." else "",
            pauseOn, "Abrir Accesibilidad"
        ) { disclosure = true }
        if (restricted) {
            GuideStep(
                2, "Permitir configuración restringida",
                "Android la bloquea en apps instaladas fuera de la Play Store. En la pantalla que se abre, tocá ⋮ arriba a la derecha → «Permitir configuración restringida». Después volvé al paso 1.",
                pauseOn, "Abrir información de Tranqui"
            ) { openAppInfo(ctx) }
        }
        GuideStep(
            if (restricted) 3 else 2, "Permitir notificaciones",
            "Para el cronómetro, el aviso de mínimo cumplido y los recordatorios.",
            notifOn, "Abrir notificaciones"
        ) { openNotifications(ctx) }
        GuideStep(
            if (restricted) 4 else 3, "Batería sin restricciones",
            "Así Android no apaga la pausa ni los recordatorios para ahorrar batería. Tranqui casi no gasta.",
            batteryOn, "Quitar la restricción"
        ) { openBattery(ctx) }
    }
}

@Composable
private fun GuideStep(n: Int, title: String, text: String, done: Boolean, button: String, onClick: () -> Unit) {
    TCard(bg = if (done) C.SalviaClara else C.Papel, bordered = !done) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(if (done) C.Bosque else C.ArcillaClara),
                contentAlignment = Alignment.Center
            ) {
                if (done) Icon(Icons.Filled.Check, contentDescription = "Listo", tint = C.Lino, modifier = Modifier.size(16.dp))
                else Body("$n", color = C.ArcillaOscura, size = 14, bold = true)
            }
            Spacer(Modifier.width(12.dp))
            Body(title, Modifier.weight(1f), bold = true)
        }
        if (!done) {
            Spacer(Modifier.height(6.dp))
            Small(text)
            Spacer(Modifier.height(8.dp))
            SoftButton(button, onClick = onClick)
        }
    }
}

data class AppItem(val pkg: String, val label: String)

@Composable
fun SettingsScreen(nav: Nav) {
    val ctx = LocalContext.current
    Store.tick.intValue
    var enabled by remember { mutableStateOf(pauseServiceEnabled(ctx)) }
    var notifOn by remember { mutableStateOf(notificationsEnabled(ctx)) }
    var batteryOn by remember { mutableStateOf(batteryUnrestricted(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            enabled = pauseServiceEnabled(ctx)
            notifOn = notificationsEnabled(ctx)
            batteryOn = batteryUnrestricted(ctx)
            delay(1500)
        }
    }
    val apps by produceState(initialValue = emptyList<AppItem>()) {
        value = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(i, 0)
                .map { AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .filter { it.pkg != ctx.packageName }
                .distinctBy { it.pkg }
                .sortedWith(compareBy<AppItem>({ it.pkg !in SUGERIDAS }, { it.label.lowercase() }))
        }
    }
    val blocked = Store.blocked
    var window by remember { mutableFloatStateOf(Store.windowSeconds.toFloat()) }
    var speed by remember { mutableFloatStateOf(Store.speed.toFloat()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackBar("Ajustes") { nav.pop() }

        TCard(bg = if (enabled) C.SalviaClara else C.ArcillaClara, bordered = false) {
            Body(if (enabled) "La pausa está activa" else "La pausa está apagada", bold = true)
            Small(
                "Para mostrar la pausa antes de abrir una app, Android pide el permiso de accesibilidad. " +
                    "Tranqui solo mira qué app se abre; no lee ni guarda lo que hay en tu pantalla."
            )
        }
        PermissionGuide(enabled, notifOn, batteryOn)

        SectionLabel("Botón «Entrar igual»")
        Body("Desaparece a los ${window.roundToInt()} segundos")
        Slider(
            value = window, onValueChange = { window = it }, valueRange = 6f..20f, steps = 13,
            onValueChangeFinished = { Store.windowSeconds = window.roundToInt() },
            colors = SliderDefaults.colors(thumbColor = C.Bosque, activeTrackColor = C.Bosque, inactiveTrackColor = C.Arena)
        )
        Body("Velocidad del rebote: ${speed.roundToInt()}")
        Slider(
            value = speed, onValueChange = { speed = it }, valueRange = 40f..160f,
            onValueChangeFinished = { Store.speed = speed.roundToInt() },
            colors = SliderDefaults.colors(thumbColor = C.Bosque, activeTrackColor = C.Bosque, inactiveTrackColor = C.Arena)
        )

        SectionLabel("Apps que muestran la pausa")
        Small("WhatsApp, llamadas, mapas y apps del trabajo conviene dejarlas afuera.")
        if (apps.isEmpty()) Small("Cargando apps…")
        apps.forEach { app ->
            val checked = app.pkg in blocked
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable {
                    Store.blocked = if (checked) blocked - app.pkg else blocked + app.pkg
                }
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { Store.blocked = if (checked) blocked - app.pkg else blocked + app.pkg },
                    colors = CheckboxDefaults.colors(checkedColor = C.Bosque)
                )
                Column(Modifier.weight(1f)) {
                    Body(app.label)
                    if (app.pkg in SUGERIDAS) Small("Sugerida")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val version = remember {
            runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
        }
        Small("Tranqui v$version · Todo se guarda en tu teléfono, sin cuentas y sin internet.")
        Spacer(Modifier.height(16.dp))
    }
}
