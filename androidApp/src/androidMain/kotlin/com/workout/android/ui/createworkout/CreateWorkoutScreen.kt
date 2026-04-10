package com.workout.android.ui.createworkout

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.workout.android.theme.BrownContainer
import com.workout.android.theme.BrownPrimary
import com.workout.android.theme.OnBrownContainer
import com.workout.android.theme.RestBlue
import com.workout.android.theme.SurfaceVariant
import com.workout.android.ui.components.DurationPicker
import com.workout.android.ui.components.toTimeString
import com.workout.core.model.Block
import com.workout.shared.feature.createworkout.CreateWorkoutEffect
import com.workout.shared.feature.createworkout.CreateWorkoutIntent
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutScreen(
    workoutId: Long,
    onNavigateBack: () -> Unit,
    viewModel: CreateWorkoutViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(workoutId) {
        if (workoutId != 0L) viewModel.dispatch(CreateWorkoutIntent.LoadWorkout(workoutId))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreateWorkoutEffect.NavigateBack -> onNavigateBack()
                is CreateWorkoutEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (workoutId == 0L) "Новая тренировка" else "Редактировать")
                        if (state.totalDurationSeconds > 0) {
                            Text(
                                text = state.totalDurationSeconds.toTimeString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.dispatch(CreateWorkoutIntent.Discard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.dispatch(CreateWorkoutIntent.Save) }) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
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
                    onValueChange = { viewModel.dispatch(CreateWorkoutIntent.UpdateName(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название тренировки") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }

            itemsIndexed(state.blocks, key = { _, block -> block.id.toString() + block.orderIndex }) { index, block ->
                BlockCard(
                    block = block,
                    index = index,
                    totalBlocks = state.blocks.size,
                    onUpdate = { updated -> viewModel.dispatch(CreateWorkoutIntent.UpdateBlock(index, updated)) },
                    onDelete = { viewModel.dispatch(CreateWorkoutIntent.RemoveBlock(index)) },
                    onDuplicate = { viewModel.dispatch(CreateWorkoutIntent.DuplicateBlock(index)) },
                    onMoveUp = { if (index > 0) viewModel.dispatch(CreateWorkoutIntent.MoveBlock(index, index - 1)) },
                    onMoveDown = { if (index < state.blocks.size - 1) viewModel.dispatch(CreateWorkoutIntent.MoveBlock(index, index + 1)) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.dispatch(CreateWorkoutIntent.AddExerciseBlock()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Упражнение")
                    }
                    Button(
                        onClick = { viewModel.dispatch(CreateWorkoutIntent.AddRestBlock()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Отдых")
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ─────────────────────────────────────────
// Карточка блока
// ─────────────────────────────────────────

@Composable
private fun BlockCard(
    block: Block,
    index: Int,
    totalBlocks: Int,
    onUpdate: (Block) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val isExercise = block is Block.Exercise
    val accentColor = if (isExercise) BrownPrimary else RestBlue

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Заголовок + иконки действий ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Цветной индикатор
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isExercise) "УПРАЖНЕНИЕ" else "ОТДЫХ",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Иконки действий — компактные
                IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Вверх",
                        modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, enabled = index < totalBlocks - 1, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Вниз",
                        modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Дублировать",
                        modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
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

// ─────────────────────────────────────────
// Контент карточки упражнения
// ─────────────────────────────────────────

@Composable
private fun ExerciseBlockContent(block: Block.Exercise, onUpdate: (Block) -> Unit) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showWorkDialog by remember { mutableStateOf(false) }
    var showRestDialog by remember { mutableStateOf(false) }

    // ── Название (тап → диалог) ──
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
            contentDescription = "Редактировать",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }

    Spacer(Modifier.height(16.dp))

    // ── Работа и Отдых (тап → тайм-пикер) ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DurationChip(
            label = "Работа",
            seconds = block.workDurationSeconds,
            color = BrownPrimary,
            modifier = Modifier.weight(1f),
            onClick = { showWorkDialog = true }
        )
        DurationChip(
            label = "Отдых",
            seconds = block.restDurationSeconds,
            color = RestBlue,
            modifier = Modifier.weight(1f),
            onClick = { showRestDialog = true }
        )
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
    Spacer(Modifier.height(12.dp))

    // ── Повторений ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Повторений",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RepeatButton(label = "−", enabled = block.repeats > 1) {
                onUpdate(block.copy(repeats = block.repeats - 1))
            }
            Text(
                text = block.repeats.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(48.dp)
                    .padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            RepeatButton(label = "+") {
                onUpdate(block.copy(repeats = block.repeats + 1))
            }
        }
    }

    // ── Диалоги ──
    if (showNameDialog) {
        NameEditDialog(
            currentName = block.name,
            onConfirm = { newName -> onUpdate(block.copy(name = newName)); showNameDialog = false },
            onDismiss = { showNameDialog = false }
        )
    }
    if (showWorkDialog) {
        DurationPickerDialog(
            title = "Время работы",
            seconds = block.workDurationSeconds,
            onConfirm = { onUpdate(block.copy(workDurationSeconds = it)); showWorkDialog = false },
            onDismiss = { showWorkDialog = false }
        )
    }
    if (showRestDialog) {
        DurationPickerDialog(
            title = "Время отдыха",
            seconds = block.restDurationSeconds,
            onConfirm = { onUpdate(block.copy(restDurationSeconds = it)); showRestDialog = false },
            onDismiss = { showRestDialog = false }
        )
    }
}

// ─────────────────────────────────────────
// Контент карточки отдыха
// ─────────────────────────────────────────

@Composable
private fun RestBlockContent(block: Block.Rest, onUpdate: (Block) -> Unit) {
    var showDurationDialog by remember { mutableStateOf(false) }

    DurationChip(
        label = "Длительность",
        seconds = block.durationSeconds,
        color = RestBlue,
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDurationDialog = true }
    )

    if (showDurationDialog) {
        DurationPickerDialog(
            title = "Время отдыха",
            seconds = block.durationSeconds,
            onConfirm = { onUpdate(block.copy(durationSeconds = it)); showDurationDialog = false },
            onDismiss = { showDurationDialog = false }
        )
    }
}

// ─────────────────────────────────────────
// Переиспользуемые компоненты
// ─────────────────────────────────────────

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
        title = { Text("Название упражнения") },
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
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
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
    var currentSeconds by remember { mutableIntStateOf(seconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DurationPicker(
                    label = "",
                    totalSeconds = currentSeconds,
                    onValueChange = { currentSeconds = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentSeconds) }) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
