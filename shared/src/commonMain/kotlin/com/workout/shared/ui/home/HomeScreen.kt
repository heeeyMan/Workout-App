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
import com.workout.shared.ui.util.WorkoutDialog
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.feature.home.HomeEffect
import com.workout.shared.feature.home.HomeIntent
import com.workout.shared.feature.home.HomeViewModel
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
import com.workout.shared.backup.formatForSharing
import com.workout.shared.platform.rememberTextSharer
import com.workout.shared.ui.components.WorkoutCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import workoutapp.shared.generated.resources.duration_min
import workoutapp.shared.generated.resources.duration_sec
import workoutapp.shared.generated.resources.exercise_label
import workoutapp.shared.generated.resources.rest_label

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
    val shareText = rememberTextSharer()
    val exerciseLabel = stringResource(Res.string.exercise_label)
    val restLabel = stringResource(Res.string.rest_label)
    val minLabel = stringResource(Res.string.duration_min)
    val secLabel = stringResource(Res.string.duration_sec)

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
        WorkoutDialog(
            onDismissRequest = { store.dispatch(HomeIntent.CancelDelete) },
            title = stringResource(Res.string.confirm_delete_workout_title),
            confirmText = stringResource(Res.string.delete),
            onConfirm = { store.dispatch(HomeIntent.ConfirmDelete) },
            dismissText = stringResource(Res.string.cancel),
            onDismiss = { store.dispatch(HomeIntent.CancelDelete) },
            content = { Text(stringResource(Res.string.confirm_delete_workout_message)) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.my_workouts)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.settings_cd))
                    }
                }
            )
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
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.workouts.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.create_first_workout),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                val lastStarted = state.workouts
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
                                onClick = { store.dispatch(HomeIntent.StartWorkout(lastStarted.id)) },
                                onEditClick = { store.dispatch(HomeIntent.EditWorkout(lastStarted.id)) },
                                onDeleteClick = { store.dispatch(HomeIntent.RequestDelete(lastStarted.id)) },
                                onShareClick = { shareText(lastStarted.formatForSharing(exerciseLabel, restLabel, minLabel, secLabel)) }
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
                    items(state.workouts, key = { it.id }) { workout ->
                        WorkoutCard(
                            workout = workout,
                            onClick = { store.dispatch(HomeIntent.StartWorkout(workout.id)) },
                            onEditClick = { store.dispatch(HomeIntent.EditWorkout(workout.id)) },
                            onDeleteClick = { store.dispatch(HomeIntent.RequestDelete(workout.id)) },
                            onShareClick = { shareText(workout.formatForSharing(exerciseLabel, restLabel, minLabel, secLabel)) }
                        )
                    }
                }
            }
        }
    }
}
