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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Celebration
import com.workout.shared.ui.util.WorkoutDialog
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.feature.timer.PhaseType
import com.workout.shared.feature.timer.TimerEffect
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerLoadSettings
import com.workout.shared.feature.timer.TimerViewModel
import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.back_to_home
import workoutapp.shared.generated.resources.notif_paused
import workoutapp.shared.generated.resources.notif_phase_prep
import workoutapp.shared.generated.resources.notif_phase_rest
import workoutapp.shared.generated.resources.notif_phase_work
import workoutapp.shared.generated.resources.notif_set_format
import workoutapp.shared.generated.resources.cd_end_workout
import workoutapp.shared.generated.resources.cd_exit_gym_mode
import workoutapp.shared.generated.resources.cd_gym_mode
import workoutapp.shared.generated.resources.cd_lock_controls
import workoutapp.shared.generated.resources.cd_pause
import workoutapp.shared.generated.resources.cd_resume
import workoutapp.shared.generated.resources.cd_skip
import workoutapp.shared.generated.resources.continue_workout
import workoutapp.shared.generated.resources.end_workout
import workoutapp.shared.generated.resources.phase_prep
import workoutapp.shared.generated.resources.phase_rest
import workoutapp.shared.generated.resources.phase_rest_name
import workoutapp.shared.generated.resources.phase_work
import workoutapp.shared.generated.resources.timer_exit_message
import workoutapp.shared.generated.resources.timer_exit_title
import workoutapp.shared.generated.resources.timer_hold_to_unlock
import workoutapp.shared.generated.resources.timer_minus_10
import workoutapp.shared.generated.resources.timer_next_phase_format
import workoutapp.shared.generated.resources.timer_plus_10
import workoutapp.shared.generated.resources.timer_previous_phase
import workoutapp.shared.generated.resources.timer_round_format
import workoutapp.shared.generated.resources.workout_finished_format
import com.workout.shared.platform.AudioFeedback
import com.workout.shared.platform.ForegroundTimerService
import com.workout.shared.platform.HapticFeedback
import com.workout.shared.platform.NotifDisplayStrings
import com.workout.shared.platform.ScreenWakeLock
import com.workout.shared.platform.TimerSettings
import com.workout.shared.platform.rememberAppReviewLauncher
import com.workout.shared.ui.components.toTimeString
import com.workout.shared.ui.util.BackHandler
import com.workout.shared.ui.theme.TimerPrepGray
import com.workout.shared.ui.theme.TimerPrepGrayDim
import com.workout.shared.ui.theme.TimerRestGreen
import com.workout.shared.ui.theme.TimerRestGreenDim
import com.workout.shared.ui.theme.TimerWorkOrange
import com.workout.shared.ui.theme.TimerWorkOrangeDim
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
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

    val vm = viewModel { TimerViewModel(repository) }
    val store = vm.store

    val notifDisplayStrings = NotifDisplayStrings(
        phasePrep = stringResource(Res.string.notif_phase_prep),
        phaseWork = stringResource(Res.string.notif_phase_work),
        phaseRest = stringResource(Res.string.notif_phase_rest),
        paused = stringResource(Res.string.notif_paused),
        setFormat = stringResource(Res.string.notif_set_format)
    )

    val quickAdjustEnabled = remember { mutableStateOf(timerSettings.timerQuickAdjustEnabled) }
    val reviewLauncher = rememberAppReviewLauncher()

    // Load workout with settings
    LaunchedEffect(workoutId) {
        store.dispatch(
            TimerIntent.Load(
                workoutId = workoutId,
                settings = TimerLoadSettings(
                    blockPrepDurationSeconds = timerSettings.blockPrepDurationSeconds,
                    soundEnabled = timerSettings.soundEnabled,
                    vibrationEnabled = timerSettings.vibrationEnabled,
                    workStartSoundPresetId = timerSettings.workStartSoundPresetId,
                    restStartSoundPresetId = timerSettings.restStartSoundPresetId,
                    finishSoundPresetId = timerSettings.workoutFinishSoundPresetId,
                    workPhaseWarningSoundPresetId = timerSettings.workPhaseWarningSoundPresetId,
                    workPhaseEndWarningSeconds = timerSettings.workPhaseEndWarningSeconds,
                ),
                restPhaseDisplayName = getString(Res.string.phase_rest_name)
            )
        )
    }

    // Start foreground service + keep screen on
    DisposableEffect(Unit) {
        foregroundService.start("") { intent -> store.dispatch(intent) }
        screenWakeLock.acquire()
        onDispose {
            foregroundService.stop()
            screenWakeLock.release()
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

                is TimerEffect.VibrateWork -> hapticFeedback.vibrateShort()
                is TimerEffect.VibrateRest -> hapticFeedback.vibrateShort()
                is TimerEffect.VibrateFinish -> hapticFeedback.vibrateFinish()
                is TimerEffect.WorkPhaseEndAlert -> {
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
            foregroundService.update(s, s.workoutName, notifDisplayStrings)
        }
    }

    val state by store.state.collectAsState()
    val quickAdjust = quickAdjustEnabled.value

    // Request in-app review after completing N workouts
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            val count = timerSettings.workoutsCompletedCount + 1
            timerSettings.workoutsCompletedCount = count
            if (count in setOf(3, 10, 30)) {
                reviewLauncher()
            }
        }
    }
    var showExitDialog by remember { mutableStateOf(false) }
    var gymMode by remember { mutableStateOf(false) }

    BackHandler(enabled = !state.isLoading && !state.isFinished) {
        showExitDialog = true
    }
    var gymControlsLocked by remember { mutableStateOf(false) }

    val timerFontSize = if (gymMode) 120.sp else 88.sp
    val titleStyle =
        if (gymMode) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium

    val phaseAccentTarget = when (state.currentPhase?.type) {
        PhaseType.Prep -> TimerPrepGray
        PhaseType.Work -> TimerWorkOrange
        else -> TimerRestGreen
    }
    val phaseDimTarget = when (state.currentPhase?.type) {
        PhaseType.Prep -> TimerPrepGrayDim
        PhaseType.Work -> TimerWorkOrangeDim
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
        WorkoutDialog(
            onDismissRequest = { showExitDialog = false },
            title = stringResource(Res.string.timer_exit_title),
            confirmText = stringResource(Res.string.continue_workout),
            onConfirm = {
                showExitDialog = false
            },
            dismissText = stringResource(Res.string.end_workout),
            onDismiss = {
                showExitDialog = false
                store.dispatch(TimerIntent.Finish)
            },
            content = { Text(stringResource(Res.string.timer_exit_message)) }
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
                                contentDescription = stringResource(Res.string.cd_exit_gym_mode)
                            )
                        }
                        if (!gymControlsLocked) {
                            IconButton(onClick = { gymControlsLocked = true }) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = stringResource(Res.string.cd_lock_controls)
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { gymMode = true }) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = stringResource(Res.string.cd_gym_mode)
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
                            text = when (state.currentPhase?.type) {
                                PhaseType.Prep -> stringResource(Res.string.phase_prep)
                                PhaseType.Work -> stringResource(Res.string.phase_work)
                                else -> stringResource(Res.string.phase_rest)
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
                            text = stringResource(Res.string.timer_round_format, label),
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
                        state.nextSignificantPhase?.let { next ->
                            Text(
                                text = stringResource(
                                    Res.string.timer_next_phase_format,
                                    next.name,
                                    next.durationSeconds.toTimeString()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
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
                                    Text(stringResource(Res.string.timer_minus_10))
                                }
                                Spacer(Modifier.width(12.dp))
                                FilledTonalButton(
                                    onClick = {
                                        store.dispatch(TimerIntent.AdjustRemainingSeconds(10))
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(stringResource(Res.string.timer_plus_10))
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
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = stringResource(Res.string.cd_skip)
                                    )
                                }

                                FilledIconButton(
                                    onClick = { store.dispatch(TimerIntent.TogglePause) },
                                    modifier = Modifier.size(76.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = accentColor
                                    )
                                ) {
                                    Icon(
                                        if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (state.isPaused) {
                                            stringResource(Res.string.cd_resume)
                                        } else {
                                            stringResource(Res.string.cd_pause)
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
                                        contentDescription = stringResource(Res.string.cd_end_workout),
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
                                    Icons.AutoMirrored.Filled.NavigateBefore,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(Res.string.timer_previous_phase),
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
                                text = stringResource(Res.string.timer_hold_to_unlock),
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
            text = stringResource(Res.string.workout_finished_format, workoutName),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(40.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(Res.string.back_to_home))
        }
    }
}
