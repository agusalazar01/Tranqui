package com.tranqui.app

import android.content.Context
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

data class AppItem(val pkg: String, val label: String)

@Composable
fun SettingsScreen(nav: Nav) {
    val ctx = LocalContext.current
    Store.tick.intValue
    var enabled by remember { mutableStateOf(pauseServiceEnabled(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            enabled = pauseServiceEnabled(ctx)
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
            if (!enabled) {
                Spacer(Modifier.height(6.dp))
                SoftButton("Activar en Ajustes de Android") {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                Small("Buscá «Tranqui: pausa antes de abrir apps» y activalo.")
            }
        }

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
        Small("Tranqui v0.1 · Todo se guarda en tu teléfono, sin cuentas ni internet.")
        Spacer(Modifier.height(16.dp))
    }
}
