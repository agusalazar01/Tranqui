package com.tranqui.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

class MainActivity : ComponentActivity() {
    private val incoming = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        Notif.createChannels(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        incoming.value = intent
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent { TranquiTheme { App(incoming) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incoming.value = intent
    }
}

sealed interface Route {
    data object Activity : Route
    data class GoalEdit(val id: String?) : Route
    data class MethodDetail(val id: String) : Route
    data class Notes(val kind: String) : Route
    data object Kanban : Route
    data class Tool(val kind: String) : Route
    data class Situation(val id: String) : Route
    data class PracticeGroupRoute(val id: String) : Route
    data object FeelingRest : Route
    data class PracticeRun(val id: String, val minutes: Int? = null) : Route
    data object Settings : Route
    data object VoiceRecord : Route
}

class Nav(
    val push: (Route) -> Unit,
    val pop: () -> Unit,
    val tab: (Int) -> Unit,
    val quick: (Int) -> Unit,
    val sapo: () -> Unit,
    val start: (label: String, goalId: String?, minutes: Int) -> Unit
)

@Composable
fun App(incoming: MutableState<Intent?>) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Route>() }
    var quick by remember { mutableStateOf<Int?>(null) }
    var sapoDialog by remember { mutableStateOf(false) }

    fun startMode(label: String, goalId: String?, minutes: Int) {
        val current = Store.active()
        if (current == null) startActivityMode(ctx, label, goalId, minutes)
        else if (current.label != label) {
            Toast.makeText(ctx, "Ya tenés en curso: ${current.label}. Terminala para empezar otra.", Toast.LENGTH_LONG).show()
        }
        if (stack.lastOrNull() != Route.Activity) stack.add(Route.Activity)
    }

    val nav = remember {
        Nav(
            push = { stack.add(it) },
            pop = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) },
            tab = { tab = it; stack.clear() },
            quick = { quick = it },
            sapo = { sapoDialog = true },
            start = { l, g, m -> startMode(l, g, m) }
        )
    }

    LaunchedEffect(incoming.value) {
        val i = incoming.value ?: return@LaunchedEffect
        val q = i.getIntExtra("quick", 0)
        if (q > 0) quick = q
        // Desde la pausa: «¿Cómo te sentís?»
        when (i.getStringExtra("feeling")) {
            "nervios" -> { stack.clear(); stack.add(Route.PracticeRun("suspiro", 1)) }
            "aburrido" -> { stack.clear(); quick = 5 }
            "cansado" -> { stack.clear(); stack.add(Route.FeelingRest) }
            "evito" -> { stack.clear(); stack.add(Route.Situation("arrancar")) }
        }
        i.getStringExtra("start_goal")?.let { id -> Store.goal(id)?.let { g -> startMode(g.name, g.id, g.minutes) } }
        if (i.getBooleanExtra("open_activity", false) && Store.active() != null && stack.lastOrNull() != Route.Activity) {
            stack.add(Route.Activity)
        }
        incoming.value = null
    }

    // Atrás: primero cierra la pantalla abierta, después vuelve a Hoy; recién en Hoy sale de la app.
    BackHandler(enabled = stack.isNotEmpty() || tab != 0) {
        if (stack.isNotEmpty()) nav.pop() else tab = 0
    }

    Box(Modifier.fillMaxSize().background(C.Lino)) {
        when (val top = stack.lastOrNull()) {
            null -> Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> HoyScreen(nav)
                        1 -> CalmaScreen(nav)
                        2 -> MetodosScreen(nav)
                        else -> ProgresoScreen(nav)
                    }
                }
                BottomBar(tab) { tab = it }
            }
            else -> Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { Detail(top, nav) }
        }

        quick?.let { size ->
            QuickPicker(size, onDismiss = { quick = null }) { opt ->
                quick = null
                if (opt.practice != null) stack.add(Route.PracticeRun(opt.practice, opt.minutes))
                else startMode(opt.label, opt.goalId, opt.minutes)
            }
        }
        if (sapoDialog) SapoDialog(
            onClose = { sapoDialog = false },
            onStart = { step ->
                sapoDialog = false
                startMode(step, null, 2)
            }
        )
    }
}

@Composable
private fun Detail(top: Route, nav: Nav) {
    when (top) {
        Route.Activity -> ActivityScreen(nav)
        is Route.GoalEdit -> GoalEditScreen(top.id, nav)
        is Route.MethodDetail -> MethodScreen(top.id, nav)
        is Route.Notes -> NotesScreen(top.kind, nav)
        Route.Kanban -> KanbanScreen(nav)
        is Route.Tool -> ToolScreen(top.kind, nav)
        is Route.Situation -> SituacionScreen(top.id, nav)
        is Route.PracticeGroupRoute -> PracticeGroupScreen(top.id, nav)
        Route.FeelingRest -> DescansoScreen(nav)
        is Route.PracticeRun -> PracticeScreen(top.id, nav, top.minutes)
        Route.Settings -> SettingsScreen(nav)
        Route.VoiceRecord -> VoiceRecordScreen(nav)
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items: List<Pair<String, ImageVector>> = listOf(
        "Hoy" to Icons.Outlined.Home,
        "Calma" to Icons.Outlined.FavoriteBorder,
        "Métodos" to Icons.Outlined.Info,
        "Progreso" to Icons.Outlined.DateRange
    )
    NavigationBar(containerColor = C.Papel.copy(alpha = 0.97f), tonalElevation = 0.dp) {
        items.forEachIndexed { i, (label, icon) ->
            NavigationBarItem(
                selected = selected == i,
                onClick = { onSelect(i) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label, fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = C.Bosque, selectedTextColor = C.Bosque,
                    unselectedIconColor = C.Texto2, unselectedTextColor = C.Texto2,
                    indicatorColor = C.SalviaClara.copy(alpha = 0.9f)
                )
            )
        }
    }
}

@Composable
fun QuickPicker(size: Int, onDismiss: () -> Unit, onPick: (QuickOption) -> Unit) {
    Store.tick.intValue
    val options = quickOptions(size, Store.goals())
    val title = when (size) {
        1 -> "1 minuto, en cualquier lugar"
        5 -> "5 minutos"
        else -> "15 minutos o más"
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(C.Lino).padding(20.dp)
                .heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SerifText(title, 24)
            Small("Elegí lo que puedas hacer ahora. Cualquier opción suma a tu día.")
            options.forEach { o ->
                TCard(onClick = { onPick(o) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Body(o.label, bold = true)
                            Small(if (o.note.isNotBlank()) "${o.minutes} min · ${o.note}" else "${o.minutes} min")
                        }
                        Chevron()
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Ahora no", color = C.Texto2)
            }
        }
    }
}

@Composable
fun SapoDialog(onClose: () -> Unit, onStart: (String) -> Unit) {
    var text by remember { mutableStateOf(Store.sapo() ?: "") }
    var step by remember { mutableStateOf(Store.sapoStep() ?: "") }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(C.Lino).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SerifText("Destrabá tu tarea", 24)
            Small("No hace falta resolverla entera. Vamos a encontrar una primera acción tan pequeña que puedas empezar ahora.")
            OutlinedTextField(
                value = text, onValueChange = { text = it }, label = { Text("¿Qué estás postergando?") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            OutlinedTextField(
                value = step, onValueChange = { step = it }, label = { Text("¿Cuál sería el primer paso de 2 minutos?") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            Small("Ejemplo: en vez de «ordenar toda la casa», empezar por guardar cinco objetos.")
            PrimaryButton("Guardar mi pequeño paso") {
                if (text.isNotBlank()) Store.setSapo(text.trim())
                if (step.isNotBlank()) Store.setSapoStep(step.trim())
                onClose()
            }
            if (step.isNotBlank()) SoftButton("Empezar este paso · 2 min", Modifier.fillMaxWidth()) {
                if (text.isNotBlank()) Store.setSapo(text.trim())
                Store.setSapoStep(step.trim())
                onStart(step.trim())
            }
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text("Ahora no", color = C.Texto2)
            }
        }
    }
}

fun appLabel(ctx: Context, pkg: String): String = try {
    ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString()
} catch (e: Exception) {
    pkg
}
