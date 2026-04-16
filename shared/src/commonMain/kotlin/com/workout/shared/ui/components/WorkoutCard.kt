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
import com.workout.shared.ui.theme.DangerRed

@Composable
fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocksCount = workout.blocks.size
    // TODO: replace with CMP string resources when available
    val blocksPart = "$blocksCount ${pluralBlocks(blocksCount)}"
    val subtitle = "${workout.totalDurationSeconds.toTimeString()} \u00B7 $blocksPart"
    // TODO: replace with CMP string resource for rest label
    val restLabel = "\u041E\u0442\u0434\u044B\u0445" // "Отдых"
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
            // TODO: replace contentDescription with CMP string resources
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Start workout",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit workout",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = DangerRed
                )
            }
        }
    }
}

// TODO: replace with proper pluralization from CMP resources
private fun pluralBlocks(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> "\u0431\u043B\u043E\u043A\u043E\u0432"
        mod10 == 1 -> "\u0431\u043B\u043E\u043A"
        mod10 in 2..4 -> "\u0431\u043B\u043E\u043A\u0430"
        else -> "\u0431\u043B\u043E\u043A\u043E\u0432"
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
                    "${block.repeats}\u00D7 $work/$rest"
                } else {
                    "${block.repeats}\u00D7 $work"
                }
            }
            is Block.Rest -> "$restLabel ${block.durationSeconds.toCompactDuration()}"
        }
    }
    val preview = items.joinToString(" \u00B7 ")
    return if (blocks.size > maxVisible) "$preview +${blocks.size - maxVisible}" else preview
}
