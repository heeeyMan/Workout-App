package com.workout.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workout.android.R

@Composable
fun DurationPicker(
    label: String,
    totalSeconds: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = minutes.toString(),
                onValueChange = { input ->
                    val min = input.take(2).toIntOrNull()?.coerceIn(0, 99) ?: return@OutlinedTextField
                    onValueChange(min * 60 + seconds)
                },
                modifier = Modifier.width(64.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                singleLine = true,
                label = { Text(stringResource(R.string.minutes_short)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(":", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = "%02d".format(seconds),
                onValueChange = { input ->
                    val sec = input.take(2).toIntOrNull()?.coerceIn(0, 59) ?: return@OutlinedTextField
                    onValueChange(minutes * 60 + sec)
                },
                modifier = Modifier.width(64.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                singleLine = true,
                label = { Text(stringResource(R.string.seconds_short)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

fun Int.toTimeString(): String {
    val m = this / 60
    val s = this % 60
    return "%02d:%02d".format(m, s)
}
