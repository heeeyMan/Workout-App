package com.workout.android.ui.createworkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
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

    var expandedIndex by remember { mutableStateOf<Int?>(null) }

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
                    isExpanded = expandedIndex == index,
                    onToggleExpand = { expandedIndex = if (expandedIndex == index) null else index },
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

@Composable
private fun BlockCard(
    block: Block,
    index: Int,
    totalBlocks: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (Block) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val isExercise = block is Block.Exercise
    val cardColor = if (isExercise) MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isExercise) "Упражнение" else "Отдых",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isExercise) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = when (block) {
                        is Block.Exercise -> "${block.name} · ${block.totalDurationSeconds.toTimeString()}"
                        is Block.Rest -> block.durationSeconds.toTimeString()
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Вверх")
            }
            IconButton(onClick = onMoveDown, enabled = index < totalBlocks - 1) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Вниз")
            }
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Дублировать")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onToggleExpand) {
                Icon(
                    if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть"
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            HorizontalDivider()
            when (block) {
                is Block.Exercise -> ExerciseBlockFields(block = block, onUpdate = onUpdate)
                is Block.Rest -> RestBlockFields(block = block, onUpdate = onUpdate)
            }
        }
    }
}

@Composable
private fun ExerciseBlockFields(block: Block.Exercise, onUpdate: (Block) -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = block.name,
            onValueChange = { onUpdate(block.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название упражнения") },
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DurationPicker(
                label = "Работа",
                totalSeconds = block.workDurationSeconds,
                onValueChange = { onUpdate(block.copy(workDurationSeconds = it)) }
            )
            DurationPicker(
                label = "Отдых",
                totalSeconds = block.restDurationSeconds,
                onValueChange = { onUpdate(block.copy(restDurationSeconds = it)) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Повторений", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (block.repeats > 1) onUpdate(block.copy(repeats = block.repeats - 1)) }
                ) { Text("−", style = MaterialTheme.typography.headlineMedium) }
                Text(
                    text = block.repeats.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { onUpdate(block.copy(repeats = block.repeats + 1)) }
                ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
            }
        }
    }
}

@Composable
private fun RestBlockFields(block: Block.Rest, onUpdate: (Block) -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DurationPicker(
            label = "Длительность отдыха",
            totalSeconds = block.durationSeconds,
            onValueChange = { onUpdate(block.copy(durationSeconds = it)) }
        )
    }
}
