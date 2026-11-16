package com.tranqui.app

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Audio engine separated from the Compose/WebView screen. MediaSessionService keeps
 * playback alive across navigation, app backgrounding and screen lock.
 * Nature recordings are CC0 excerpts documented in assets/soundscape/CREDITS.md.
 */
@OptIn(androidx.media3.common.util.UnstableApi::class)
class SoundPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true
            )
            repeatMode = Player.REPEAT_MODE_ONE
            setWakeMode(C.WAKE_MODE_LOCAL)
            setHandleAudioBecomingNoisy(true)
            volume = prefs.getFloat(KEY_VOLUME, 0.62f)
            playbackParameters = PlaybackParameters(1f, pitchFactor(prefs.getInt(KEY_PITCH, 0)))
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    prefs.edit().putBoolean(KEY_PLAYING, isPlaying).apply()
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    prefs.edit().putBoolean(KEY_PLAYING, false)
                        .putString(KEY_ERROR, error.errorCodeName)
                        .apply()
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val id = mediaItem?.mediaMetadata?.extras?.getString("tranqui_sound_id")
                    if (id != null) prefs.edit().putString(KEY_SELECTED, id).apply()
                }
            })
        }
        mediaSession = MediaSession.Builder(this, player).build()
        mediaSession?.let(::addSession)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY -> playSound(intent.getStringExtra(EXTRA_SOUND) ?: selectedSound())
            ACTION_PAUSE -> player.pause()
            ACTION_STOP -> {
                cancelTimer()
                player.stop()
                player.clearMediaItems()
                prefs.edit().putBoolean(KEY_PLAYING, false).putInt(KEY_TIMER, 0).apply()
                stopSelf()
            }
            ACTION_VOLUME -> {
                val value = intent.getFloatExtra(EXTRA_VOLUME, 0.62f).coerceIn(0f, 1f)
                player.volume = value
                prefs.edit().putFloat(KEY_VOLUME, value).apply()
                if (player.playbackState == Player.STATE_IDLE) stopSelf()
            }
            ACTION_PITCH -> {
                val semis = intent.getIntExtra(EXTRA_PITCH, 0).coerceIn(-6, 6)
                prefs.edit().putInt(KEY_PITCH, semis).apply()
                player.playbackParameters = PlaybackParameters(1f, pitchFactor(semis))
                if (player.playbackState == Player.STATE_IDLE) stopSelf()
            }
            ACTION_TIMER -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0).coerceIn(0, 180)
                prefs.edit().putInt(KEY_TIMER, minutes).apply()
                cancelTimer()
                if (minutes > 0 && player.isPlaying) scheduleTimer(minutes)
                if (player.playbackState == Player.STATE_IDLE) stopSelf()
            }
        }
        return result
    }

    private fun playSound(requested: String) {
        // Las grabaciones de la app suenan sin internet. Las de «En línea» se escuchan por
        // internet y no se guardan: ExoPlayer las lee directo, sin caché en disco.
        val online = onlineUrl(this, requested)
        val id = if (assetExists(requested) || online != null) requested else DEFAULT
        cancelTimer()
        val timerMinutes = prefs.getInt(KEY_TIMER, 0)
        prefs.edit().putString(KEY_SELECTED, id).remove(KEY_ERROR).apply()
        val metadata = MediaMetadata.Builder()
            .setTitle(titleOf(this, id))
            .setArtist("Tranqui · Paisajes sonoros")
            .setExtras(android.os.Bundle().apply { putString("tranqui_sound_id", id) })
            .build()
        val uri = online ?: "asset:///soundscape/sounds/$id.ogg"
        val item = MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).build()
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.setMediaItem(item)
        player.prepare()
        player.play()
        if (timerMinutes > 0) scheduleTimer(timerMinutes)
    }

    private fun assetExists(id: String): Boolean =
        runCatching { assets.list("soundscape/sounds")?.contains("$id.ogg") == true }.getOrDefault(false)

    private fun scheduleTimer(minutes: Int) {
        cancelTimer()
        timerRunnable = Runnable {
            player.pause()
            prefs.edit().putBoolean(KEY_PLAYING, false).putInt(KEY_TIMER, 0).apply()
        }.also { handler.postDelayed(it, minutes * 60_000L) }
    }

    private fun selectedSound(): String = prefs.getString(KEY_SELECTED, DEFAULT) ?: DEFAULT

    private fun cancelTimer() {
        timerRunnable?.let(handler::removeCallbacks)
        timerRunnable = null
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        cancelTimer()
        prefs.edit().putBoolean(KEY_PLAYING, false).apply()
        mediaSession?.release()
        player.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val PREFS = "tranqui_sound_player"
        const val KEY_SELECTED = "selected_sound"
        const val KEY_PLAYING = "is_playing"
        const val KEY_VOLUME = "volume"
        const val KEY_TIMER = "timer_minutes"
        const val KEY_PITCH = "pitch_semitones"
        const val KEY_ERROR = "last_error"
        const val EXTRA_SOUND = "sound_id"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_PITCH = "pitch"
        const val ACTION_PLAY = "com.tranqui.app.sound.PLAY"
        const val ACTION_PAUSE = "com.tranqui.app.sound.PAUSE"
        const val ACTION_STOP = "com.tranqui.app.sound.STOP"
        const val ACTION_VOLUME = "com.tranqui.app.sound.VOLUME"
        const val ACTION_TIMER = "com.tranqui.app.sound.TIMER"
        const val ACTION_PITCH = "com.tranqui.app.sound.PITCH"
        const val DEFAULT = "arroyo"

        /** Semitonos a factor de tono: +12 es una octava arriba. La velocidad no cambia. */
        fun pitchFactor(semis: Int): Float = Math.pow(2.0, semis / 12.0).toFloat()

        private var titles: Map<String, String>? = null
        private var urls: Map<String, String>? = null

        private fun loadCatalog(ctx: android.content.Context) {
            if (titles != null) return
            runCatching {
                val json = org.json.JSONObject(ctx.assets.open("soundscape/catalogo.json").bufferedReader().use { it.readText() })
                val arr = json.getJSONArray("sounds")
                val t = mutableMapOf<String, String>()
                val u = mutableMapOf<String, String>()
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    t[s.getString("id")] = s.getString("title")
                    val src = s.optJSONObject("source")
                    if (src != null && src.optString("type") == "online") u[s.getString("id")] = src.getString("url")
                }
                titles = t; urls = u
            }.onFailure { titles = emptyMap(); urls = emptyMap() }
        }

        /** Nombre para la notificación, tomado del catálogo de sonidos. */
        fun titleOf(ctx: android.content.Context, id: String): String {
            loadCatalog(ctx)
            return titles?.get(id) ?: id.replace('-', ' ').replaceFirstChar { it.uppercase() }
        }

        /** Dirección web de un sonido «En línea», o null si está dentro de la app. */
        fun onlineUrl(ctx: android.content.Context, id: String): String? {
            loadCatalog(ctx)
            return urls?.get(id)
        }
    }
}
