package com.workout.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.workout.core.model.Block
import com.workout.core.model.Workout
import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.blocks_count
import workoutapp.shared.generated.resources.cd_edit_workout
import workoutapp.shared.generated.resources.cd_start_workout
import workoutapp.shared.generated.resources.delete
import workoutapp.shared.generated.resources.duration_min
import workoutapp.shared.generated.resources.duration_sec
import workoutapp.shared.generated.resources.rest_label
import com.workout.shared.ui.theme.DangerRed
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocksCount = workout.blocks.size
    val blocksPart = pluralStringResource(Res.plurals.blocks_count, blocksCount, blocksCount)
    val subtitle = "${workout.totalDurationSeconds.toTimeString()} \u00B7 $blocksPart"
    val restLabel = stringResource(Res.string.rest_label)
    val minSuffix = stringResource(Res.string.duration_min)
    val secSuffix = stringResource(Res.string.duration_sec)
    val structurePreview = workout.toStructurePreview(restLabel, minSuffix, secSuffix)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = workout.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (structurePreview.isNotEmpty()) {
                    Text(
                        text = structurePreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(Res.string.cd_start_workout),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(Res.string.cd_edit_workout),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(Res.string.delete),
                    tint = DangerRed
                )
            }
        }
    }
}

private fun Int.toCompactDuration(minSuffix: String, secSuffix: String): String {
    if (this < 60) return "$this$secSuffix"
    val m = this / 60
    val s = this % 60
    return if (s == 0) "$m$minSuffix" else "$m:${s.toString().padStart(2, '0')}"
}

private fun Workout.toStructurePreview(restLabel: String, minSuffix: String, secSuffix: String): String {
    if (blocks.isEmpty()) return ""
    val maxVisible = 3
    val items = blocks.take(maxVisible).map { block ->
        when (block) {
            is Block.Exercise -> {
                val work = block.workDurationSeconds.toCompactDuration(minSuffix, secSuffix)
                if (block.restDurationSeconds > 0) {
                    val rest = block.restDurationSeconds.toCompactDuration(minSuffix, secSuffix)
                    "${block.repeats}\u00D7 $work/$rest"
                } else {
                    "${block.repeats}\u00D7 $work"
                }
            }
            is Block.Rest -> "$restLabel ${block.durationSeconds.toCompactDuration(minSuffix, secSuffix)}"
        }
    }
    val preview = items.joinToString(" \u00B7 ")
    return if (blocks.size > maxVisible) "$preview +${blocks.size - maxVisible}" else preview
}
