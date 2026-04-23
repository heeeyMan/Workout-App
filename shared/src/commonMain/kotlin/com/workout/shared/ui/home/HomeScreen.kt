package com.workout.shared.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workout.core.model.Workout
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.feature.home.HomeEffect
import com.workout.shared.feature.home.HomeIntent
import com.workout.shared.feature.home.HomeViewModel
import com.workout.shared.ui.components.WorkoutCard
import com.workout.shared.ui.util.WorkoutDialog
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.all_workouts
import workoutapp.shared.generated.resources.cancel
import workoutapp.shared.generated.resources.confirm_delete_workout_message
import workoutapp.shared.generated.resources.confirm_delete_workout_title
import workoutapp.shared.generated.resources.create_first_workout
import workoutapp.shared.generated.resources.delete
import workoutapp.shared.generated.resources.last_workout
import workoutapp.shared.generated.resources.my_workouts
import workoutapp.shared.generated.resources.new_workout
import workoutapp.shared.generated.resources.settings_cd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTimer: (Long) -> Unit,
    onNavigateToCreateWorkout: () -> Unit,
    onNavigateToEditWorkout: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val repository = koinInject<WorkoutRepository>()
    val vm = viewModel { HomeViewModel(repository) }
    val store = vm.store
    val state by store.state.collectAsState()

    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToTimer -> onNavigateToTimer(effect.workoutId)
                is HomeEffect.NavigateToCreateWorkout -> onNavigateToCreateWorkout()
                is HomeEffect.NavigateToEditWorkout -> onNavigateToEditWorkout(effect.workoutId)
            }
        }
    }

    if (state.pendingDeleteId != null) {
        DeleteConfirmDialog(
            onConfirm = { store.dispatch(HomeIntent.ConfirmDelete) },
            onDismiss = { store.dispatch(HomeIntent.CancelDelete) }
        )
    }

    Scaffold(
        topBar = {
            HomeTopBar(onNavigateToSettings = onNavigateToSettings)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { store.dispatch(HomeIntent.CreateWorkout) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.new_workout)) }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(padding)
            state.workouts.isEmpty() -> EmptyContent(padding)
            else -> WorkoutListContent(
                workouts = state.workouts,
                onDispatch = store::dispatch,
                padding = padding,
            )
        }
    }
}

@Composable
private fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    WorkoutDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.confirm_delete_workout_title),
        confirmText = stringResource(Res.string.delete),
        onConfirm = onConfirm,
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        content = { Text(stringResource(Res.string.confirm_delete_workout_message)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onNavigateToSettings: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(Res.string.my_workouts)) },
        actions = {
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.settings_cd))
            }
        }
    )
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.create_first_workout),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WorkoutListContent(
    workouts: List<Workout>,
    onDispatch: (HomeIntent) -> Unit,
    padding: PaddingValues
) {
    val lastStarted = workouts
        .filter { it.lastStartedAt != null }
        .maxByOrNull { it.lastStartedAt ?: 0L }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (lastStarted != null) {
            item {
                Text(
                    text = stringResource(Res.string.last_workout),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "last_${lastStarted.id}") {
                WorkoutCard(
                    workout = lastStarted,
                    onClick = { onDispatch(HomeIntent.StartWorkout(lastStarted.id)) },
                    onEditClick = { onDispatch(HomeIntent.EditWorkout(lastStarted.id)) },
                    onDeleteClick = { onDispatch(HomeIntent.RequestDelete(lastStarted.id)) },
                )
            }
            item { }
        }
        item {
            Text(
                text = stringResource(Res.string.all_workouts),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(workouts.filter { it.id != lastStarted?.id }, key = { it.id }) { workout ->
            WorkoutCard(
                workout = workout,
                onClick = { onDispatch(HomeIntent.StartWorkout(workout.id)) },
                onEditClick = { onDispatch(HomeIntent.EditWorkout(workout.id)) },
                onDeleteClick = { onDispatch(HomeIntent.RequestDelete(workout.id)) },
            )
        }
    }
}
