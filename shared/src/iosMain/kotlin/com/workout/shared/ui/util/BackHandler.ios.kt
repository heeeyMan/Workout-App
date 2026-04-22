package com.workout.shared.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS не имеет системной кнопки «Назад»; свайп обрабатывается навигационным фреймворком
}
