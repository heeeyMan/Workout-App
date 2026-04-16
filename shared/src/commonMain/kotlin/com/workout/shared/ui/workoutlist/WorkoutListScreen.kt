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
import com.workout.shared.ui.components.WorkoutCard
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
            title = { Text("Delete workout?") }, // TODO: CMP resources
            text = { Text("This workout will be permanently deleted.") }, // TODO: CMP resources
            confirmButton = {
                TextButton(onClick = { store.dispatch(WorkoutListIntent.ConfirmDelete) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error) // TODO: CMP resources
                }
            },
            dismissButton = {
                TextButton(onClick = { store.dispatch(WorkoutListIntent.CancelDelete) }) {
                    Text("Cancel") // TODO: CMP resources
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Workouts") }, // TODO: CMP resources
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // TODO: CMP resources
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { store.dispatch(WorkoutListIntent.CreateWorkout) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Create") } // TODO: CMP resources
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
                    "No workouts yet", // TODO: CMP resources
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
