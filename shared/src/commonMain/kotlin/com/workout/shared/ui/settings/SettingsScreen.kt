package com.workout.shared.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.back
import workoutapp.shared.generated.resources.cancel
import workoutapp.shared.generated.resources.cd_confirm_selection
import workoutapp.shared.generated.resources.prep_time_dialog_title
import workoutapp.shared.generated.resources.prep_time_title
import workoutapp.shared.generated.resources.save
import workoutapp.shared.generated.resources.seconds_input_label
import workoutapp.shared.generated.resources.seconds_unit_suffix
import workoutapp.shared.generated.resources.settings_title
import workoutapp.shared.generated.resources.sound_before_rest
import workoutapp.shared.generated.resources.sound_before_work
import workoutapp.shared.generated.resources.sound_picker_finish_title
import workoutapp.shared.generated.resources.sound_picker_rest_title
import workoutapp.shared.generated.resources.sound_picker_work_phase_warn_title
import workoutapp.shared.generated.resources.sound_picker_work_title
import workoutapp.shared.generated.resources.sound_work_phase_warn
import workoutapp.shared.generated.resources.sound_workout_finish
import workoutapp.shared.generated.resources.timer_quick_adjust_title
import workoutapp.shared.generated.resources.timer_sound_enabled
import workoutapp.shared.generated.resources.vibration
import workoutapp.shared.generated.resources.work_phase_warn_dialog_title
import workoutapp.shared.generated.resources.work_phase_warn_disabled_hint
import workoutapp.shared.generated.resources.work_phase_warn_off
import workoutapp.shared.generated.resources.work_phase_warn_title
import com.workout.shared.platform.AudioFeedback
import com.workout.shared.platform.SoundPresets
import com.workout.shared.platform.TimerSettings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private enum class SoundPickerTarget { WORK, REST, FINISH, WORK_PHASE_WARN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val settings = koinInject<TimerSettings>()
    val audioFeedback = koinInject<AudioFeedback>()

    // Local state backed by TimerSettings
    var prepSeconds by remember { mutableStateOf(settings.blockPrepDurationSeconds) }
    var soundEnabled by remember { mutableStateOf(settings.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(settings.vibrationEnabled) }
    var workPhaseEndWarningSeconds by remember { mutableStateOf(settings.workPhaseEndWarningSeconds) }
    var workSoundPresetId by remember { mutableStateOf(settings.workStartSoundPresetId) }
    var restSoundPresetId by remember { mutableStateOf(settings.restStartSoundPresetId) }
    var finishSoundPresetId by remember { mutableStateOf(settings.workoutFinishSoundPresetId) }
    var workPhaseWarnSoundPresetId by remember { mutableStateOf(settings.workPhaseWarningSoundPresetId) }
    var timerQuickAdjustEnabled by remember { mutableStateOf(settings.timerQuickAdjustEnabled) }

    // Sound picker state
    var soundPickerTarget by remember { mutableStateOf<SoundPickerTarget?>(null) }
    var pendingSoundPresetId by remember { mutableStateOf(SoundPresets.DEFAULT_WORK_ID) }

    // Prep dialog state
    var showPrepDialog by remember { mutableStateOf(false) }
    var prepInput by remember { mutableStateOf("") }

    // Work warning dialog state
    var showWorkWarnDialog by remember { mutableStateOf(false) }
    var workWarnInput by remember { mutableStateOf("") }

    val pickerTarget = soundPickerTarget
    if (pickerTarget != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        LaunchedEffect(pickerTarget) {
            sheetState.expand()
        }
        ModalBottomSheet(
            onDismissRequest = { soundPickerTarget = null },
            sheetState = sheetState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 360.dp)
                    .navigationBarsPadding()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 88.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Text(
                            text = when (pickerTarget) {
                                SoundPickerTarget.WORK -> stringResource(Res.string.sound_picker_work_title)
                                SoundPickerTarget.REST -> stringResource(Res.string.sound_picker_rest_title)
                                SoundPickerTarget.FINISH -> stringResource(Res.string.sound_picker_finish_title)
                                SoundPickerTarget.WORK_PHASE_WARN -> stringResource(Res.string.sound_picker_work_phase_warn_title)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    items(SoundPresets.all, key = { it.id }) { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    pendingSoundPresetId = preset.id
                                    audioFeedback.previewPreset(preset.id)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = pendingSoundPresetId == preset.id,
                                onClick = {
                                    pendingSoundPresetId = preset.id
                                    audioFeedback.previewPreset(preset.id)
                                }
                            )
                            Text(
                                text = stringResource(preset.label),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = {
                        when (pickerTarget) {
                            SoundPickerTarget.WORK -> {
                                settings.workStartSoundPresetId = pendingSoundPresetId
                                workSoundPresetId = pendingSoundPresetId
                            }
                            SoundPickerTarget.REST -> {
                                settings.restStartSoundPresetId = pendingSoundPresetId
                                restSoundPresetId = pendingSoundPresetId
                            }
                            SoundPickerTarget.FINISH -> {
                                settings.workoutFinishSoundPresetId = pendingSoundPresetId
                                finishSoundPresetId = pendingSoundPresetId
                            }
                            SoundPickerTarget.WORK_PHASE_WARN -> {
                                settings.workPhaseWarningSoundPresetId = pendingSoundPresetId
                                workPhaseWarnSoundPresetId = pendingSoundPresetId
                            }
                        }
                        soundPickerTarget = null
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.cd_confirm_selection))
                }
            }
        }
    }

    if (showPrepDialog) {
        AlertDialog(
            onDismissRequest = { showPrepDialog = false },
            title = { Text(stringResource(Res.string.prep_time_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = prepInput,
                    onValueChange = { value ->
                        prepInput = value.filter { it.isDigit() }.take(3)
                    },
                    label = { Text(stringResource(Res.string.seconds_input_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val v = if (prepInput.isBlank()) 0 else prepInput.toIntOrNull() ?: prepSeconds
                        val clamped = v.coerceIn(0, 999)
                        settings.blockPrepDurationSeconds = clamped
                        prepSeconds = clamped
                        showPrepDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrepDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (showWorkWarnDialog) {
        AlertDialog(
            onDismissRequest = { showWorkWarnDialog = false },
            title = { Text(stringResource(Res.string.work_phase_warn_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = workWarnInput,
                    onValueChange = { value ->
                        workWarnInput = value.filter { it.isDigit() }.take(3)
                    },
                    label = { Text(stringResource(Res.string.seconds_input_label)) },
                    supportingText = { Text(stringResource(Res.string.work_phase_warn_disabled_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val v = if (workWarnInput.isBlank()) 0
                                else workWarnInput.toIntOrNull() ?: workPhaseEndWarningSeconds
                        val clamped = v.coerceIn(0, 999)
                        settings.workPhaseEndWarningSeconds = clamped
                        workPhaseEndWarningSeconds = clamped
                        showWorkWarnDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorkWarnDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.timer_sound_enabled),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            settings.soundEnabled = it
                            soundEnabled = it
                        }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.vibration),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            settings.vibrationEnabled = it
                            vibrationEnabled = it
                        }
                    )
                }
            }
            item {
                SettingsPresetRow(
                    label = stringResource(Res.string.sound_before_work),
                    presetId = workSoundPresetId,
                    onClick = {
                        pendingSoundPresetId = workSoundPresetId
                        soundPickerTarget = SoundPickerTarget.WORK
                    }
                )
            }
            item {
                SettingsPresetRow(
                    label = stringResource(Res.string.sound_before_rest),
                    presetId = restSoundPresetId,
                    onClick = {
                        pendingSoundPresetId = restSoundPresetId
                        soundPickerTarget = SoundPickerTarget.REST
                    }
                )
            }
            item {
                SettingsPresetRow(
                    label = stringResource(Res.string.sound_workout_finish),
                    presetId = finishSoundPresetId,
                    onClick = {
                        pendingSoundPresetId = finishSoundPresetId
                        soundPickerTarget = SoundPickerTarget.FINISH
                    }
                )
            }
            item {
                SettingsPresetRow(
                    label = stringResource(Res.string.sound_work_phase_warn),
                    presetId = workPhaseWarnSoundPresetId,
                    onClick = {
                        pendingSoundPresetId = workPhaseWarnSoundPresetId
                        soundPickerTarget = SoundPickerTarget.WORK_PHASE_WARN
                    }
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            workWarnInput = workPhaseEndWarningSeconds.toString()
                            showWorkWarnDialog = true
                        }
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.work_phase_warn_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = if (workPhaseEndWarningSeconds <= 0) {
                            stringResource(Res.string.work_phase_warn_off)
                        } else {
                            stringResource(Res.string.seconds_unit_suffix, workPhaseEndWarningSeconds)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.timer_quick_adjust_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Switch(
                        checked = timerQuickAdjustEnabled,
                        onCheckedChange = {
                            settings.timerQuickAdjustEnabled = it
                            timerQuickAdjustEnabled = it
                        }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            prepInput = prepSeconds.toString()
                            showPrepDialog = true
                        }
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.prep_time_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(Res.string.seconds_unit_suffix, prepSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPresetRow(
    label: String,
    presetId: String,
    onClick: () -> Unit
) {
    val presetLabel = SoundPresets.all.find { it.id == presetId }
        ?.let { stringResource(it.label) }
        ?: presetId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Text(
            text = presetLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
