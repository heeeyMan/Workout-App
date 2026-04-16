package com.workout.shared.ui.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.feature.timer.PhaseType
import com.workout.shared.feature.timer.TimerEffect
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerStore
import com.workout.shared.platform.AudioFeedback
import com.workout.shared.platform.ForegroundTimerService
import com.workout.shared.platform.HapticFeedback
import com.workout.shared.platform.ScreenWakeLock
import com.workout.shared.platform.TimerSettings
import com.workout.shared.ui.components.toTimeString
import com.workout.shared.ui.theme.TimerPrepGray
import com.workout.shared.ui.theme.TimerPrepGrayDim
import com.workout.shared.ui.theme.TimerRestGreen
import com.workout.shared.ui.theme.TimerRestGreenDim
import com.workout.shared.ui.theme.TimerWorkOrange
import com.workout.shared.ui.theme.TimerWorkOrangeDim
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
    workoutId: Long,
    onNavigateBack: () -> Unit
) {
    val repository = koinInject<WorkoutRepository>()
    val timerSettings = koinInject<TimerSettings>()
    val audioFeedback = koinInject<AudioFeedback>()
    val hapticFeedback = koinInject<HapticFeedback>()
    val screenWakeLock = koinInject<ScreenWakeLock>()
    val foregroundService = koinInject<ForegroundTimerService>()

    val store = remember { TimerStore(repository) }

    val quickAdjustEnabled = remember { mutableStateOf(timerSettings.timerQuickAdjustEnabled) }

    // Load workout with settings
    LaunchedEffect(workoutId) {
        store.dispatch(
            TimerIntent.Load(
                workoutId = workoutId,
                blockPrepDurationSeconds = timerSettings.blockPrepDurationSeconds,
                soundEnabled = timerSettings.soundEnabled,
                vibrationEnabled = timerSettings.vibrationEnabled,
                workStartSoundPresetId = timerSettings.workStartSoundPresetId,
                restStartSoundPresetId = timerSettings.restStartSoundPresetId,
                finishSoundPresetId = timerSettings.workoutFinishSoundPresetId,
                workPhaseWarningSoundPresetId = timerSettings.workPhaseWarningSoundPresetId,
                workPhaseEndWarningSeconds = timerSettings.workPhaseEndWarningSeconds,
                restPhaseDisplayName = "Rest" // TODO: CMP resources
            )
        )
    }

    // Keep screen on
    DisposableEffect(Unit) {
        screenWakeLock.acquire()
        onDispose { screenWakeLock.release() }
    }

    // Clean up store
    DisposableEffect(Unit) {
        onDispose {
            foregroundService.stop()
            store.destroy()
        }
    }

    // Handle effects (sound, vibration, navigation)
    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            when (effect) {
                is TimerEffect.NavigateBack -> onNavigateBack()
                is TimerEffect.PlayPrepTickSound -> audioFeedback.playPrepTickTone()
                is TimerEffect.PlayPrepEndSound -> {
                    val st = store.state.value
                    audioFeedback.playPrepEndTone(st.workStartSoundPresetId)
                }
                is TimerEffect.VibratePrepEnd -> hapticFeedback.vibratePrepEnd()
                is TimerEffect.PlayWorkSound -> {
                    val st = store.state.value
                    audioFeedback.playWorkTone(st.workStartSoundPresetId)
                }
                is TimerEffect.PlayRestSound -> {
                    val st = store.state.value
                    audioFeedback.playRestTone(st.restStartSoundPresetId)
                }
                is TimerEffect.PlayFinishSound -> {
                    val st = store.state.value
                    audioFeedback.playFinishTone(st.finishSoundPresetId)
                }
                is TimerEffect.Vibrate -> hapticFeedback.vibrateShort()
                is TimerEffect.VibrateFinish -> hapticFeedback.vibrateFinish()
                is TimerEffect.Alert10Seconds -> {
                    val st = store.state.value
                    if (st.soundEnabled) {
                        audioFeedback.playWarningTone(st.workPhaseWarningSoundPresetId)
                    }
                    if (effect.withVibration && st.vibrationEnabled) {
                        hapticFeedback.vibrateAlert()
                    }
                }
            }
        }
    }

    // Update foreground service notification
    LaunchedEffect(Unit) {
        store.state.collect { s ->
            foregroundService.update(s, s.workoutName)
        }
    }

    val state by store.state.collectAsState()
    val quickAdjust = quickAdjustEnabled.value
    var showExitDialog by remember { mutableStateOf(false) }
    var gymMode by remember { mutableStateOf(false) }
    var gymControlsLocked by remember { mutableStateOf(false) }

    // BackHandler is Android-specific; in CMP we handle exit via the dialog only
    // TODO: expect/actual BackHandler if needed on each platform

    val timerFontSize = if (gymMode) 120.sp else 88.sp
    val titleStyle =
        if (gymMode) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium

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

    val warningPulse by rememberInfiniteTransition(label = "warning_pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_pulse_value"
    )
    val effectiveAccentColor = if (state.isInWorkEndWarning) {
        lerp(accentColor, Color(0xFFE53935), warningPulse)
    } else {
        accentColor
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("End workout?") }, // TODO: CMP resources
            text = { Text("Current progress will be lost.") }, // TODO: CMP resources
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        store.dispatch(TimerIntent.Finish)
                    }
                ) {
                    Text("End Workout", color = MaterialTheme.colorScheme.error) // TODO: CMP resources
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continue") // TODO: CMP resources
                }
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
                FinishedContent(
                    workoutName = state.workoutName,
                    onBack = onNavigateBack
                )
                return@Box
            }

            Column(modifier = Modifier.fillMaxSize()) {
                LinearProgressIndicator(
                    progress = { state.overallProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = if (gymMode) Arrangement.SpaceBetween else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (gymMode) {
                        IconButton(
                            onClick = {
                                gymMode = false
                                gymControlsLocked = false
                            }
                        ) {
                            Icon(
                                Icons.Default.FullscreenExit,
                                contentDescription = "Exit gym mode" // TODO: CMP resources
                            )
                        }
                        if (!gymControlsLocked) {
                            IconButton(onClick = { gymControlsLocked = true }) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Lock controls" // TODO: CMP resources
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { gymMode = true }) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Gym mode" // TODO: CMP resources
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))

                    Spacer(Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            // TODO: CMP resources
                            text = when {
                                state.isPrepBeforeWork -> "PREP"
                                state.currentPhase?.type == PhaseType.Work -> "WORK"
                                else -> "REST"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = effectiveAccentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = state.currentPhase?.name ?: "",
                        style = titleStyle,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    state.currentPhase?.repeatLabel?.let { label ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            // TODO: CMP resources
                            text = "Round $label",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = state.secondsRemaining.toTimeString(),
                        fontSize = timerFontSize,
                        fontWeight = FontWeight.Bold,
                        color = effectiveAccentColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    LinearProgressIndicator(
                        progress = { state.phaseProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = effectiveAccentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(Modifier.height(24.dp))

                    if (!gymControlsLocked || !gymMode) {
                        state.nextPhase?.let { next ->
                            Text(
                                // TODO: CMP resources
                                text = "Next: ${next.name} ${next.durationSeconds.toTimeString()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            // TODO: CMP resources
                            text = "${state.currentPhaseIndex + 1} / ${state.totalPhases}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    if (!gymControlsLocked) {
                        if (quickAdjust) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        store.dispatch(TimerIntent.AdjustRemainingSeconds(-10))
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("-10s") // TODO: CMP resources
                                }
                                Spacer(Modifier.width(12.dp))
                                FilledTonalButton(
                                    onClick = {
                                        store.dispatch(TimerIntent.AdjustRemainingSeconds(10))
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("+10s") // TODO: CMP resources
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledIconButton(
                                    onClick = { store.dispatch(TimerIntent.SkipPhase) },
                                    modifier = Modifier.size(52.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Skip") // TODO: CMP resources
                                }

                                FilledIconButton(
                                    onClick = { store.dispatch(TimerIntent.TogglePause) },
                                    modifier = Modifier.size(76.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentColor)
                                ) {
                                    Icon(
                                        if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (state.isPaused) {
                                            "Resume" // TODO: CMP resources
                                        } else {
                                            "Pause" // TODO: CMP resources
                                        },
                                        modifier = Modifier.size(38.dp),
                                        tint = MaterialTheme.colorScheme.background
                                    )
                                }

                                FilledIconButton(
                                    onClick = { showExitDialog = true },
                                    modifier = Modifier.size(52.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "End workout", // TODO: CMP resources
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            FilledTonalButton(
                                onClick = { store.dispatch(TimerIntent.PreviousPhase) },
                                modifier = Modifier.padding(bottom = 48.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.NavigateBefore,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Previous", // TODO: CMP resources
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { gymControlsLocked = false }
                                )
                                .padding(vertical = 20.dp, horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Hold to unlock", // TODO: CMP resources
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
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
            // TODO: CMP resources
            text = "$workoutName completed!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(40.dp))
        TextButton(onClick = onBack) {
            Text("Back to Home") // TODO: CMP resources
        }
    }
}
