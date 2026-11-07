package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("never_silent_prefs", Context.MODE_PRIVATE)

    private val _isProtectionEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_PROTECTION_ENABLED, true)
    )
    val isProtectionEnabled: StateFlow<Boolean> = _isProtectionEnabled.asStateFlow()

    private val _targetVolumePercent = MutableStateFlow(
        prefs.getInt(KEY_TARGET_VOLUME_PERCENT, 100)
    )
    val targetVolumePercent: StateFlow<Int> = _targetVolumePercent.asStateFlow()

    private val _soundChimeEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SOUND_CHIME, true)
    )
    val soundChimeEnabled: StateFlow<Boolean> = _soundChimeEnabled.asStateFlow()

    private val _vibrateEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_VIBRATE, true)
    )
    val vibrateEnabled: StateFlow<Boolean> = _vibrateEnabled.asStateFlow()

    private val _interceptionHistory = MutableStateFlow(loadHistory())
    val interceptionHistory: StateFlow<List<InterceptionLog>> = _interceptionHistory.asStateFlow()

    fun setProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
        _isProtectionEnabled.value = enabled
    }

    fun setTargetVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(20, 100)
        prefs.edit().putInt(KEY_TARGET_VOLUME_PERCENT, clamped).apply()
        _targetVolumePercent.value = clamped
    }

    fun setSoundChimeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_CHIME, enabled).apply()
        _soundChimeEnabled.value = enabled
    }

    fun setVibrateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
        _vibrateEnabled.value = enabled
    }

    fun addInterceptionLog(log: InterceptionLog) {
        val current = _interceptionHistory.value.toMutableList()
        current.add(0, log)
        val trimmed = if (current.size > 50) current.take(50) else current
        saveHistory(trimmed)
        _interceptionHistory.value = trimmed
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
        _interceptionHistory.value = emptyList()
    }

    private fun loadHistory(): List<InterceptionLog> {
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<InterceptionLog>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InterceptionLog(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        triggerReason = obj.getString("triggerReason"),
                        timeToRestoreSeconds = obj.getDouble("timeToRestoreSeconds").toFloat(),
                        restoredVolumePercent = obj.getInt("restoredVolumePercent")
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore parse errors
        }
        return list
    }

    private fun saveHistory(list: List<InterceptionLog>) {
        val array = JSONArray()
        for (log in list) {
            val obj = JSONObject().apply {
                put("id", log.id)
                put("timestamp", log.timestamp)
                put("triggerReason", log.triggerReason)
                put("timeToRestoreSeconds", log.timeToRestoreSeconds.toDouble())
                put("restoredVolumePercent", log.restoredVolumePercent)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val KEY_PROTECTION_ENABLED = "key_protection_enabled"
        private const val KEY_TARGET_VOLUME_PERCENT = "key_target_volume_percent"
        private const val KEY_SOUND_CHIME = "key_sound_chime"
        private const val KEY_VIBRATE = "key_vibrate"
        private const val KEY_HISTORY = "key_history"
    }
}
