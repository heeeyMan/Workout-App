package com.workout.shared.ui.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workout.core.model.Workout
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.onboarding.WorkoutTemplate
import com.workout.shared.onboarding.WorkoutTemplates
import com.workout.shared.platform.TimerSettings
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.duration_min
import workoutapp.shared.generated.resources.exercise_label
import workoutapp.shared.generated.resources.onboarding_add_selected
import workoutapp.shared.generated.resources.onboarding_skip
import workoutapp.shared.generated.resources.onboarding_subtitle
import workoutapp.shared.generated.resources.onboarding_title

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val repository = koinInject<WorkoutRepository>()
    val settings = koinInject<TimerSettings>()
    val scope = rememberCoroutineScope()
    val exerciseLabel = stringResource(Res.string.exercise_label)

    var selected by remember { mutableStateOf(setOf<Int>()) }
    val templateNames = WorkoutTemplates.all.map { stringResource(it.nameRes) }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (selected.isEmpty()) {
                            settings.onboardingCompleted = true
                            onComplete()
                        } else {
                            scope.launch {
                                selected.forEach { index ->
                                    val template = WorkoutTemplates.all[index]
                                    repository.saveWorkout(
                                        Workout(
                                            id = 0L,
                                            name = templateNames[index],
                                            createdAt = 0L,
                                            blocks = template.makeBlocks(exerciseLabel)
                                        )
                                    )
                                }
                                settings.onboardingCompleted = true
                                onComplete()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (selected.isEmpty())
                            stringResource(Res.string.onboarding_skip)
                        else
                            stringResource(Res.string.onboarding_add_selected, selected.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(Res.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            val templates = WorkoutTemplates.all
            val rows = templates.chunked(2)
            items(rows.size) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val row = rows[rowIndex]
                    row.forEachIndexed { colIndex, template ->
                        val globalIndex = rowIndex * 2 + colIndex
                        val isSelected = globalIndex in selected
                        TemplateCard(
                            template = template,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selected = if (isSelected) selected - globalIndex else selected + globalIndex
                            }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun TemplateCard(
    template: WorkoutTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = stringResource(template.nameRes)
    val description = stringResource(template.descriptionRes)
    val totalMin = template.totalDurationSeconds / 60

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Transparent
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (totalMin > 0) {
                Text(
                    text = "$totalMin ${stringResource(Res.string.duration_min)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
