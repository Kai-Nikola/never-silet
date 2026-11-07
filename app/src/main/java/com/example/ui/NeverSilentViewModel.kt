package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AudioEnforcer
import com.example.core.ProtectionStateHolder
import com.example.data.InterceptionLog
import com.example.data.PreferencesManager
import com.example.service.NeverSilentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NeverSilentViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager(application)

    val isProtectionEnabled: StateFlow<Boolean> = prefsManager.isProtectionEnabled
    val targetVolumePercent: StateFlow<Int> = prefsManager.targetVolumePercent
    val soundChimeEnabled: StateFlow<Boolean> = prefsManager.soundChimeEnabled
    val vibrateEnabled: StateFlow<Boolean> = prefsManager.vibrateEnabled
    val interceptionHistory: StateFlow<List<InterceptionLog>> = prefsManager.interceptionHistory

    val isServiceActive: StateFlow<Boolean> = ProtectionStateHolder.isServiceActive
    val currentRingerMode: StateFlow<Int> = ProtectionStateHolder.currentRingerMode
    val currentVolume: StateFlow<Int> = ProtectionStateHolder.currentVolume
    val maxVolume: StateFlow<Int> = ProtectionStateHolder.maxVolume
    val countdownRemaining: StateFlow<Float?> = ProtectionStateHolder.countdownRemaining

    private val _isDndGranted = MutableStateFlow(false)
    val isDndGranted: StateFlow<Boolean> = _isDndGranted.asStateFlow()

    private val _isNotificationGranted = MutableStateFlow(true)
    val isNotificationGranted: StateFlow<Boolean> = _isNotificationGranted.asStateFlow()

    init {
        refreshState(application)
    }

    fun refreshState(context: Context = getApplication()) {
        _isDndGranted.value = AudioEnforcer.isDndAccessGranted(context)

        _isNotificationGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val audioManager = AudioEnforcer.getAudioManager(context)
        ProtectionStateHolder.updateAudioState(
            ringerMode = audioManager.ringerMode,
            volume = audioManager.getStreamVolume(AudioManager.STREAM_RING),
            maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        )

        // If enabled in preferences but service not yet running, start it
        if (isProtectionEnabled.value && !ProtectionStateHolder.isServiceActive.value) {
            try {
                NeverSilentService.start(context)
            } catch (_: Exception) {}
        }
    }

    fun toggleProtection(context: Context = getApplication()) {
        val newTarget = !isProtectionEnabled.value
        prefsManager.setProtectionEnabled(newTarget)
        if (newTarget) {
            NeverSilentService.start(context)
        } else {
            NeverSilentService.stop(context)
        }
    }

    fun setTargetVolumePercent(percent: Int) {
        prefsManager.setTargetVolumePercent(percent)
    }

    fun setSoundChimeEnabled(enabled: Boolean) {
        prefsManager.setSoundChimeEnabled(enabled)
    }

    fun setVibrateEnabled(enabled: Boolean) {
        prefsManager.setVibrateEnabled(enabled)
    }

    fun simulateMuteTest(context: Context = getApplication()) {
        viewModelScope.launch {
            AudioEnforcer.simulateMute(context)
            NeverSilentService.triggerCheck(context)
        }
    }

    fun clearHistory() {
        prefsManager.clearHistory()
    }
}
