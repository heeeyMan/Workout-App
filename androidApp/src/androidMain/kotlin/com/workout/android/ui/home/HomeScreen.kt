package com.workout.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.workout.android.ui.components.WorkoutCard
import com.workout.shared.feature.home.HomeEffect
import com.workout.shared.feature.home.HomeIntent
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onNavigateToTimer: (Long) -> Unit,
    onNavigateToCreateWorkout: () -> Unit,
    onNavigateToWorkoutList: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToTimer -> onNavigateToTimer(effect.workoutId)
                is HomeEffect.NavigateToCreateWorkout -> onNavigateToCreateWorkout()
                is HomeEffect.NavigateToWorkoutList -> onNavigateToWorkoutList()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.dispatch(HomeIntent.CreateWorkout) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Новая тренировка") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Workout", style = MaterialTheme.typography.headlineLarge)

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.lastWorkout != null) {
                val workout = state.lastWorkout!!
                Text(
                    text = "Последняя тренировка",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WorkoutCard(
                    workout = workout,
                    onClick = { viewModel.dispatch(HomeIntent.StartWorkout(workout.id)) },
                    onDeleteClick = {}
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.dispatch(HomeIntent.OpenWorkoutList) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Все тренировки")
                }
            } else {
                EmptyHomeContent(onCreateClick = { viewModel.dispatch(HomeIntent.CreateWorkout) })
            }
        }
    }
}

@Composable
private fun EmptyHomeContent(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Тренировок пока нет", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Создай первую тренировку, чтобы начать",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Text("Создать тренировку")
        }
    }
}
