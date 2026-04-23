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
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.backup.WorkoutBackupManager
import com.workout.shared.platform.AudioFeedback
import com.workout.shared.platform.SoundPresets
import com.workout.shared.platform.TimerSettings
import com.workout.shared.platform.rememberJsonExporter
import com.workout.shared.platform.rememberJsonImporter
import com.workout.shared.platform.rememberNotificationPermissionState
import com.workout.shared.ui.util.WorkoutDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.back
import workoutapp.shared.generated.resources.backup_section
import workoutapp.shared.generated.resources.cancel
import workoutapp.shared.generated.resources.cd_confirm_selection
import workoutapp.shared.generated.resources.export_workouts
import workoutapp.shared.generated.resources.go_to_settings
import workoutapp.shared.generated.resources.import_error
import workoutapp.shared.generated.resources.import_result_title
import workoutapp.shared.generated.resources.import_success
import workoutapp.shared.generated.resources.import_workouts
import workoutapp.shared.generated.resources.notification_permission_rationale_body
import workoutapp.shared.generated.resources.notification_permission_rationale_title
import workoutapp.shared.generated.resources.ok
import workoutapp.shared.generated.resources.prep_time_dialog_title
import workoutapp.shared.generated.resources.prep_time_title
import workoutapp.shared.generated.resources.save
import workoutapp.shared.generated.resources.seconds_input_label
import workoutapp.shared.generated.resources.seconds_unit_suffix
import workoutapp.shared.generated.resources.settings_contact_email_label
import workoutapp.shared.generated.resources.settings_contact_section
import workoutapp.shared.generated.resources.settings_developer_email
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
import workoutapp.shared.generated.resources.workout_backup_filename

private enum class SoundPickerTarget { WORK, REST, FINISH, WORK_PHASE_WARN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val settings = koinInject<TimerSettings>()
    val audioFeedback = koinInject<AudioFeedback>()
    val repository = koinInject<WorkoutRepository>()
    val backupManager = remember { WorkoutBackupManager(repository) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val developerEmail = stringResource(Res.string.settings_developer_email)
    val notificationPermission = rememberNotificationPermissionState()

    var prepSeconds by remember { mutableStateOf(settings.blockPrepDurationSeconds) }
    var soundEnabled by remember { mutableStateOf(settings.soundEnabled && notificationPermission.isGranted) }
    var awaitingPermissionToEnable by remember { mutableStateOf(false) }

    LaunchedEffect(notificationPermission.isGranted) {
        if (!notificationPermission.isGranted) {
            settings.soundEnabled = false
            soundEnabled = false
            awaitingPermissionToEnable = false
        } else if (awaitingPermissionToEnable) {
            settings.soundEnabled = true
            soundEnabled = true
            awaitingPermissionToEnable = false
        }
    }

    var vibrationEnabled by remember { mutableStateOf(settings.vibrationEnabled) }
    var workPhaseEndWarningSeconds by remember { mutableStateOf(settings.workPhaseEndWarningSeconds) }
    var workSoundPresetId by remember { mutableStateOf(settings.workStartSoundPresetId) }
    var restSoundPresetId by remember { mutableStateOf(settings.restStartSoundPresetId) }
    var finishSoundPresetId by remember { mutableStateOf(settings.workoutFinishSoundPresetId) }
    var workPhaseWarnSoundPresetId by remember { mutableStateOf(settings.workPhaseWarningSoundPresetId) }
    var timerQuickAdjustEnabled by remember { mutableStateOf(settings.timerQuickAdjustEnabled) }

    var soundPickerTarget by remember { mutableStateOf<SoundPickerTarget?>(null) }
    var importResultMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showPrepDialog by remember { mutableStateOf(false) }
    var showWorkWarnDialog by remember { mutableStateOf(false) }

    val importErrorText = stringResource(Res.string.import_error)
    val importSuccessText = stringResource(Res.string.import_success)
    val backupFilename = stringResource(Res.string.workout_backup_filename)

    val exportJson = rememberJsonExporter { }
    val importJson = rememberJsonImporter { content ->
        if (content == null) return@rememberJsonImporter
        scope.launch {
            try {
                val count = backupManager.importFromJson(content)
                importResultMessage = importSuccessText.replace("%1\$d", count.toString())
            } catch (e: Exception) {
                importResultMessage = importErrorText
            }
        }
    }

    // Overlays
    val pickerTarget = soundPickerTarget
    if (pickerTarget != null) {
        SoundPickerSheet(
            target = pickerTarget,
            initialPresetId = when (pickerTarget) {
                SoundPickerTarget.WORK -> workSoundPresetId
                SoundPickerTarget.REST -> restSoundPresetId
                SoundPickerTarget.FINISH -> finishSoundPresetId
                SoundPickerTarget.WORK_PHASE_WARN -> workPhaseWarnSoundPresetId
            },
            audioFeedback = audioFeedback,
            onConfirm = { presetId ->
                when (pickerTarget) {
                    SoundPickerTarget.WORK -> { settings.workStartSoundPresetId = presetId; workSoundPresetId = presetId }
                    SoundPickerTarget.REST -> { settings.restStartSoundPresetId = presetId; restSoundPresetId = presetId }
                    SoundPickerTarget.FINISH -> { settings.workoutFinishSoundPresetId = presetId; finishSoundPresetId = presetId }
                    SoundPickerTarget.WORK_PHASE_WARN -> { settings.workPhaseWarningSoundPresetId = presetId; workPhaseWarnSoundPresetId = presetId }
                }
                soundPickerTarget = null
            },
            onDismiss = { soundPickerTarget = null }
        )
    }

    importResultMessage?.let { message ->
        ImportResultDialog(message = message, onDismiss = { importResultMessage = null })
    }

    if (showPermissionRationaleDialog) {
        PermissionRationaleDialog(
            onGoToSettings = {
                showPermissionRationaleDialog = false
                notificationPermission.openSettings()
            },
            onDismiss = { showPermissionRationaleDialog = false }
        )
    }

    if (showPrepDialog) {
        PrepTimeDialog(
            currentSeconds = prepSeconds,
            onSave = { clamped ->
                settings.blockPrepDurationSeconds = clamped
                prepSeconds = clamped
                showPrepDialog = false
            },
            onDismiss = { showPrepDialog = false }
        )
    }

    if (showWorkWarnDialog) {
        WorkPhaseWarnDialog(
            currentSeconds = workPhaseEndWarningSeconds,
            onSave = { clamped ->
                settings.workPhaseEndWarningSeconds = clamped
                workPhaseEndWarningSeconds = clamped
                showWorkWarnDialog = false
            },
            onDismiss = { showWorkWarnDialog = false }
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
                SettingsSwitchRow(
                    label = stringResource(Res.string.timer_sound_enabled),
                    checked = soundEnabled,
                    onToggle = {
                        val checked = !soundEnabled
                        if (checked && !notificationPermission.isGranted) {
                            if (notificationPermission.shouldOpenSettings) {
                                showPermissionRationaleDialog = true
                            } else {
                                awaitingPermissionToEnable = true
                                notificationPermission.request()
                            }
                        } else {
                            settings.soundEnabled = checked
                            soundEnabled = checked
                        }
                    }
                )
            }
            item {
                SettingsSwitchRow(
                    label = stringResource(Res.string.vibration),
                    checked = vibrationEnabled,
                    onToggle = {
                        settings.vibrationEnabled = !vibrationEnabled
                        vibrationEnabled = !vibrationEnabled
                    }
                )
            }
            item {
                SettingsPresetRow(
                    label = stringResource(Res.string.sound_before_work),
                    presetId = workSoundPresetId,
                    onClick = { soundPickerTarget = SoundPickerTarget.WORK }
                )
            }
            item {
                SettingsPresetRow(
                    label = stringResource(Res.string.sound_before_rest),
                    presetId = restSoundPresetId,
                    onClick = { soundPickerTarget = SoundPickerTarget.REST }
                )
            }
            item {
                SettingsPresetRow(
                    label = stringResource(Res.string.sound_workout_finish),
                    presetId = finishSoundPresetId,
                    onClick = { soundPickerTarget = SoundPickerTarget.FINISH }
                )
            }
            item {
                SettingsPresetRow(
                    label = stringResource(Res.string.sound_work_phase_warn),
                    presetId = workPhaseWarnSoundPresetId,
                    onClick = { soundPickerTarget = SoundPickerTarget.WORK_PHASE_WARN }
                )
            }
            item {
                SettingsValueRow(
                    label = stringResource(Res.string.work_phase_warn_title),
                    value = if (workPhaseEndWarningSeconds <= 0) {
                        stringResource(Res.string.work_phase_warn_off)
                    } else {
                        stringResource(Res.string.seconds_unit_suffix, workPhaseEndWarningSeconds)
                    },
                    onClick = { showWorkWarnDialog = true }
                )
            }
            item {
                SettingsSwitchRow(
                    label = stringResource(Res.string.timer_quick_adjust_title),
                    checked = timerQuickAdjustEnabled,
                    onToggle = {
                        settings.timerQuickAdjustEnabled = !timerQuickAdjustEnabled
                        timerQuickAdjustEnabled = !timerQuickAdjustEnabled
                    }
                )
            }
            item {
                SettingsValueRow(
                    label = stringResource(Res.string.prep_time_title),
                    value = stringResource(Res.string.seconds_unit_suffix, prepSeconds),
                    onClick = { showPrepDialog = true }
                )
            }
            item {
                SettingsSectionHeader(title = stringResource(Res.string.backup_section))
            }
            item {
                SettingsClickRow(
                    label = stringResource(Res.string.export_workouts),
                    onClick = {
                        scope.launch {
                            val json = backupManager.exportToJson()
                            exportJson(backupFilename, json)
                        }
                    }
                )
            }
            item {
                SettingsClickRow(
                    label = stringResource(Res.string.import_workouts),
                    onClick = { importJson() }
                )
            }
            item {
                SettingsSectionHeader(title = stringResource(Res.string.settings_contact_section))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { uriHandler.openUri("mailto:$developerEmail") }
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.settings_contact_email_label),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = developerEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundPickerSheet(
    target: SoundPickerTarget,
    initialPresetId: String,
    audioFeedback: AudioFeedback,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPresetId by remember { mutableStateOf(initialPresetId) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(target) { sheetState.expand() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
                        text = when (target) {
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
                                selectedPresetId = preset.id
                                audioFeedback.previewPreset(preset.id)
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPresetId == preset.id,
                            onClick = {
                                selectedPresetId = preset.id
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
                onClick = { onConfirm(selectedPresetId) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.cd_confirm_selection))
            }
        }
    }
}

@Composable
private fun ImportResultDialog(message: String, onDismiss: () -> Unit) {
    WorkoutDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.import_result_title),
        confirmText = stringResource(Res.string.ok),
        onConfirm = onDismiss,
        content = { Text(message) }
    )
}

@Composable
private fun PermissionRationaleDialog(onGoToSettings: () -> Unit, onDismiss: () -> Unit) {
    WorkoutDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.notification_permission_rationale_title),
        confirmText = stringResource(Res.string.go_to_settings),
        onConfirm = onGoToSettings,
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        content = { Text(stringResource(Res.string.notification_permission_rationale_body)) }
    )
}

@Composable
private fun PrepTimeDialog(currentSeconds: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf(currentSeconds.toString()) }
    WorkoutDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.prep_time_dialog_title),
        confirmText = stringResource(Res.string.save),
        onConfirm = {
            val v = if (input.isBlank()) 0 else input.toIntOrNull() ?: currentSeconds
            onSave(v.coerceIn(0, 999))
        },
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        content = { SecondsInputField(value = input, onValueChange = { input = it }) }
    )
}

@Composable
private fun WorkPhaseWarnDialog(currentSeconds: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf(currentSeconds.toString()) }
    WorkoutDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.work_phase_warn_dialog_title),
        confirmText = stringResource(Res.string.save),
        onConfirm = {
            val v = if (input.isBlank()) 0 else input.toIntOrNull() ?: currentSeconds
            onSave(v.coerceIn(0, 999))
        },
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        content = {
            SecondsInputField(
                value = input,
                onValueChange = { input = it },
                supportingText = stringResource(Res.string.work_phase_warn_disabled_hint)
            )
        }
    )
}

@Composable
private fun SecondsInputField(
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String? = null
) {
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = Color.Black,
            backgroundColor = Color.Black.copy(alpha = 0.3f)
        )
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(3)) },
            label = { Text(stringResource(Res.string.seconds_input_label)) },
            supportingText = if (supportingText != null) {
                { Text(supportingText) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color.Black
            )
        )
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsValueRow(label: String, value: String, onClick: () -> Unit) {
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
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsClickRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsPresetRow(label: String, presetId: String, onClick: () -> Unit) {
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
