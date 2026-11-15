package com.tranqui.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import kotlin.math.roundToInt

// ---------- Modo actividad ----------

fun startActivityMode(ctx: Context, label: String, goalId: String?, minMinutes: Int) {
    Store.init(ctx)
    Store.setActive(Active(label, goalId, minMinutes, System.currentTimeMillis(), 0L, 0L))
    ContextCompat.startForegroundService(ctx, Intent(ctx, TimerService::class.java).setAction(TimerService.START))
    scheduleMinAlarm(ctx)
}

fun pauseActivityMode(ctx: Context) {
    Store.pause()
    cancelMinAlarm(ctx)
    ctx.startService(Intent(ctx, TimerService::class.java).setAction(TimerService.UPDATE))
}

fun resumeActivityMode(ctx: Context) {
    Store.resume()
    scheduleMinAlarm(ctx)
    ctx.startService(Intent(ctx, TimerService::class.java).setAction(TimerService.UPDATE))
}

/** Termina la actividad, guarda los minutos y devuelve cuántos fueron. */
fun finishActivityMode(ctx: Context): Int {
    val a = Store.active() ?: return 0
    val mins = maxOf(1, (a.elapsed(System.currentTimeMillis()) / 60000.0).roundToInt())
    Store.addSession(Session(Store.today(), a.goalId, a.label, mins))
    Store.setActive(null)
    cancelMinAlarm(ctx)
    NotificationManagerCompat.from(ctx).cancel(Notif.PROMPT_ID)
    ctx.stopService(Intent(ctx, TimerService::class.java))
    return mins
}

private fun minAlarmIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
    ctx, 7, Intent(ctx, MinReachedReceiver::class.java),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
)

fun scheduleMinAlarm(ctx: Context) {
    val a = Store.active() ?: return
    if (a.paused) return
    val remaining = a.minMinutes * 60_000L - a.elapsed(System.currentTimeMillis())
    if (remaining <= 0) return
    val am = ctx.getSystemService(AlarmManager::class.java)
    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + remaining, minAlarmIntent(ctx))
}

fun cancelMinAlarm(ctx: Context) {
    ctx.getSystemService(AlarmManager::class.java).cancel(minAlarmIntent(ctx))
}

fun buzz(ctx: Context, ms: Long) {
    try {
        @Suppress("DEPRECATION")
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {
    }
}

/**
 * Servicio liviano: solo muestra la notificación con el cronómetro del sistema
 * y escucha cuando desbloqueás el teléfono para avisarte cuánto llevás.
 */
class TimerService : Service() {
    companion object {
        const val START = "start"
        const val UPDATE = "update"
        const val PAUSE = "pause"
        const val RESUME = "resume"
        const val ID = 11
    }

    private var registered = false

    private val presentReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val a = Store.active() ?: return
            if (a.paused) return
            Notif.unlockPrompt(c, a)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Store.init(this)
        when (intent?.action) {
            PAUSE -> { Store.pause(); cancelMinAlarm(this); NotificationManagerCompat.from(this).cancel(Notif.PROMPT_ID) }
            RESUME -> { Store.resume(); scheduleMinAlarm(this) }
        }
        val a = Store.active()
        if (a == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val n = Notif.ongoing(this, a)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID, n)
        }
        if (!registered) {
            ContextCompat.registerReceiver(this, presentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_EXPORTED)
            registered = true
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (registered) {
            try { unregisterReceiver(presentReceiver) } catch (_: Exception) { }
            registered = false
        }
        super.onDestroy()
    }
}

// ---------- Notificaciones ----------

object Notif {
    const val CH_ACT = "actividad"
    const val CH_AVISO = "avisos"
    const val CH_REC = "recordatorios"
    const val PROMPT_ID = 12
    const val MIN_ID = 13

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_ACT, "Actividad en curso", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_AVISO, "Avisos al volver al celular", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_REC, "Recordatorios de objetivos", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun openApp(ctx: Context, code: Int, extras: Intent.() -> Unit): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        i.extras()
        return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun serviceAction(ctx: Context, action: String, code: Int): PendingIntent = PendingIntent.getService(
        ctx, code, Intent(ctx, TimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun ongoing(ctx: Context, a: Active): Notification {
        val now = System.currentTimeMillis()
        val el = a.elapsed(now)
        val b = NotificationCompat.Builder(ctx, CH_ACT)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(a.label)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(0xFF344C40.toInt())
            .setContentIntent(openApp(ctx, 21) { putExtra("open_activity", true) })
        if (!a.paused) {
            b.setUsesChronometer(true).setShowWhen(true).setWhen(now - el)
                .setContentText("En curso · mínimo ${a.minMinutes} min")
                .addAction(0, "Pausar", serviceAction(ctx, TimerService.PAUSE, 22))
        } else {
            b.setUsesChronometer(false).setShowWhen(false)
                .setContentText("En pausa · ${el / 60000} min")
                .addAction(0, "Seguir", serviceAction(ctx, TimerService.RESUME, 23))
        }
        b.addAction(0, "Terminar", openApp(ctx, 24) { putExtra("open_activity", true) })
        return b.build()
    }

    private fun canNotify(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun unlockPrompt(ctx: Context, a: Active) {
        if (!canNotify(ctx)) return
        val mins = a.elapsed(System.currentTimeMillis()) / 60000
        val n = NotificationCompat.Builder(ctx, CH_AVISO)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Llevás $mins min: ${a.label}")
            .setContentText("Si seguís, ignorá este aviso.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(openApp(ctx, 25) { putExtra("open_activity", true) })
            .addAction(0, "Pausar", serviceAction(ctx, TimerService.PAUSE, 26))
            .addAction(0, "Terminar", openApp(ctx, 27) { putExtra("open_activity", true) })
            .build()
        try { NotificationManagerCompat.from(ctx).notify(PROMPT_ID, n) } catch (_: SecurityException) { }
    }

    fun simple(ctx: Context, id: Int, channel: String, title: String, text: String, pi: PendingIntent) {
        if (!canNotify(ctx)) return
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) { }
    }
}

/** Avisa con una vibración suave cuando llegás al mínimo. El cronómetro sigue. */
class MinReachedReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        val a = Store.active() ?: return
        if (a.paused) return
        buzz(ctx, 350)
        Notif.simple(
            ctx, Notif.MIN_ID, Notif.CH_AVISO, "Mínimo cumplido: ${a.minMinutes} min",
            "Podés seguir todo lo que quieras.", Notif.openApp(ctx, 28) { putExtra("open_activity", true) }
        )
    }
}

// ---------- Recordatorios ----------

object Reminders {
    private fun pi(ctx: Context, id: String): PendingIntent = PendingIntent.getBroadcast(
        ctx, id.hashCode(), Intent(ctx, ReminderReceiver::class.java).putExtra("goal", id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun schedule(ctx: Context, g: Goal) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val p = pi(ctx, g.id)
        am.cancel(p)
        if (g.reminderHour < 0) return
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, g.reminderHour)
            set(Calendar.MINUTE, g.reminderMinute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.timeInMillis, AlarmManager.INTERVAL_DAY, p)
    }

    fun cancel(ctx: Context, id: String) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pi(ctx, id))
    }

    fun scheduleAll(ctx: Context) = Store.goals().forEach { schedule(ctx, it) }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        val g = Store.goal(intent.getStringExtra("goal")) ?: return
        if (Stats.doneToday(g.id)) return
        val text = if (g.anchor.isNotBlank()) "Después de ${g.anchor}. Tocá para empezar." else "Tocá para empezar."
        Notif.simple(
            ctx, 100 + (g.id.hashCode() and 0xFFF), Notif.CH_REC, "¿Tus ${g.minutes} minutos? ${g.name}", text,
            Notif.openApp(ctx, 200 + (g.id.hashCode() and 0xFFF)) { putExtra("start_goal", g.id) }
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Store.init(ctx)
        Reminders.scheduleAll(ctx)
        Store.active()?.let { if (!it.paused) ContextCompat.startForegroundService(ctx, Intent(ctx, TimerService::class.java).setAction(TimerService.START)) }
    }
}
