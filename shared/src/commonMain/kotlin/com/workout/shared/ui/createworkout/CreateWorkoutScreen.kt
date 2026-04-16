package com.workout.shared.ui.createworkout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.workout.core.model.Block
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.feature.createworkout.CreateWorkoutEffect
import com.workout.shared.feature.createworkout.CreateWorkoutIntent
import com.workout.shared.feature.createworkout.CreateWorkoutStore
import com.workout.shared.ui.components.WheelTimePicker
import com.workout.shared.ui.components.toTimeString
import com.workout.shared.ui.theme.BrownContainer
import com.workout.shared.ui.theme.OnBrownContainer
import com.workout.shared.ui.theme.SurfaceVariant
import com.workout.shared.ui.theme.TimerRestGreen
import com.workout.shared.ui.theme.TimerWorkOrange
import kotlin.math.abs
import kotlin.random.Random
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutScreen(
    workoutId: Long,
    onNavigateBack: () -> Unit
) {
    val repository = koinInject<WorkoutRepository>()
    val store = remember { CreateWorkoutStore(repository) }
    DisposableEffect(Unit) { onDispose { store.destroy() } }

    val state by store.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val blockCenters = remember { mutableStateMapOf<Int, Float>() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(workoutId) {
        if (workoutId != 0L) {
            store.dispatch(CreateWorkoutIntent.LoadWorkout(workoutId))
        } else {
            // TODO: CMP resources
            val defaultName = "Workout ${Random.nextInt(101)}"
            store.dispatch(CreateWorkoutIntent.SetDefaultWorkoutNameIfEmpty(defaultName))
        }
    }

    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            when (effect) {
                is CreateWorkoutEffect.NavigateBack -> onNavigateBack()
                is CreateWorkoutEffect.ShowErrorEmptyWorkoutName -> snackbarHostState.showSnackbar(
                    "Workout name is required" // TODO: CMP resources
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            // TODO: CMP resources
                            if (workoutId == 0L) "New Workout" else "Edit Workout"
                        )
                        if (state.totalDurationSeconds > 0) {
                            val totalTime = state.totalDurationSeconds.toTimeString()
                            Text(
                                // TODO: CMP resources
                                text = "Training time: $totalTime",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { store.dispatch(CreateWorkoutIntent.Discard) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back" // TODO: CMP resources
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { store.dispatch(CreateWorkoutIntent.Save) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save") // TODO: CMP resources
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { store.dispatch(CreateWorkoutIntent.UpdateName(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Workout name") }, // TODO: CMP resources
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }

            itemsIndexed(state.blocks, key = { _, block -> block.id.toString() + block.orderIndex }) { index, block ->
                Box(modifier = Modifier.animateItem()) {
                    BlockCard(
                        block = block,
                        index = index,
                        totalBlocks = state.blocks.size,
                        isDragging = draggingIndex == index,
                        dragOffsetY = if (draggingIndex == index) dragOffsetY else 0f,
                        onMeasuredCenterY = { center -> blockCenters[index] = center },
                        onStartDrag = {
                            draggingIndex = index
                            dragOffsetY = 0f
                        },
                        onDrag = { deltaY ->
                            if (draggingIndex < 0) return@BlockCard
                            dragOffsetY += deltaY

                            val from = draggingIndex
                            val currentCenter = blockCenters[from] ?: return@BlockCard
                            val targetCenter = currentCenter + dragOffsetY

                            val nearest = blockCenters
                                .minByOrNull { (_, center) -> abs(center - targetCenter) }
                                ?.key
                                ?: return@BlockCard

                            if (nearest != from && nearest in state.blocks.indices) {
                                store.dispatch(CreateWorkoutIntent.MoveBlock(from, nearest))
                                draggingIndex = nearest
                                dragOffsetY = 0f
                            }
                        },
                        onEndDrag = {
                            draggingIndex = -1
                            dragOffsetY = 0f
                        },
                        onUpdate = { updated -> store.dispatch(CreateWorkoutIntent.UpdateBlock(index, updated)) },
                        onDelete = { store.dispatch(CreateWorkoutIntent.RemoveBlock(index)) },
                        onDuplicate = { store.dispatch(CreateWorkoutIntent.DuplicateBlock(index)) },
                        onMoveUp = { if (index > 0) store.dispatch(CreateWorkoutIntent.MoveBlock(index, index - 1)) },
                        onMoveDown = { if (index < state.blocks.size - 1) store.dispatch(CreateWorkoutIntent.MoveBlock(index, index + 1)) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val n = state.blocks.size + 1
                            store.dispatch(
                                CreateWorkoutIntent.AddExerciseBlock(
                                    afterIndex = null,
                                    // TODO: CMP resources
                                    defaultExerciseName = "Exercise $n"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Exercise") // TODO: CMP resources
                    }
                    Button(
                        onClick = { store.dispatch(CreateWorkoutIntent.AddRestBlock()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Rest") // TODO: CMP resources
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// -----------------------------------------------
// Block card
// -----------------------------------------------

@Composable
private fun BlockCard(
    block: Block,
    index: Int,
    totalBlocks: Int,
    isDragging: Boolean,
    dragOffsetY: Float,
    onMeasuredCenterY: (Float) -> Unit,
    onStartDrag: () -> Unit,
    onDrag: (Float) -> Unit,
    onEndDrag: () -> Unit,
    onUpdate: (Block) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val isExercise = block is Block.Exercise
    val accentColor = if (isExercise) TimerWorkOrange else TimerRestGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragOffsetY
                shadowElevation = if (isDragging) 24f else 0f
            }
            .pointerInput(index) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onStartDrag() },
                    onDragCancel = { onEndDrag() },
                    onDragEnd = { onEndDrag() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    }
                )
            }
            .onGloballyPositioned { coordinates ->
                val centerY = coordinates.positionInRoot().y + coordinates.size.height / 2f
                onMeasuredCenterY(centerY)
            },
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // -- Header + action icons --
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    // TODO: CMP resources
                    text = if (isExercise) "EXERCISE" else "REST",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDragging) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.DragHandle,
                            contentDescription = "Reorder", // TODO: CMP resources
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Duplicate", // TODO: CMP resources
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete", // TODO: CMP resources
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

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
// Exercise block content
// -----------------------------------------------

@Composable
private fun ExerciseBlockContent(block: Block.Exercise, onUpdate: (Block) -> Unit) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showWorkDialog by remember { mutableStateOf(false) }
    var showRestDialog by remember { mutableStateOf(false) }

    // -- Name (tap -> dialog) --
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
            contentDescription = "Edit", // TODO: CMP resources
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }

    Spacer(Modifier.height(16.dp))

    // -- Work and Rest (tap -> time picker) --
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DurationChip(
            label = "Work", // TODO: CMP resources
            seconds = block.workDurationSeconds,
            color = TimerWorkOrange,
            modifier = Modifier.weight(1f),
            onClick = { showWorkDialog = true }
        )
        DurationChip(
            label = "Rest", // TODO: CMP resources
            seconds = block.restDurationSeconds,
            color = TimerRestGreen,
            modifier = Modifier.weight(1f),
            onClick = { showRestDialog = true }
        )
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
    Spacer(Modifier.height(12.dp))

    // -- Repeats --
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Repeats", // TODO: CMP resources
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RepeatButton(label = "\u2212", enabled = block.repeats > 1) {
                onUpdate(block.copy(repeats = block.repeats - 1))
            }
            Text(
                text = block.repeats.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(48.dp)
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )
            RepeatButton(label = "+") {
                onUpdate(block.copy(repeats = block.repeats + 1))
            }
        }
    }

    // -- Dialogs --
    if (showNameDialog) {
        NameEditDialog(
            currentName = block.name,
            onConfirm = { newName -> onUpdate(block.copy(name = newName)); showNameDialog = false },
            onDismiss = { showNameDialog = false }
        )
    }
    if (showWorkDialog) {
        DurationPickerDialog(
            title = "Work Duration", // TODO: CMP resources
            seconds = block.workDurationSeconds,
            onConfirm = { onUpdate(block.copy(workDurationSeconds = it)); showWorkDialog = false },
            onDismiss = { showWorkDialog = false }
        )
    }
    if (showRestDialog) {
        DurationPickerDialog(
            title = "Rest Duration", // TODO: CMP resources
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
        label = "Duration", // TODO: CMP resources
        seconds = block.durationSeconds,
        color = TimerRestGreen,
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDurationDialog = true }
    )

    if (showDurationDialog) {
        DurationPickerDialog(
            title = "Rest Duration", // TODO: CMP resources
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
private fun DurationChip(
    label: String,
    seconds: Int,
    color: androidx.compose.ui.graphics.Color,
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
                if (enabled) BrownContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) OnBrownContainer else MaterialTheme.colorScheme.onSurfaceVariant,
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
    var text by remember { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exercise Name") }, // TODO: CMP resources
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) {
                Text("Done") // TODO: CMP resources
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") } // TODO: CMP resources
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                WheelTimePicker(
                    minutes = minutes,
                    seconds = secs,
                    onMinutesChange = { minutes = it },
                    onSecondsChange = { secs = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(minutes * 60 + secs) },
                modifier = Modifier.heightIn(min = 60.dp),
                contentPadding = PaddingValues(horizontal = 35.dp, vertical = 14.dp)
            ) {
                Text(
                    "Done", // TODO: CMP resources
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 58.dp),
                contentPadding = PaddingValues(horizontal = 30.dp, vertical = 14.dp)
            ) {
                Text(
                    "Cancel", // TODO: CMP resources
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    )
}
