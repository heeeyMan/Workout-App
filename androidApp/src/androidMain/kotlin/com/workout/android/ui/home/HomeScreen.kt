package com.workout.android.ui.home

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workout.android.R
import com.workout.android.ui.components.WorkoutCard
import com.workout.shared.feature.home.HomeEffect
import com.workout.shared.feature.home.HomeIntent
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTimer: (Long) -> Unit,
    onNavigateToCreateWorkout: () -> Unit,
    onNavigateToEditWorkout: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToTimer -> onNavigateToTimer(effect.workoutId)
                is HomeEffect.NavigateToCreateWorkout -> onNavigateToCreateWorkout()
                is HomeEffect.NavigateToEditWorkout -> onNavigateToEditWorkout(effect.workoutId)
            }
        }
    }

    if (state.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dispatch(HomeIntent.CancelDelete) },
            title = { Text(stringResource(R.string.confirm_delete_workout_title)) },
            text = { Text(stringResource(R.string.confirm_delete_workout_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dispatch(HomeIntent.ConfirmDelete) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dispatch(HomeIntent.CancelDelete) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_workouts)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.dispatch(HomeIntent.CreateWorkout) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_workout)) }
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
                        text = stringResource(R.string.empty_home_subtitle),
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
                                text = stringResource(R.string.last_workout),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item(key = "last_${lastStarted.id}") {
                            WorkoutCard(
                                workout = lastStarted,
                                onClick = { viewModel.dispatch(HomeIntent.StartWorkout(lastStarted.id)) },
                                onEditClick = { viewModel.dispatch(HomeIntent.EditWorkout(lastStarted.id)) },
                                onDeleteClick = { viewModel.dispatch(HomeIntent.RequestDelete(lastStarted.id)) }
                            )
                        }
                        item { }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.all_workouts),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.workouts, key = { it.id }) { workout ->
                        WorkoutCard(
                            workout = workout,
                            onClick = { viewModel.dispatch(HomeIntent.StartWorkout(workout.id)) },
                            onEditClick = { viewModel.dispatch(HomeIntent.EditWorkout(workout.id)) },
                            onDeleteClick = { viewModel.dispatch(HomeIntent.RequestDelete(workout.id)) }
                        )
                    }
                }
            }
        }
    }
}
