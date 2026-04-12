import Shared
import SwiftUI
import UIKit

private func timerState(_ store: TimerStore) -> TimerState? {
    store.state.value as? TimerState
}

/// Экран таймера на [TimerStore] (общая логика с Android).
struct TimerView: View {
    let workoutId: Int64

    @EnvironmentObject private var appModel: AppModel
    @Environment(\.dismiss) private var dismiss

    @State private var tick = 0
    @State private var store: TimerStore?
    @State private var cancelEffects: (() -> Void)?
    @State private var showExitConfirm = false
    @State private var gymMode = false
    @State private var gymControlsLocked = false

    private let uiPulse = Timer.publish(every: 0.25, on: .main, in: .common)

    var body: some View {
        let _ = tick
        let immersiveGym: Bool = {
            guard let store, let s = timerState(store) else { return false }
            return gymMode && !s.isLoading && !s.isFinished
        }()
        NavigationStack {
            ZStack {
                if let store, let state = timerState(store) {
                    if state.isLoading {
                        ProgressView(L10n.tr("loading"))
                            .tint(WorkoutPalette.primary)
                    } else if state.isFinished {
                        finishedView(workoutName: state.workoutName)
                    } else {
                        activeTimerView(store: store, state: state)
                    }
                } else {
                    ProgressView(L10n.tr("loading"))
                        .tint(WorkoutPalette.primary)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(WorkoutPalette.surface)
            .navigationTitle(L10n.tr("timer_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .tint(WorkoutPalette.primary)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    if let store, let state = timerState(store), !state.isLoading, !state.isFinished,
                       !(gymMode && gymControlsLocked)
                    {
                        Button(L10n.tr("close")) { showExitConfirm = true }
                    }
                }
            }
            .alert(L10n.tr("timer_exit_title"), isPresented: $showExitConfirm) {
                Button(L10n.tr("end_workout"), role: .destructive) {
                    if let s = store {
                        s.dispatch(intent: TimerIntentFinish.shared)
                    }
                }
                Button(L10n.tr("continue_workout"), role: .cancel) {}
            } message: {
                Text(L10n.tr("timer_exit_message"))
            }
            .workoutTimerImmersiveGymChrome(hidden: immersiveGym)
        }
        .persistentSystemOverlays(immersiveGym ? .hidden : .automatic)
        .onAppear {
            guard store == nil else { return }
            UIApplication.shared.isIdleTimerDisabled = true
            let s = appModel.controller.createTimerStore()
            store = s
            let prefs = TimerUserSettings.shared
            cancelEffects = appModel.controller.observeTimerEffects(store: s) { effect in
                if effect is TimerEffectNavigateBack {
                    DispatchQueue.main.async {
                        dismiss()
                    }
                } else {
                    TimerFeedback.handle(effect, store: s)
                }
            }
            s.dispatch(
                intent: TimerIntentLoad(
                    workoutId: workoutId,
                    blockPrepDurationSeconds: Int32(prefs.blockPrepDurationSeconds),
                    soundEnabled: prefs.soundEnabled,
                    vibrationEnabled: prefs.vibrationEnabled,
                    workStartSoundPresetId: prefs.workStartSoundPresetId,
                    restStartSoundPresetId: prefs.restStartSoundPresetId,
                    finishSoundPresetId: prefs.workoutFinishSoundPresetId,
                    workPhaseEndWarningSeconds: Int32(prefs.workPhaseEndWarningSeconds),
                    restPhaseDisplayName: L10n.tr("phase_rest_name")
                )
            )
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
            cancelEffects?()
            store?.destroy()
            store = nil
        }
        .onReceive(uiPulse.autoconnect()) { _ in tick &+= 1 }
        .onChange(of: gymMode) { isGym in
            if !isGym { gymControlsLocked = false }
        }
    }

    @ViewBuilder
    private func finishedView(workoutName: String) -> some View {
        VStack(spacing: 24) {
            Image(systemName: "party.popper.fill")
                .font(.system(size: 72))
                .foregroundStyle(WorkoutPalette.timerWorkOrange)
            Text(L10n.tr("workout_finished_title", workoutName))
                .font(.title.weight(.bold))
                .multilineTextAlignment(.center)
            Button(L10n.tr("back_to_home")) {
                dismiss()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }

    @ViewBuilder
    private func activeTimerView(store: TimerStore, state: TimerState) -> some View {
        let prep = state.isPrepBeforeWork
        let isWork = state.currentPhase?.type == PhaseType.work
        let accent: Color = {
            if prep { return WorkoutPalette.timerPrepGray }
            if isWork { return WorkoutPalette.timerWorkOrange }
            return WorkoutPalette.timerRestGreen
        }()
        let dim = accent.opacity(0.22)
        let prefs = TimerUserSettings.shared
        let hideMeta = gymMode && gymControlsLocked

        VStack(spacing: 0) {
            ProgressView(value: Double(state.overallProgress))
                .tint(accent)
                .padding(.horizontal)

            gymModeToolbar

            Spacer(minLength: 8)

            Text(phaseTitle(state: state))
                .font(.title2.weight(.bold))
                .foregroundStyle(accent)

            Text(state.currentPhase?.name ?? "")
                .font(gymMode ? .largeTitle.weight(.semibold) : .title.weight(.semibold))
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            if let label = state.currentPhase?.repeatLabel {
                Text(L10n.tr("timer_repeat_format", label))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Text(formatTime(state.secondsRemaining))
                .font(.system(size: gymMode ? 96 : 72, weight: .bold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(accent)
                .padding(.vertical, 16)

            ProgressView(value: Double(state.phaseProgress))
                .tint(accent)
                .padding(.horizontal)

            if !hideMeta {
                if let next = state.nextPhase {
                    Text(L10n.tr("timer_next_phase", next.name, formatTime(next.durationSeconds)))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.top, 12)
                }

                Text(
                    L10n.tr(
                        "timer_block_of",
                        Int(state.currentPhaseIndex) + 1,
                        Int(state.totalPhases)
                    )
                )
                .font(.footnote)
                .foregroundStyle(.tertiary)
            }

            Spacer(minLength: 8)

            if gymMode && gymControlsLocked {
                unlockStrip
            } else {
                if prefs.timerQuickAdjustEnabled {
                    HStack(spacing: 16) {
                        Button(L10n.tr("timer_minus_10")) {
                            store.dispatch(intent: TimerIntentAdjustRemainingSeconds(delta: -10))
                        }
                        .buttonStyle(.bordered)
                        Button(L10n.tr("timer_plus_10")) {
                            store.dispatch(intent: TimerIntentAdjustRemainingSeconds(delta: 10))
                        }
                        .buttonStyle(.bordered)
                    }
                    .padding(.bottom, 8)
                }

                HStack(spacing: 20) {
                    Button {
                        store.dispatch(intent: TimerIntentSkipPhase.shared)
                    } label: {
                        Image(systemName: "forward.end.fill")
                            .font(.title2)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.secondary)
                    .accessibilityLabel(L10n.tr("cd_skip"))

                    Button {
                        store.dispatch(intent: TimerIntentTogglePause.shared)
                    } label: {
                        Image(systemName: state.isPaused ? "play.fill" : "pause.fill")
                            .font(.system(size: 32))
                            .frame(width: 76, height: 76)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(accent)
                    .accessibilityLabel(state.isPaused ? L10n.tr("cd_resume") : L10n.tr("cd_pause"))

                    Button {
                        showExitConfirm = true
                    } label: {
                        Image(systemName: "stop.fill")
                            .font(.title2)
                            .foregroundStyle(.red)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.secondary)
                    .accessibilityLabel(L10n.tr("cd_end_workout"))
                }
                .padding(.bottom, 12)

                Button {
                    store.dispatch(intent: TimerIntentPreviousPhase.shared)
                } label: {
                    Label(L10n.tr("timer_previous_phase"), systemImage: "backward.end.fill")
                }
                .padding(.bottom, 32)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(dim.ignoresSafeArea())
    }

    @ViewBuilder
    private var gymModeToolbar: some View {
        HStack {
            if gymMode {
                Button {
                    gymMode = false
                    gymControlsLocked = false
                } label: {
                    Image(systemName: "arrow.down.right.and.arrow.up.left")
                }
                .accessibilityLabel(L10n.tr("cd_exit_gym_mode"))
                Spacer()
                if !gymControlsLocked {
                    Button {
                        gymControlsLocked = true
                    } label: {
                        Image(systemName: "lock.fill")
                    }
                    .accessibilityLabel(L10n.tr("cd_lock_controls"))
                }
            } else {
                Spacer()
                Button {
                    gymMode = true
                } label: {
                    Image(systemName: "arrow.up.left.and.arrow.down.right")
                }
                .accessibilityLabel(L10n.tr("cd_gym_mode"))
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
    }

    private var unlockStrip: some View {
        Text(L10n.tr("timer_hold_to_unlock"))
            .font(.title3.weight(.medium))
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
            .padding(.horizontal, 16)
            .background(RoundedRectangle(cornerRadius: 16).fill(Color(.secondarySystemGroupedBackground)))
            .padding(.horizontal)
            .padding(.bottom, 48)
            .onLongPressGesture(minimumDuration: 0.55) {
                gymControlsLocked = false
            }
    }

    private func phaseTitle(state: TimerState) -> String {
        if state.isPrepBeforeWork { return L10n.tr("phase_prep") }
        if state.currentPhase?.type == PhaseType.work { return L10n.tr("phase_work") }
        return L10n.tr("phase_rest")
    }

    private func formatTime(_ sec: Int32) -> String {
        let s = max(0, Int(sec))
        return String(format: "%02d:%02d", s / 60, s % 60)
    }
}

private extension View {
    /// В gym-режиме убираем панель навигации (выход — кнопка «выйти из полноэкранного» в контенте).
    @ViewBuilder
    func workoutTimerImmersiveGymChrome(hidden: Bool) -> some View {
        if #available(iOS 17.0, *) {
            toolbar(hidden ? .hidden : .automatic, for: .navigationBar)
        } else {
            navigationBarHidden(hidden)
        }
    }
}
