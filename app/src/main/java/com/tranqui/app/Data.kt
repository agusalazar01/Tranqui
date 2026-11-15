package com.tranqui.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

data class Goal(
    val id: String,
    val name: String,
    val templateId: String,
    val minutes: Int,
    val anchor: String,
    val reminderHour: Int,      // -1 = sin recordatorio
    val reminderMinute: Int,
    val startDay: Long,
    val pleasure: String
) {
    fun json(): JSONObject = JSONObject().put("id", id).put("name", name).put("templateId", templateId)
        .put("minutes", minutes).put("anchor", anchor).put("rh", reminderHour).put("rm", reminderMinute)
        .put("startDay", startDay).put("pleasure", pleasure)

    companion object {
        fun from(o: JSONObject) = Goal(
            o.getString("id"), o.getString("name"), o.optString("templateId", "propio"),
            o.optInt("minutes", 5), o.optString("anchor", ""), o.optInt("rh", -1), o.optInt("rm", 0),
            o.optLong("startDay", LocalDate.now().toEpochDay()), o.optString("pleasure", "")
        )

        fun newId(): String = UUID.randomUUID().toString()
    }
}

data class Session(val day: Long, val goalId: String?, val label: String, val minutes: Int) {
    fun json(): JSONObject = JSONObject().put("day", day).put("goalId", goalId ?: "").put("label", label).put("minutes", minutes)

    companion object {
        fun from(o: JSONObject) = Session(
            o.getLong("day"), o.optString("goalId", "").ifEmpty { null }, o.optString("label", ""), o.optInt("minutes", 0)
        )
    }
}

data class Interception(val day: Long, val pkg: String, val choice: String) {
    fun json(): JSONObject = JSONObject().put("day", day).put("pkg", pkg).put("choice", choice)

    companion object {
        fun from(o: JSONObject) = Interception(o.getLong("day"), o.optString("pkg"), o.optString("choice"))
    }
}

/** Actividad en curso. Solo se guardan marcas de tiempo: no hay contador corriendo. */
data class Active(
    val label: String,
    val goalId: String?,
    val minMinutes: Int,
    val startedAt: Long,
    val pausedAt: Long,
    val pausedTotal: Long
) {
    val paused: Boolean get() = pausedAt > 0

    fun elapsed(now: Long): Long = ((if (paused) pausedAt else now) - startedAt - pausedTotal).coerceAtLeast(0)

    fun json(): JSONObject = JSONObject().put("label", label).put("goalId", goalId ?: "").put("min", minMinutes)
        .put("startedAt", startedAt).put("pausedAt", pausedAt).put("pausedTotal", pausedTotal)

    companion object {
        fun from(o: JSONObject) = Active(
            o.optString("label"), o.optString("goalId", "").ifEmpty { null }, o.optInt("min", 5),
            o.getLong("startedAt"), o.optLong("pausedAt", 0), o.optLong("pausedTotal", 0)
        )
    }
}

data class Note(val day: Long, val fields: List<Pair<String, String>>)

data class KTask(val id: String, val text: String, val col: Int)

/** Todo se guarda en el teléfono, sin cuentas ni internet. */
object Store {
    private lateinit var p: SharedPreferences
    val tick = mutableIntStateOf(0)

    fun init(ctx: Context) {
        if (!::p.isInitialized) p = ctx.applicationContext.getSharedPreferences("tranqui", Context.MODE_PRIVATE)
    }

    private fun bump() {
        tick.intValue = tick.intValue + 1
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    private fun arr(key: String): List<JSONObject> {
        val s = p.getString(key, null) ?: return emptyList()
        return try {
            val a = JSONArray(s)
            (0 until a.length()).map { a.getJSONObject(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun putArr(key: String, list: List<JSONObject>) {
        val a = JSONArray()
        list.forEach { a.put(it) }
        p.edit().putString(key, a.toString()).apply()
        bump()
    }

    // Objetivos
    fun goals(): List<Goal> = arr("goals").mapNotNull { runCatching { Goal.from(it) }.getOrNull() }
    fun goal(id: String?): Goal? = if (id == null) null else goals().find { it.id == id }
    fun saveGoal(g: Goal) {
        val list = goals().toMutableList()
        val i = list.indexOfFirst { it.id == g.id }
        if (i >= 0) list[i] = g else list.add(g)
        putArr("goals", list.map { it.json() })
    }
    fun deleteGoal(id: String) = putArr("goals", goals().filter { it.id != id }.map { it.json() })

    // Sesiones
    fun sessions(): List<Session> = arr("sessions").mapNotNull { runCatching { Session.from(it) }.getOrNull() }
    fun addSession(s: Session) = putArr("sessions", (sessions() + s).takeLast(5000).map { it.json() })

    // Pausas
    fun interceptions(): List<Interception> = arr("intercepts").mapNotNull { runCatching { Interception.from(it) }.getOrNull() }
    fun logInterception(pkg: String, choice: String) =
        putArr("intercepts", (interceptions() + Interception(today(), pkg, choice)).takeLast(3000).map { it.json() })

    // Ajustes
    var blocked: Set<String>
        get() = p.getStringSet("blocked", emptySet())?.toSet() ?: emptySet()
        set(v) {
            p.edit().putStringSet("blocked", v).apply(); bump()
        }
    var windowSeconds: Int
        get() = p.getInt("window", 14)
        set(v) {
            p.edit().putInt("window", v).apply(); bump()
        }
    var speed: Int
        get() = p.getInt("speed", 105)
        set(v) {
            p.edit().putInt("speed", v).apply(); bump()
        }

    // Permiso temporal después de "Entrar igual"
    fun allow(pkg: String, minutes: Int) {
        p.edit().putLong("allow_$pkg", System.currentTimeMillis() + minutes * 60_000L).apply()
    }
    fun isAllowed(pkg: String): Boolean = System.currentTimeMillis() < p.getLong("allow_$pkg", 0)

    // Actividad en curso
    fun active(): Active? {
        val s = p.getString("active", null) ?: return null
        return runCatching { Active.from(JSONObject(s)) }.getOrNull()
    }
    fun setActive(a: Active?) {
        if (a == null) p.edit().remove("active").apply() else p.edit().putString("active", a.json().toString()).apply()
        bump()
    }
    fun pause() {
        val a = active() ?: return
        if (!a.paused) setActive(a.copy(pausedAt = System.currentTimeMillis()))
    }
    fun resume() {
        val a = active() ?: return
        if (a.paused) setActive(a.copy(pausedTotal = a.pausedTotal + System.currentTimeMillis() - a.pausedAt, pausedAt = 0))
    }

    // Sapo del día
    fun sapo(): String? = if (p.getLong("sapo_day", -1) == today()) p.getString("sapo", null) else null
    fun setSapo(text: String) {
        p.edit().putString("sapo", text).putLong("sapo_day", today()).apply(); bump()
    }

    // Notas: WOOP, Hansei, Ikigai
    fun notes(kind: String): List<Note> = arr("notes_$kind").map { o ->
        val f = o.getJSONArray("f")
        Note(o.getLong("day"), (0 until f.length()).map { val x = f.getJSONObject(it); x.optString("k") to x.optString("v") })
    }
    fun addNote(kind: String, fields: List<Pair<String, String>>) {
        val o = JSONObject().put("day", today())
        val f = JSONArray()
        fields.forEach { f.put(JSONObject().put("k", it.first).put("v", it.second)) }
        o.put("f", f)
        putArr("notes_$kind", (listOf(o) + arr("notes_$kind")).take(50))
    }

    // Kanban
    fun tasks(): List<KTask> = arr("kanban").map { KTask(it.getString("id"), it.getString("t"), it.getInt("c")) }
    fun saveTasks(list: List<KTask>) = putArr("kanban", list.map { JSONObject().put("id", it.id).put("t", it.text).put("c", it.col) })

    // Sugerencias de nivel
    fun lastSuggest(goalId: String): Long = p.getLong("sug_$goalId", -100)
    fun setLastSuggest(goalId: String) {
        p.edit().putLong("sug_$goalId", today()).apply(); bump()
    }
}

/** Cálculos de progreso. Se mide constancia, no rachas que vuelven a cero. */
object Stats {
    fun doneDays(goalId: String? = null): Set<Long> =
        Store.sessions().filter { goalId == null || it.goalId == goalId }.map { it.day }.toSet()

    fun doneToday(goalId: String): Boolean = Store.sessions().any { it.day == Store.today() && it.goalId == goalId }

    /** Lunes a domingo de esta semana: (letra, cumplido). */
    fun week(): List<Pair<String, Boolean>> {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        val done = doneDays()
        val letters = listOf("L", "M", "M", "J", "V", "S", "D")
        return (0..6).map { letters[it] to (monday.plusDays(it.toLong()).toEpochDay() in done) }
    }

    fun weekCount(): Int = week().count { it.second }

    fun goalDays(g: Goal): Int = doneDays(g.id).count { it >= g.startDay }

    fun last7(goalId: String): Int {
        val t = Store.today()
        val d = doneDays(goalId)
        return (0..6).count { (t - it) in d }
    }

    fun monthMinutes(goalId: String? = null): Int {
        val start = LocalDate.now().withDayOfMonth(1).toEpochDay()
        return Store.sessions().filter { it.day >= start && (goalId == null || it.goalId == goalId) }.sumOf { it.minutes }
    }

    fun monthNoEntrar(): Int {
        val start = LocalDate.now().withDayOfMonth(1).toEpochDay()
        return Store.interceptions().count { it.day >= start && it.choice != "entro" }
    }

    /** Propone subir si cumpliste 6 de 7 días, o bajar si cumpliste menos de 3. */
    fun suggestion(g: Goal): Int? {
        val t = Store.today()
        if (t - Store.lastSuggest(g.id) < 7) return null
        val age = t - g.startDay
        val n = last7(g.id)
        val step = TEMPLATES.find { it.id == g.templateId }?.step ?: 5
        if (age >= 6 && n >= 6) return g.minutes + step
        if (age >= 7 && n < 3 && g.minutes > 2) return maxOf(2, g.minutes - step)
        return null
    }

    /** Ayer no hubo nada y hoy todavía tampoco: mensaje amable. */
    fun missedYesterday(): Boolean {
        val goals = Store.goals()
        if (goals.isEmpty()) return false
        val t = Store.today()
        if (goals.minOf { it.startDay } > t - 1) return false
        val d = doneDays()
        return (t - 1) !in d && t !in d
    }
}
