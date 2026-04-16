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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.feature.home.HomeEffect
import com.workout.shared.feature.home.HomeIntent
import com.workout.shared.feature.home.HomeStore
import com.workout.shared.ui.components.WorkoutCard
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTimer: (Long) -> Unit,
    onNavigateToCreateWorkout: () -> Unit,
    onNavigateToEditWorkout: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val repository = koinInject<WorkoutRepository>()
    val store = remember { HomeStore(repository) }
    DisposableEffect(Unit) { onDispose { store.destroy() } }

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
        AlertDialog(
            onDismissRequest = { store.dispatch(HomeIntent.CancelDelete) },
            title = { Text("Delete workout?") }, // TODO: CMP resources
            text = { Text("This workout will be permanently deleted.") }, // TODO: CMP resources
            confirmButton = {
                TextButton(onClick = { store.dispatch(HomeIntent.ConfirmDelete) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error) // TODO: CMP resources
                }
            },
            dismissButton = {
                TextButton(onClick = { store.dispatch(HomeIntent.CancelDelete) }) {
                    Text("Cancel") // TODO: CMP resources
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Workouts") }, // TODO: CMP resources
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings") // TODO: CMP resources
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { store.dispatch(HomeIntent.CreateWorkout) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Workout") } // TODO: CMP resources
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
                        text = "Create your first workout!", // TODO: CMP resources
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                val lastStarted = state.workouts
                    .filter { it.lastStartedAt != null }
                    .maxByOrNull { it.lastStartedAt!! }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (lastStarted != null) {
                        item {
                            Text(
                                text = "Last Workout", // TODO: CMP resources
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item(key = "last_${lastStarted.id}") {
                            WorkoutCard(
                                workout = lastStarted,
                                onClick = { store.dispatch(HomeIntent.StartWorkout(lastStarted.id)) },
                                onEditClick = { store.dispatch(HomeIntent.EditWorkout(lastStarted.id)) },
                                onDeleteClick = { store.dispatch(HomeIntent.RequestDelete(lastStarted.id)) }
                            )
                        }
                        item { }
                    }
                    item {
                        Text(
                            text = "All Workouts", // TODO: CMP resources
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.workouts, key = { it.id }) { workout ->
                        WorkoutCard(
                            workout = workout,
                            onClick = { store.dispatch(HomeIntent.StartWorkout(workout.id)) },
                            onEditClick = { store.dispatch(HomeIntent.EditWorkout(workout.id)) },
                            onDeleteClick = { store.dispatch(HomeIntent.RequestDelete(workout.id)) }
                        )
                    }
                }
            }
        }
    }
}
