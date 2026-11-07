package com.example.core

import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProtectionStateHolder {
    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _currentRingerMode = MutableStateFlow(AudioManager.RINGER_MODE_NORMAL)
    val currentRingerMode: StateFlow<Int> = _currentRingerMode.asStateFlow()

    private val _currentVolume = MutableStateFlow(7)
    val currentVolume: StateFlow<Int> = _currentVolume.asStateFlow()

    private val _maxVolume = MutableStateFlow(15)
    val maxVolume: StateFlow<Int> = _maxVolume.asStateFlow()

    private val _countdownRemaining = MutableStateFlow<Float?>(null)
    val countdownRemaining: StateFlow<Float?> = _countdownRemaining.asStateFlow()

    private val _lastRestoredEvent = MutableStateFlow<Long?>(null)
    val lastRestoredEvent: StateFlow<Long?> = _lastRestoredEvent.asStateFlow()

    fun setServiceActive(active: Boolean) {
        _isServiceActive.value = active
    }

    fun updateAudioState(ringerMode: Int, volume: Int, maxVol: Int) {
        _currentRingerMode.value = ringerMode
        _currentVolume.value = volume
        _maxVolume.value = maxVol
    }

    fun setCountdown(remainingSeconds: Float?) {
        _countdownRemaining.value = remainingSeconds
    }

    fun notifyRestored() {
        _lastRestoredEvent.value = System.currentTimeMillis()
        _countdownRemaining.value = null
    }
}
