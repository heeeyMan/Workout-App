package com.workout.android.ui.components

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.workout.android.R
import com.workout.android.theme.DangerRed
import com.workout.core.model.Block
import com.workout.core.model.Workout

@Composable
fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocksPart = pluralStringResource(R.plurals.blocks_count, workout.blocks.size, workout.blocks.size)
    val subtitle = stringResource(R.string.workout_card_line, workout.totalDurationSeconds.toTimeString(), blocksPart)
    val restLabel = stringResource(R.string.rest_label)
    val structurePreview = workout.toStructurePreview(restLabel)

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
                    contentDescription = stringResource(R.string.cd_start_workout),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.cd_edit_workout),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = DangerRed
                )
            }
        }
    }
}

private fun Int.toCompactDuration(): String {
    if (this < 60) return "${this}s"
    val m = this / 60
    val s = this % 60
    return if (s == 0) "${m}m" else "${m}:${s.toString().padStart(2, '0')}"
}

private fun Workout.toStructurePreview(restLabel: String): String {
    if (blocks.isEmpty()) return ""
    val maxVisible = 3
    val items = blocks.take(maxVisible).map { block ->
        when (block) {
            is Block.Exercise -> {
                val work = block.workDurationSeconds.toCompactDuration()
                if (block.restDurationSeconds > 0) {
                    val rest = block.restDurationSeconds.toCompactDuration()
                    "${block.repeats}× $work/$rest"
                } else {
                    "${block.repeats}× $work"
                }
            }
            is Block.Rest -> "$restLabel ${block.durationSeconds.toCompactDuration()}"
        }
    }
    val preview = items.joinToString(" · ")
    return if (blocks.size > maxVisible) "$preview +${blocks.size - maxVisible}" else preview
}
