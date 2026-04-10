package com.workout.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
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
import com.workout.core.model.Workout

@Composable
fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocksPart = pluralStringResource(R.plurals.blocks_count, workout.blocks.size, workout.blocks.size)
    val subtitle = stringResource(R.string.workout_card_line, workout.totalDurationSeconds.toTimeString(), blocksPart)

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
            }
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(R.string.cd_start_workout),
                    tint = MaterialTheme.colorScheme.primary
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
