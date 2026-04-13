package com.workout.android.ui.settings

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.workout.android.R
import com.workout.android.feedback.TimerFeedback
import com.workout.android.feedback.TimerSoundPresets
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val prepSeconds by viewModel.blockPrepSeconds.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val workPhaseEndWarningSeconds by viewModel.workPhaseEndWarningSeconds.collectAsState()
    val workSoundPresetId by viewModel.workSoundPresetId.collectAsState()
    val restSoundPresetId by viewModel.restSoundPresetId.collectAsState()
    val finishSoundPresetId by viewModel.finishSoundPresetId.collectAsState()
    val workPhaseWarnSoundPresetId by viewModel.workPhaseWarnSoundPresetId.collectAsState()
    val timerQuickAdjustEnabled by viewModel.timerQuickAdjustEnabled.collectAsState()
    val soundPickerTarget by viewModel.soundPickerTarget.collectAsState()
    val pendingSoundPresetId by viewModel.pendingSoundPresetId.collectAsState()
    val context = LocalContext.current
    var showPrepDialog by remember { mutableStateOf(false) }
    var prepInput by remember { mutableStateOf("") }
    var showWorkWarnDialog by remember { mutableStateOf(false) }
    var workWarnInput by remember { mutableStateOf("") }

    val pickerTarget = soundPickerTarget
    if (pickerTarget != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        LaunchedEffect(pickerTarget) {
            sheetState.expand()
        }
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSoundPicker() },
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
                                TimerSoundPickerTarget.WORK -> stringResource(R.string.sound_picker_work_title)
                                TimerSoundPickerTarget.REST -> stringResource(R.string.sound_picker_rest_title)
                                TimerSoundPickerTarget.FINISH -> stringResource(R.string.sound_picker_finish_title)
                                TimerSoundPickerTarget.WORK_PHASE_WARN ->
                                    stringResource(R.string.sound_picker_work_phase_warn_title)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    items(TimerSoundPresets.all, key = { it.id }) { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setPendingSoundPresetId(preset.id)
                                    TimerFeedback.previewPreset(context, preset.id)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = pendingSoundPresetId == preset.id,
                                onClick = {
                                    viewModel.setPendingSoundPresetId(preset.id)
                                    TimerFeedback.previewPreset(context, preset.id)
                                }
                            )
                            Text(
                                text = stringResource(preset.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { viewModel.confirmSoundPicker() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.cd_confirm_selection))
                }
            }
        }
    }

    if (showPrepDialog) {
        AlertDialog(
            onDismissRequest = { showPrepDialog = false },
            title = { Text(stringResource(R.string.prep_time_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = prepInput,
                    onValueChange = { value ->
                        prepInput = value.filter { it.isDigit() }.take(3)
                    },
                    label = { Text(stringResource(R.string.seconds_input_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val v = if (prepInput.isBlank()) {
                            0
                        } else {
                            prepInput.toIntOrNull() ?: prepSeconds
                        }
                        viewModel.setBlockPrepSeconds(v)
                        showPrepDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrepDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showWorkWarnDialog) {
        AlertDialog(
            onDismissRequest = { showWorkWarnDialog = false },
            title = { Text(stringResource(R.string.work_phase_warn_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = workWarnInput,
                    onValueChange = { value ->
                        workWarnInput = value.filter { it.isDigit() }.take(3)
                    },
                    label = { Text(stringResource(R.string.seconds_input_label)) },
                    supportingText = { Text(stringResource(R.string.work_phase_warn_dialog_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val v = if (workWarnInput.isBlank()) {
                            0
                        } else {
                            workWarnInput.toIntOrNull() ?: workPhaseEndWarningSeconds
                        }
                        viewModel.setWorkPhaseEndWarningSeconds(v)
                        showWorkWarnDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorkWarnDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                        text = stringResource(R.string.timer_sound_enabled),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = viewModel::setSoundEnabled
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
                        text = stringResource(R.string.vibration),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = viewModel::setVibrationEnabled
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = viewModel::openWorkSoundPicker)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sound_before_work),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(TimerSoundPresets.byId(workSoundPresetId).labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = viewModel::openRestSoundPicker)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sound_before_rest),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(TimerSoundPresets.byId(restSoundPresetId).labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = viewModel::openFinishSoundPicker)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sound_workout_finish),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(TimerSoundPresets.byId(finishSoundPresetId).labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = viewModel::openWorkPhaseWarnSoundPicker)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sound_work_phase_warn),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(TimerSoundPresets.byId(workPhaseWarnSoundPresetId).labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        text = stringResource(R.string.work_phase_warn_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = if (workPhaseEndWarningSeconds <= 0) {
                            stringResource(R.string.work_phase_warn_off)
                        } else {
                            stringResource(R.string.seconds_unit_suffix, workPhaseEndWarningSeconds)
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
                        text = stringResource(R.string.timer_quick_adjust_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Switch(
                        checked = timerQuickAdjustEnabled,
                        onCheckedChange = viewModel::setTimerQuickAdjustEnabled
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
                        text = stringResource(R.string.prep_time_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.seconds_unit_suffix, prepSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
