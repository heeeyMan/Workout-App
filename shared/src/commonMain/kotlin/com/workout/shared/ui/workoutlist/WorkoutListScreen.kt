package com.workout.shared.ui.workoutlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.feature.workoutlist.WorkoutListEffect
import com.workout.shared.feature.workoutlist.WorkoutListIntent
import com.workout.shared.feature.workoutlist.WorkoutListStore
import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.back
import workoutapp.shared.generated.resources.cancel
import workoutapp.shared.generated.resources.confirm_delete_workout_message
import workoutapp.shared.generated.resources.confirm_delete_workout_title
import workoutapp.shared.generated.resources.create
import workoutapp.shared.generated.resources.delete
import workoutapp.shared.generated.resources.my_workouts
import workoutapp.shared.generated.resources.no_workouts
import com.workout.shared.ui.components.WorkoutCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListScreen(
    onNavigateToTimer: (Long) -> Unit,
    onNavigateToCreateWorkout: () -> Unit,
    onNavigateToEditWorkout: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val repository = koinInject<WorkoutRepository>()
    val store = remember { WorkoutListStore(repository) }
    DisposableEffect(Unit) { onDispose { store.destroy() } }

    val state by store.state.collectAsState()

    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            when (effect) {
                is WorkoutListEffect.NavigateToTimer -> onNavigateToTimer(effect.workoutId)
                is WorkoutListEffect.NavigateToCreateWorkout -> onNavigateToCreateWorkout()
                is WorkoutListEffect.NavigateToEditWorkout -> onNavigateToEditWorkout(effect.workoutId)
            }
        }
    }

    if (state.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { store.dispatch(WorkoutListIntent.CancelDelete) },
            title = { Text(stringResource(Res.string.confirm_delete_workout_title)) },
            text = { Text(stringResource(Res.string.confirm_delete_workout_message)) },
            confirmButton = {
                TextButton(onClick = { store.dispatch(WorkoutListIntent.ConfirmDelete) }) {
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { store.dispatch(WorkoutListIntent.CancelDelete) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.my_workouts)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { store.dispatch(WorkoutListIntent.CreateWorkout) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.create)) }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.workouts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.no_workouts),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.workouts, key = { it.id }) { workout ->
                    WorkoutCard(
                        workout = workout,
                        onClick = { store.dispatch(WorkoutListIntent.SelectWorkout(workout.id)) },
                        onEditClick = { store.dispatch(WorkoutListIntent.EditWorkout(workout.id)) },
                        onDeleteClick = { store.dispatch(WorkoutListIntent.RequestDelete(workout.id)) }
                    )
                }
            }
        }
    }
}
