package com.workout.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val ItemHeight = 72.dp
private const val VisibleItems = 5                // нечётное -> чёткий центр
private val PickerHeight = ItemHeight * VisibleItems
private const val PaddingItems = VisibleItems / 2 // = 2
private val ColumnWidth = 112.dp

/**
 * Барабанный пикер времени (мм : сс).
 * Получает минуты и секунды отдельно, чтобы родитель управлял состоянием.
 */
@Composable
fun WheelTimePicker(
    minutes: Int,
    seconds: Int,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelColumn(
            count = 100,          // 00-99 минут
            selectedIndex = minutes,
            onIndexChange = onMinutesChange,
            label = { it.toString().padStart(2, '0') },
            modifier = Modifier.width(ColumnWidth)
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        WheelColumn(
            count = 60,           // 00-59 секунд
            selectedIndex = seconds,
            onIndexChange = onSecondsChange,
            label = { it.toString().padStart(2, '0') },
            modifier = Modifier.width(ColumnWidth)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Один барабан
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WheelColumn(
    count: Int,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    label: (Int) -> String,
    modifier: Modifier = Modifier
) {
    // initialFirstVisibleItemIndex используется только при первом compose.
    // После этого LazyColumn сам хранит позицию прокрутки.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex
    )
    val snapFling = rememberSnapFlingBehavior(listState)

    // Текущий центральный индекс данных = firstVisibleItemIndex
    // (потому что мы добавили PaddingItems пустых ячеек сверху и снизу)
    val centeredDataIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, count - 1) }
    }

    // Сообщаем об изменении, когда прокрутка остановилась
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            onIndexChange(centeredDataIndex)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Подсветка выбранной ячейки
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapFling,
            modifier = Modifier.height(PickerHeight),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Верхний отступ: делает первый элемент прокручиваемым в центр
            items(PaddingItems) {
                Box(Modifier.height(ItemHeight))
            }

            // Данные
            items(count) { index ->
                val distance = abs(index - centeredDataIndex)
                val alpha = when (distance) {
                    0 -> 1f
                    1 -> 0.55f
                    else -> 0.25f
                }
                // Активный элемент лежит на primaryContainer, неактивные — на фоне диалога (primary)
                val textColor = if (distance == 0) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
                Box(
                    modifier = Modifier
                        .height(ItemHeight)
                        .fillMaxWidth()
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(index),
                        style = if (distance == 0) {
                            MaterialTheme.typography.displaySmall
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                        fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }

            // Нижний отступ
            items(PaddingItems) {
                Box(Modifier.height(ItemHeight))
            }
        }
    }
}
