package com.example.service

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
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.core.AudioEnforcer
import com.example.core.ProtectionStateHolder
import com.example.data.InterceptionLog
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class NeverSilentService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var monitorJob: Job? = null
    private lateinit var audioManager: AudioManager
    private lateinit var prefsManager: PreferencesManager

    private var isReceiverRegistered = false
    private var countdownStartTime: Long? = null
    private var violationReason: String = ""

    private val audioStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkAndEnforceAudioState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = AudioEnforcer.getAudioManager(this)
        prefsManager = PreferencesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                prefsManager.setProtectionEnabled(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CHECK_NOW -> {
                checkAndEnforceAudioState()
            }
            else -> {
                startForegroundServiceNotification()
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(this, NeverSilentService::class.java).apply {
            action = ACTION_STOP
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Never Silent Guardian Active")
            .setContentText("Monitoring audio state. Auto-restores ringtone in 3s.")
            .setSmallIcon(applicationInfo.icon)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Turn Off",
                pausePendingIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        ProtectionStateHolder.setServiceActive(true)
    }

    private fun startMonitoring() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction("android.media.VOLUME_CHANGED_ACTION")
            }
            registerReceiver(audioStateReceiver, filter)
            isReceiverRegistered = true
        }

        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                checkAndEnforceAudioState()
                delay(200) // High-precision 200ms loop
            }
        }
    }

    private fun checkAndEnforceAudioState() {
        if (!prefsManager.isProtectionEnabled.value) {
            countdownStartTime = null
            ProtectionStateHolder.setCountdown(null)
            return
        }

        val currentMode = audioManager.ringerMode
        val ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)

        ProtectionStateHolder.updateAudioState(currentMode, ringVol, maxVol)

        val isMuted = AudioEnforcer.isMutedOrSilent(audioManager)

        if (isMuted) {
            val now = System.currentTimeMillis()
            if (countdownStartTime == null) {
                countdownStartTime = now
                violationReason = AudioEnforcer.getTriggerReason(audioManager)
                Log.d(TAG, "Mute/Silent detected ($violationReason). Starting 3s countdown.")
            }

            val elapsedMs = now - (countdownStartTime ?: now)
            val remainingSeconds = ((3000L - elapsedMs) / 1000f).coerceAtLeast(0f)

            if (remainingSeconds > 0f) {
                ProtectionStateHolder.setCountdown(remainingSeconds)
            } else {
                // 3 seconds elapsed! Auto-restore now!
                executeRestoration(elapsedMs)
            }
        } else {
            if (countdownStartTime != null) {
                Log.d(TAG, "User manually unmuted or volume restored. Resetting countdown.")
                countdownStartTime = null
                ProtectionStateHolder.setCountdown(null)
            }
        }
    }

    private fun executeRestoration(elapsedMs: Long) {
        // Enforce 100% maximum volume whenever sound is restored
        val targetPercent = 100
        val playChime = prefsManager.soundChimeEnabled.value
        val vibrate = prefsManager.vibrateEnabled.value

        val restored = AudioEnforcer.restoreRingtone(
            context = this,
            targetVolumePercent = targetPercent,
            playChime = playChime,
            vibrate = vibrate
        )

        val restoredVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val actualPercent = if (maxVol > 0) (restoredVol * 100) / maxVol else targetPercent

        val log = InterceptionLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            triggerReason = violationReason.ifEmpty { "Silent mode auto-override" },
            timeToRestoreSeconds = (elapsedMs / 1000f).coerceIn(0.5f, 3.2f),
            restoredVolumePercent = actualPercent
        )
        prefsManager.addInterceptionLog(log)

        countdownStartTime = null
        ProtectionStateHolder.notifyRestored()
        ProtectionStateHolder.updateAudioState(audioManager.ringerMode, restoredVol, maxVol)

        Log.d(TAG, "Restored ringtone! Success: $restored, Volume: $restoredVol/$maxVol")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Never Silent Guardian Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps ringtone protection active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(audioStateReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
        ProtectionStateHolder.setServiceActive(false)
        ProtectionStateHolder.setCountdown(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "NeverSilentService"
        private const val CHANNEL_ID = "never_silent_guardian_channel"
        private const val NOTIFICATION_ID = 4242

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_CHECK_NOW = "com.example.service.ACTION_CHECK_NOW"

        fun start(context: Context) {
            val intent = Intent(context, NeverSilentService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, NeverSilentService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerCheck(context: Context) {
            val intent = Intent(context, NeverSilentService::class.java).apply {
                action = ACTION_CHECK_NOW
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}
