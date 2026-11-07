package com.example.ui

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.AudioEnforcer
import com.example.data.InterceptionLog
import com.example.ui.theme.GuardianAmber
import com.example.ui.theme.GuardianCoral
import com.example.ui.theme.GuardianCyan
import com.example.ui.theme.GuardianCyanDark
import com.example.ui.theme.GuardianTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NeverSilentScreen(
    viewModel: NeverSilentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsStateWithLifecycle()
    val isDndGranted by viewModel.isDndGranted.collectAsStateWithLifecycle()
    val isNotificationGranted by viewModel.isNotificationGranted.collectAsStateWithLifecycle()

    val currentRingerMode by viewModel.currentRingerMode.collectAsStateWithLifecycle()
    val currentVolume by viewModel.currentVolume.collectAsStateWithLifecycle()
    val maxVolume by viewModel.maxVolume.collectAsStateWithLifecycle()
    val countdownRemaining by viewModel.countdownRemaining.collectAsStateWithLifecycle()

    val targetVolumePercent by viewModel.targetVolumePercent.collectAsStateWithLifecycle()
    val soundChimeEnabled by viewModel.soundChimeEnabled.collectAsStateWithLifecycle()
    val vibrateEnabled by viewModel.vibrateEnabled.collectAsStateWithLifecycle()
    val interceptionHistory by viewModel.interceptionHistory.collectAsStateWithLifecycle()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshState(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HeaderSection(isProtectionEnabled = isProtectionEnabled)
        }

        // Permission Card: DND Access (Crucial on Android 7+)
        if (!isDndGranted) {
            item {
                PermissionWarningCard(
                    title = stringResource(R.string.permission_required_title),
                    description = stringResource(R.string.permission_required_desc),
                    buttonText = stringResource(R.string.permission_button),
                    testTag = "grant_dnd_button",
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Permission Card: Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationGranted) {
            item {
                PermissionWarningCard(
                    title = stringResource(R.string.notification_permission_title),
                    description = stringResource(R.string.notification_permission_desc),
                    buttonText = stringResource(R.string.grant_notification_button),
                    testTag = "grant_notification_button",
                    onClick = {
                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }
        }

        // Hero Master On/Off Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            MasterPowerButton(
                isProtectionEnabled = isProtectionEnabled,
                onClick = {
                    viewModel.toggleProtection(context)
                }
            )
        }

        // Active 3-Second Countdown Banner (Shows up when phone was silenced/muted)
        if (countdownRemaining != null && countdownRemaining!! > 0f) {
            item {
                ActiveCountdownCard(
                    remainingSeconds = countdownRemaining!!,
                    onRestoreNow = {
                        AudioEnforcer.restoreRingtone(
                            context = context,
                            targetVolumePercent = 100,
                            playChime = soundChimeEnabled,
                            vibrate = vibrateEnabled
                        )
                    }
                )
            }
        }

        // Device Live Audio Status
        item {
            AudioStatusCard(
                ringerMode = currentRingerMode,
                currentVolume = currentVolume,
                maxVolume = maxVolume,
                isProtected = isProtectionEnabled
            )
        }

        // Instant Mute Simulation / Test Tool
        item {
            TestGuardianCard(
                isProtectionEnabled = isProtectionEnabled,
                onSimulateMute = {
                    viewModel.simulateMuteTest(context)
                }
            )
        }

        // Target Volume & Settings
        item {
            GuardianSettingsCard(
                targetVolumePercent = targetVolumePercent,
                onVolumeChange = { viewModel.setTargetVolumePercent(it) },
                soundChimeEnabled = soundChimeEnabled,
                onSoundChimeToggle = { viewModel.setSoundChimeEnabled(it) },
                vibrateEnabled = vibrateEnabled,
                onVibrateToggle = { viewModel.setVibrateEnabled(it) }
            )
        }

        // Interception Activity Log
        item {
            ActivityLogCard(
                history = interceptionHistory,
                onClear = { viewModel.clearHistory() }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderSection(isProtectionEnabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = if (isProtectionEnabled) GuardianCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isProtectionEnabled) "Auto-Restore 3s Active" else "Protection Standby",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isProtectionEnabled) GuardianTeal else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isProtectionEnabled) GuardianTeal.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isProtectionEnabled) GuardianTeal else GuardianCoral)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isProtectionEnabled) "ENFORCING" else "PAUSED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isProtectionEnabled) GuardianTeal else GuardianCoral
                )
            }
        }
    }
}

@Composable
private fun MasterPowerButton(
    isProtectionEnabled: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isProtectionEnabled) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = if (isProtectionEnabled) GuardianCyan else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(400),
        label = "btn_bg"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isProtectionEnabled) Color(0xFF002B36) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(400),
        label = "icon_color"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glowing outer aura when enabled
            if (isProtectionEnabled) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(GuardianCyan.copy(alpha = 0.18f))
                )
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(GuardianCyanDark.copy(alpha = 0.25f))
                )
            }

            // Tactile Main Circle Button
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(CircleShape)
                    .background(buttonBgColor)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            if (isProtectionEnabled) listOf(GuardianCyan, GuardianTeal)
                            else listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline)
                        ),
                        shape = CircleShape
                    )
                    .clickable { onClick() }
                    .testTag("master_power_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isProtectionEnabled) Icons.Filled.NotificationsActive
                        else Icons.Filled.NotificationsOff,
                        contentDescription = stringResource(
                            if (isProtectionEnabled) R.string.button_turn_off else R.string.button_turn_on
                        ),
                        tint = iconColor,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isProtectionEnabled) "ACTIVE" else "OFF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = iconColor,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(
                if (isProtectionEnabled) R.string.status_protected else R.string.status_paused
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(
                if (isProtectionEnabled) R.string.status_description_active
                else R.string.status_description_paused
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun ActiveCountdownCard(
    remainingSeconds: Float,
    onRestoreNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("countdown_alert_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianCoral.copy(alpha = 0.15f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(GuardianCoral, GuardianAmber))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = GuardianCoral,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MUTE DETECTED!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GuardianCoral
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = String.format(Locale.US, "Restoring ringtone in %.1fs", remainingSeconds),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = GuardianAmber
            )

            Spacer(modifier = Modifier.height(10.dp))

            val progress = (1f - (remainingSeconds / 3.0f)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = GuardianCoral,
                trackColor = GuardianCoral.copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRestoreNow,
                colors = ButtonDefaults.buttonColors(containerColor = GuardianCoral),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("restore_now_button")
            ) {
                Text(
                    text = "Restore Ringtone Now",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AudioStatusCard(
    ringerMode: Int,
    currentVolume: Int,
    maxVolume: Int,
    isProtected: Boolean
) {
    val modeText = when (ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> "Normal (Loud)"
        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate Only"
        AudioManager.RINGER_MODE_SILENT -> "Silent / Mute"
        else -> "Unknown"
    }

    val modeColor = when (ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> GuardianTeal
        AudioManager.RINGER_MODE_VIBRATE -> GuardianAmber
        else -> GuardianCoral
    }

    val volumePercent = if (maxVolume > 0) ((currentVolume * 100) / maxVolume) else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Audio Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (ringerMode == AudioManager.RINGER_MODE_NORMAL) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = null,
                        tint = modeColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ringer Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = modeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = modeColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = GuardianCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ring Volume Level",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$volumePercent% ($currentVolume/$maxVolume)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TestGuardianCard(
    isProtectionEnabled: Boolean,
    onSimulateMute: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = GuardianCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Live Test Guardian",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tap the button below to simulate muting the phone. Observe the 3-second live countdown restore normal ringtone automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSimulateMute,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulate_mute_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuardianCyanDark
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_simulate_mute),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun GuardianSettingsCard(
    targetVolumePercent: Int,
    onVolumeChange: (Int) -> Unit,
    soundChimeEnabled: Boolean,
    onSoundChimeToggle: (Boolean) -> Unit,
    vibrateEnabled: Boolean,
    onVibrateToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = GuardianTeal,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardian Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Volume Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.target_volume_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$targetVolumePercent%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GuardianCyan
                )
            }

            Slider(
                value = targetVolumePercent.toFloat(),
                onValueChange = { onVolumeChange(it.toInt()) },
                valueRange = 30f..100f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = GuardianCyan,
                    activeTrackColor = GuardianCyan
                ),
                modifier = Modifier.testTag("target_volume_slider")
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Sound Chime Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sound_feedback_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = soundChimeEnabled,
                    onCheckedChange = onSoundChimeToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = GuardianCyan),
                    modifier = Modifier.testTag("sound_chime_switch")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vibrate Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.vibrate_feedback_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = vibrateEnabled,
                    onCheckedChange = onVibrateToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = GuardianCyan),
                    modifier = Modifier.testTag("vibrate_switch")
                )
            }
        }
    }
}

@Composable
private fun ActivityLogCard(
    history: List<InterceptionLog>,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = GuardianTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stringResource(R.string.recent_interceptions_title)} (${history.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (history.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_interceptions_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                history.take(6).forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = log.triggerReason,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "At ${dateFormat.format(Date(log.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GuardianTeal.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Restored ${String.format(Locale.US, "%.1fs", log.timeToRestoreSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GuardianTeal
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningCard(
    title: String,
    description: String,
    buttonText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag + "_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianAmber.copy(alpha = 0.12f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(GuardianAmber, GuardianCoral))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = GuardianAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GuardianAmber
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = GuardianAmber),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
