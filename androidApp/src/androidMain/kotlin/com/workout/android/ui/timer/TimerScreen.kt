package com.workout.android.ui.timer

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workout.android.theme.TimerPrepGray
import com.workout.android.theme.TimerPrepGrayDim
import com.workout.android.theme.TimerRestGreen
import com.workout.android.theme.TimerRestGreenDim
import com.workout.android.theme.TimerWorkOrange
import com.workout.android.theme.TimerWorkOrangeDim
import com.workout.android.feedback.TimerFeedback
import com.workout.android.ui.components.toTimeString
import com.workout.shared.feature.timer.PhaseType
import com.workout.shared.feature.timer.TimerEffect
import com.workout.shared.feature.timer.TimerIntent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TimerScreen(
    workoutId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TimerViewModel = koinViewModel(parameters = { parametersOf(workoutId) })
) {
    val state by viewModel.state.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    // Держать экран включённым
    val context = LocalContext.current
    val appContext = context.applicationContext
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TimerEffect.NavigateBack -> onNavigateBack()
                is TimerEffect.PlayPrepTickSound -> TimerFeedback.playPrepTickTone(appContext)
                is TimerEffect.PlayPrepEndSound -> TimerFeedback.playPrepEndTone(appContext)
                is TimerEffect.VibratePrepEnd -> TimerFeedback.vibratePrepEnd(appContext)
                is TimerEffect.PlayWorkSound -> TimerFeedback.playWorkTone(appContext)
                is TimerEffect.PlayRestSound -> TimerFeedback.playRestTone(appContext)
                is TimerEffect.PlayFinishSound -> TimerFeedback.playFinishTone(appContext)
                is TimerEffect.Vibrate -> TimerFeedback.vibrateShort(appContext)
                is TimerEffect.VibrateFinish -> TimerFeedback.vibrateFinish(appContext)
                is TimerEffect.Alert10Seconds -> {
                    val st = viewModel.state.value
                    if (st.soundEnabled) TimerFeedback.playAlertTone(appContext)
                    if (st.vibrationEnabled) TimerFeedback.vibrateAlert(appContext)
                }
            }
        }
    }

    val inPrep = state.isPrepBeforeWork
    val isWorkPhase = state.currentPhase?.type == PhaseType.Work
    val phaseAccentTarget = when {
        inPrep -> TimerPrepGray
        isWorkPhase -> TimerWorkOrange
        else -> TimerRestGreen
    }
    val phaseDimTarget = when {
        inPrep -> TimerPrepGrayDim
        isWorkPhase -> TimerWorkOrangeDim
        else -> TimerRestGreenDim
    }
    val backgroundColor by animateColorAsState(
        targetValue = phaseDimTarget,
        animationSpec = tween(durationMillis = 600),
        label = "bg_color"
    )
    val accentColor by animateColorAsState(
        targetValue = phaseAccentTarget,
        animationSpec = tween(durationMillis = 600),
        label = "accent_color"
    )

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Завершить тренировку?") },
            text = { Text("Прогресс не сохранится.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dispatch(TimerIntent.Finish) }) {
                    Text("Завершить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Продолжить") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        if (state.isFinished) {
            FinishedContent(workoutName = state.workoutName, onBack = onNavigateBack)
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя полоса прогресса всей тренировки
            LinearProgressIndicator(
                progress = { state.overallProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                Spacer(Modifier.weight(1f))

                // Тип фазы
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = when {
                            state.isPrepBeforeWork -> "ПОДГОТОВКА"
                            state.currentPhase?.type == PhaseType.Work -> "РАБОТА"
                            else -> "ОТДЫХ"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Название упражнения
                Text(
                    text = state.currentPhase?.name ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Счётчик повторений
                state.currentPhase?.repeatLabel?.let { label ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Повтор $label",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Главный таймер (цвет стадии: оранжевый / зелёный / серый)
                Text(
                    text = state.secondsRemaining.toTimeString(),
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // Прогресс текущей фазы
                LinearProgressIndicator(
                    progress = { state.phaseProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Spacer(Modifier.height(24.dp))

                // Следующая фаза
                state.nextPhase?.let { next ->
                    Text(
                        text = "Следующее: ${next.name} · ${next.durationSeconds.toTimeString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Блок из блоков
                Text(
                    text = "Блок ${state.currentPhaseIndex + 1} из ${state.totalPhases}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                // Кнопки управления
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Пропустить
                    FilledIconButton(
                        onClick = { viewModel.dispatch(TimerIntent.SkipPhase) },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Пропустить")
                    }

                    // Пауза / Продолжить
                    FilledIconButton(
                        onClick = { viewModel.dispatch(TimerIntent.TogglePause) },
                        modifier = Modifier.size(80.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentColor)
                    ) {
                        Icon(
                            if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isPaused) "Продолжить" else "Пауза",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.background
                        )
                    }

                    // Завершить тренировку (справа от паузы)
                    FilledIconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Завершить тренировку",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun FinishedContent(workoutName: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Celebration,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = TimerWorkOrange
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Тренировка $workoutName завершена",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(40.dp))
        TextButton(onClick = onBack) {
            Text("На главную")
        }
    }
}
