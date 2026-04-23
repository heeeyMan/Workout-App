package com.workout.shared.ui.createworkout

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workout.core.model.Block
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.feature.createworkout.CreateWorkoutEffect
import com.workout.shared.feature.createworkout.CreateWorkoutIntent
import com.workout.shared.feature.createworkout.CreateWorkoutViewModel
import com.workout.shared.ui.components.WheelTimePicker
import com.workout.shared.ui.components.toTimeString
import com.workout.shared.ui.theme.TimerRestGreen
import com.workout.shared.ui.theme.TimerWorkOrange
import com.workout.shared.ui.util.WorkoutDialog
import kotlin.random.Random
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.add_exercise
import workoutapp.shared.generated.resources.add_rest
import workoutapp.shared.generated.resources.back
import workoutapp.shared.generated.resources.block_type_exercise
import workoutapp.shared.generated.resources.block_type_rest
import workoutapp.shared.generated.resources.cancel
import workoutapp.shared.generated.resources.cd_duplicate
import workoutapp.shared.generated.resources.cd_edit
import workoutapp.shared.generated.resources.cd_move_down
import workoutapp.shared.generated.resources.cd_move_up
import workoutapp.shared.generated.resources.create_workout_title_edit
import workoutapp.shared.generated.resources.create_workout_title_new
import workoutapp.shared.generated.resources.default_exercise_name_pattern
import workoutapp.shared.generated.resources.default_workout_name_pattern
import workoutapp.shared.generated.resources.delete
import workoutapp.shared.generated.resources.dialog_exercise_name_title
import workoutapp.shared.generated.resources.dialog_rest_duration_title
import workoutapp.shared.generated.resources.dialog_work_duration_title
import workoutapp.shared.generated.resources.done
import workoutapp.shared.generated.resources.duration_label
import workoutapp.shared.generated.resources.error_workout_name_required
import workoutapp.shared.generated.resources.repeats_label
import workoutapp.shared.generated.resources.rest_label
import workoutapp.shared.generated.resources.save
import workoutapp.shared.generated.resources.training_time
import workoutapp.shared.generated.resources.work_label
import workoutapp.shared.generated.resources.workout_name_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutScreen(
    workoutId: Long,
    onNavigateBack: () -> Unit
) {
    val repository = koinInject<WorkoutRepository>()
    val vm = viewModel { CreateWorkoutViewModel(repository) }
    val store = vm.store

    val state by store.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(workoutId) {
        if (workoutId != 0L) {
            store.dispatch(CreateWorkoutIntent.LoadWorkout(workoutId))
        } else {
            val defaultName = getString(Res.string.default_workout_name_pattern, Random.nextInt(101))
            store.dispatch(CreateWorkoutIntent.SetDefaultWorkoutNameIfEmpty(defaultName))
        }
    }

    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            when (effect) {
                is CreateWorkoutEffect.NavigateBack -> onNavigateBack()
                is CreateWorkoutEffect.ShowErrorEmptyWorkoutName -> snackbarHostState.showSnackbar(
                    getString(Res.string.error_workout_name_required)
                )
            }
        }
    }

    val nextExerciseName = stringResource(Res.string.default_exercise_name_pattern, state.blocks.size + 1)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CreateWorkoutTopBar(
                isEdit = workoutId != 0L,
                totalDurationSeconds = state.totalDurationSeconds,
                onDiscard = { store.dispatch(CreateWorkoutIntent.Discard) },
                onSave = { store.dispatch(CreateWorkoutIntent.Save) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WorkoutNameField(
                    name = state.name,
                    onNameChange = { store.dispatch(CreateWorkoutIntent.UpdateName(it)) }
                )
            }

            itemsIndexed(
                items = state.blocks,
                key = { _, block -> if (block.id != 0L) block.id else "n${block.orderIndex}" }
            ) { index, block ->
                BlockCard(
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    block = block,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.blocks.size - 1,
                    onMoveUp = { store.dispatch(CreateWorkoutIntent.MoveBlock(index, index - 1)) },
                    onMoveDown = { store.dispatch(CreateWorkoutIntent.MoveBlock(index, index + 1)) },
                    onUpdate = { updated -> store.dispatch(CreateWorkoutIntent.UpdateBlock(index, updated)) },
                    onDelete = { store.dispatch(CreateWorkoutIntent.RemoveBlock(index)) },
                    onDuplicate = { store.dispatch(CreateWorkoutIntent.DuplicateBlock(index)) }
                )
            }

            item {
                AddBlockButtons(
                    onAddExercise = {
                        store.dispatch(CreateWorkoutIntent.AddExerciseBlock(afterIndex = null, defaultExerciseName = nextExerciseName))
                    },
                    onAddRest = { store.dispatch(CreateWorkoutIntent.AddRestBlock()) }
                )
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// -----------------------------------------------
// Top bar
// -----------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateWorkoutTopBar(
    isEdit: Boolean,
    totalDurationSeconds: Int,
    onDiscard: () -> Unit,
    onSave: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    if (isEdit) stringResource(Res.string.create_workout_title_edit)
                    else stringResource(Res.string.create_workout_title_new)
                )
                if (totalDurationSeconds > 0) {
                    Text(
                        text = stringResource(Res.string.training_time, totalDurationSeconds.toTimeString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onDiscard) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
            }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.save))
            }
        }
    )
}

// -----------------------------------------------
// Workout name field
// -----------------------------------------------

@Composable
private fun WorkoutNameField(name: String, onNameChange: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.workout_name_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    )
}

// -----------------------------------------------
// Add block buttons
// -----------------------------------------------

@Composable
private fun AddBlockButtons(onAddExercise: () -> Unit, onAddRest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onAddExercise,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(Res.string.add_exercise))
        }
        Button(
            onClick = onAddRest,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(Res.string.add_rest))
        }
    }
}

// -----------------------------------------------
// Block card
// -----------------------------------------------

@Composable
private fun BlockCard(
    modifier: Modifier = Modifier,
    block: Block,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUpdate: (Block) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val isExercise = block is Block.Exercise
    val accentColor = if (isExercise) TimerWorkOrange else TimerRestGreen

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BlockCardHeader(
                isExercise = isExercise,
                accentColor = accentColor,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDuplicate = onDuplicate,
                onDelete = onDelete
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            Spacer(Modifier.height(16.dp))

            when (block) {
                is Block.Exercise -> ExerciseBlockContent(block = block, onUpdate = onUpdate)
                is Block.Rest -> RestBlockContent(block = block, onUpdate = onUpdate)
            }
        }
    }
}

// -----------------------------------------------
// Block card header
// -----------------------------------------------

@Composable
private fun BlockCardHeader(
    isExercise: Boolean,
    accentColor: Color,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accentColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isExercise) stringResource(Res.string.block_type_exercise)
                   else stringResource(Res.string.block_type_rest),
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Стрелки вверх/вниз для точного перемещения на одну позицию
            val arrowTint = MaterialTheme.colorScheme.onSurfaceVariant
            val arrowDisabledTint = arrowTint.copy(alpha = 0.3f)
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = stringResource(Res.string.cd_move_up),
                    tint = if (canMoveUp) arrowTint else arrowDisabledTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(7.dp))
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.cd_move_down),
                    tint = if (canMoveDown) arrowTint else arrowDisabledTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(30.dp))
            IconButton(onClick = onDuplicate, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(Res.string.cd_duplicate),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(Res.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// -----------------------------------------------
// Exercise block content
// -----------------------------------------------

@Composable
private fun ExerciseBlockContent(block: Block.Exercise, onUpdate: (Block) -> Unit) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showWorkDialog by remember { mutableStateOf(false) }
    var showRestDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { showNameDialog = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = block.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Outlined.Edit,
            contentDescription = stringResource(Res.string.cd_edit),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DurationChip(
            label = stringResource(Res.string.work_label),
            seconds = block.workDurationSeconds,
            color = TimerWorkOrange,
            modifier = Modifier.weight(1f),
            onClick = { showWorkDialog = true }
        )
        DurationChip(
            label = stringResource(Res.string.rest_label),
            seconds = block.restDurationSeconds,
            color = TimerRestGreen,
            modifier = Modifier.weight(1f),
            onClick = { showRestDialog = true }
        )
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
    Spacer(Modifier.height(12.dp))

    RepeatsRow(
        repeats = block.repeats,
        onDecrement = { onUpdate(block.copy(repeats = block.repeats - 1)) },
        onIncrement = { onUpdate(block.copy(repeats = block.repeats + 1)) }
    )

    if (showNameDialog) {
        NameEditDialog(
            currentName = block.name,
            onConfirm = { newName -> onUpdate(block.copy(name = newName)); showNameDialog = false },
            onDismiss = { showNameDialog = false }
        )
    }
    if (showWorkDialog) {
        DurationPickerDialog(
            title = stringResource(Res.string.dialog_work_duration_title),
            seconds = block.workDurationSeconds,
            onConfirm = { onUpdate(block.copy(workDurationSeconds = it)); showWorkDialog = false },
            onDismiss = { showWorkDialog = false }
        )
    }
    if (showRestDialog) {
        DurationPickerDialog(
            title = stringResource(Res.string.dialog_rest_duration_title),
            seconds = block.restDurationSeconds,
            onConfirm = { onUpdate(block.copy(restDurationSeconds = it)); showRestDialog = false },
            onDismiss = { showRestDialog = false }
        )
    }
}

// -----------------------------------------------
// Rest block content
// -----------------------------------------------

@Composable
private fun RestBlockContent(block: Block.Rest, onUpdate: (Block) -> Unit) {
    var showDurationDialog by remember { mutableStateOf(false) }

    DurationChip(
        label = stringResource(Res.string.duration_label),
        seconds = block.durationSeconds,
        color = TimerRestGreen,
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDurationDialog = true }
    )

    if (showDurationDialog) {
        DurationPickerDialog(
            title = stringResource(Res.string.dialog_rest_duration_title),
            seconds = block.durationSeconds,
            onConfirm = { onUpdate(block.copy(durationSeconds = it)); showDurationDialog = false },
            onDismiss = { showDurationDialog = false }
        )
    }
}

// -----------------------------------------------
// Reusable components
// -----------------------------------------------

@Composable
private fun RepeatsRow(repeats: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(Res.string.repeats_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RepeatButton(label = "\u2212", enabled = repeats > 1, onClick = onDecrement)
            Text(
                text = repeats.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(48.dp)
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )
            RepeatButton(label = "+", onClick = onIncrement)
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    seconds: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = seconds.toTimeString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RepeatButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NameEditDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue(currentName, selection = TextRange(currentName.length))) }
    val focusRequester = remember { FocusRequester() }

    WorkoutDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.dialog_exercise_name_title),
        confirmText = stringResource(Res.string.done),
        onConfirm = { if (text.text.isNotBlank()) onConfirm(text.text.trim()) },
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        content = {
            val onDialog = MaterialTheme.colorScheme.onPrimary
            val onDialogMuted = MaterialTheme.colorScheme.onSurfaceVariant
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = onDialog,
                    backgroundColor = onDialog.copy(alpha = 0.3f)
                )
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = onDialog,
                        unfocusedTextColor = onDialog,
                        focusedBorderColor = onDialog,
                        unfocusedBorderColor = onDialogMuted,
                        cursorColor = onDialog
                    )
                )
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    )
}

@Composable
private fun DurationPickerDialog(
    title: String,
    seconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var minutes by remember { mutableIntStateOf(seconds / 60) }
    var secs by remember { mutableIntStateOf(seconds % 60) }

    WorkoutDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = stringResource(Res.string.done),
        onConfirm = { onConfirm(minutes * 60 + secs) },
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        content = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WheelTimePicker(
                    minutes = minutes,
                    seconds = secs,
                    onMinutesChange = { minutes = it },
                    onSecondsChange = { secs = it }
                )
            }
        }
    )
}
