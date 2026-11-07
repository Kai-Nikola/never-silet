package com.example.core

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object AudioEnforcer {

    private const val TAG = "AudioEnforcer"

    fun isDndAccessGranted(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.isNotificationPolicyAccessGranted == true
    }

    fun getAudioManager(context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun isMutedOrSilent(audioManager: AudioManager): Boolean {
        val mode = audioManager.ringerMode
        val ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        return mode != AudioManager.RINGER_MODE_NORMAL || ringVol == 0
    }

    fun getTriggerReason(audioManager: AudioManager): String {
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent mode detected"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate-only mode detected"
            else -> {
                val vol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                if (vol == 0) "Ring volume muted to 0" else "Unknown audio mute"
            }
        }
    }

    fun restoreRingtone(
        context: Context,
        targetVolumePercent: Int = 100,
        playChime: Boolean = true,
        vibrate: Boolean = true
    ): Boolean {
        val audioManager = getAudioManager(context)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val targetVolume = if (targetVolumePercent >= 100) maxVolume else ((maxVolume * targetVolumePercent) / 100).coerceIn(1, maxVolume)

        val maxNotificationVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        val targetNotificationVolume = if (targetVolumePercent >= 100) maxNotificationVolume else ((maxNotificationVolume * targetVolumePercent) / 100).coerceIn(1, maxNotificationVolume)

        var success = true

        // 1. Restore Ringer Mode if not normal
        try {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                if (isDndAccessGranted(context)) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                } else {
                    Log.w(TAG, "Cannot switch ringerMode to NORMAL without DND permission")
                    success = false
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException restoring ringer mode", e)
            success = false
        }

        // 2. Restore Ringtone and Notification Volume to 100% full volume
        try {
            audioManager.setStreamVolume(
                AudioManager.STREAM_RING,
                targetVolume,
                AudioManager.FLAG_SHOW_UI
            )
            audioManager.setStreamVolume(
                AudioManager.STREAM_NOTIFICATION,
                targetNotificationVolume,
                0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting stream volume", e)
            success = false
        }

        // 3. Optional haptic vibration feedback
        if (vibrate) {
            triggerVibration(context)
        }

        // 4. Optional crisp audio tone confirmation
        if (playChime) {
            playRestoreChime()
        }

        return success
    }

    fun simulateMute(context: Context): Boolean {
        val audioManager = getAudioManager(context)
        return try {
            if (isDndAccessGranted(context)) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            } else {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_RING,
                    0,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error simulating mute", e)
            false
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 120, 80, 120),
                    intArrayOf(0, 200, 0, 255),
                    -1
                )
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 120, 80, 120),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 120, 80, 120), -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not trigger vibration", e)
        }
    }

    private fun playRestoreChime() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_RING, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            // Release after tone completes in a quick thread
            Thread {
                try {
                    Thread.sleep(250)
                    toneGen.release()
                } catch (_: Exception) {}
            }.start()
        } catch (e: Exception) {
            Log.w(TAG, "Could not play tone generator chime", e)
        }
    }
}
