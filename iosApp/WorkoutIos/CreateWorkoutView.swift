import Shared
import SwiftUI

private func createState(_ store: CreateWorkoutStore) -> CreateWorkoutState? {
    store.state.value as? CreateWorkoutState
}

struct CreateWorkoutView: View {
    let workoutId: Int64

    @EnvironmentObject private var appModel: AppModel
    @Environment(\.dismiss) private var dismiss

    @State private var tick = 0
    @State private var store: CreateWorkoutStore?
    @State private var cancelEffects: (() -> Void)?
    @State private var emptyNameAlert = false

    private var uiPulse: Timer.TimerPublisher {
        Timer.publish(every: 0.2, on: .main, in: .common)
    }

    var body: some View {
        let _ = tick
        Group {
            if let store, let state = createState(store) {
                mainList(store: store, state: state)
            } else {
                ProgressView(L10n.tr("loading"))
                    .tint(WorkoutPalette.primary)
            }
        }
        .background(WorkoutPalette.background)
        .navigationTitle(workoutId == 0 ? L10n.tr("create_workout_title_new") : L10n.tr("create_workout_title_edit"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .tint(WorkoutPalette.primary)
        .onAppear {
            guard store == nil else { return }
            let s = appModel.controller.createCreateWorkoutStore()
            store = s
            cancelEffects = appModel.controller.observeCreateWorkoutEffects(store: s) { effect in
                if effect is CreateWorkoutEffectNavigateBack {
                    DispatchQueue.main.async { dismiss() }
                } else if effect is CreateWorkoutEffectShowErrorEmptyWorkoutName {
                    DispatchQueue.main.async { emptyNameAlert = true }
                }
            }
            if workoutId != 0 {
                s.dispatch(intent: CreateWorkoutIntentLoadWorkout(workoutId: workoutId))
            } else {
                let n = Int.random(in: 0...100)
                let defaultName = String(format: L10n.tr("default_workout_name_pattern"), n)
                s.dispatch(intent: CreateWorkoutIntentSetDefaultWorkoutNameIfEmpty(name: defaultName))
            }
        }
        .onDisappear {
            cancelEffects?()
            store?.destroy()
            store = nil
        }
        .onReceive(uiPulse.autoconnect()) { _ in tick &+= 1 }
        .alert(L10n.tr("error_workout_name_required"), isPresented: $emptyNameAlert) {
            Button(L10n.tr("ok"), role: .cancel) {}
        }
    }

    @ViewBuilder
    private func mainList(store: CreateWorkoutStore, state: CreateWorkoutState) -> some View {
        let blocks = state.blocks as [CoreBlock]
        List {
            Section {
                TextField(L10n.tr("workout_name_label"), text: Binding(
                    get: { state.name },
                    set: { store.dispatch(intent: CreateWorkoutIntentUpdateName(name: $0)) }
                ))
            }
            Section {
                Button {
                    let n = Int.random(in: 0...100)
                    let name = String(format: L10n.tr("default_exercise_name_pattern"), n)
                    store.dispatch(intent: CreateWorkoutIntentAddExerciseBlock(afterIndex: nil, defaultExerciseName: name))
                } label: {
                    Label(L10n.tr("add_exercise"), systemImage: "plus.circle")
                }
                Button {
                    store.dispatch(intent: CreateWorkoutIntentAddRestBlock(afterIndex: nil))
                } label: {
                    Label(L10n.tr("add_rest"), systemImage: "moon.zzz")
                }
            }
            Section(header: Text(L10n.tr("blocks_section")).foregroundStyle(WorkoutPalette.onSurfaceMuted)) {
                if blocks.isEmpty {
                    Text(L10n.tr("blocks_empty_hint"))
                        .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                } else {
                    ForEach(Array(0..<blocks.count), id: \.self) { index in
                        let block = blocks[index]
                        BlockCardView(
                            controller: appModel.controller,
                            store: store,
                            block: block,
                            index: index,
                            count: blocks.count
                        )
                        .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                    }
                    .onMove { source, destination in
                        guard let from = source.first else { return }
                        var to = destination
                        if from < to { to -= 1 }
                        store.dispatch(intent: CreateWorkoutIntentMoveBlock(fromIndex: Int32(from), toIndex: Int32(to)))
                    }
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(WorkoutPalette.background)
        .environment(\.editMode, .constant(.active))
        .disabled(state.isSaving)
        .overlay {
            if state.isSaving {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(.ultraThinMaterial)
            }
        }
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                if state.totalDurationSeconds > 0 {
                    Text(formatTotalTime(Int(state.totalDurationSeconds)))
                        .font(.subheadline)
                        .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button(L10n.tr("save")) {
                    store.dispatch(intent: CreateWorkoutIntentSave.shared)
                }
                .disabled(state.isSaving)
            }
        }
    }

    private func formatTotalTime(_ totalSeconds: Int) -> String {
        let m = totalSeconds / 60
        let s = totalSeconds % 60
        return String(format: "%02d:%02d", m, s)
    }
}

// MARK: - Block Card

private struct BlockCardView: View {
    let controller: WorkoutIosAppController
    let store: CreateWorkoutStore
    let block: CoreBlock
    let index: Int
    let count: Int

    @State private var showNameDialog = false
    @State private var showWorkPicker = false
    @State private var showRestPicker = false
    @State private var showDurationPicker = false

    private var isExercise: Bool { controller.blockKind(block: block) == "exercise" }
    private var accentColor: Color { isExercise ? WorkoutPalette.timerWorkOrange : WorkoutPalette.timerRestGreen }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header
            HStack(spacing: 8) {
                Circle()
                    .fill(accentColor)
                    .frame(width: 8, height: 8)
                Text(isExercise ? L10n.tr("block_type_exercise") : L10n.tr("block_type_rest"))
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(accentColor)
                Spacer()
                Button {
                    store.dispatch(intent: CreateWorkoutIntentDuplicateBlock(index: Int32(index)))
                } label: {
                    Image(systemName: "doc.on.doc")
                        .font(.caption)
                        .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                }
                .buttonStyle(.plain)
                Button(role: .destructive) {
                    store.dispatch(intent: CreateWorkoutIntentRemoveBlock(index: Int32(index)))
                } label: {
                    Image(systemName: "trash")
                        .font(.callout)
                        .foregroundStyle(WorkoutPalette.dangerRed)
                }
                .buttonStyle(.plain)
            }
            .padding(.bottom, 12)

            Divider()
                .overlay(Color.white.opacity(0.15))
                .padding(.bottom, 16)

            // Content
            if isExercise, let fields = controller.exerciseBlockFields(block: block) {
                exerciseContent(fields: fields)
            } else if let fields = controller.restBlockFields(block: block) {
                restContent(fields: fields)
            }
        }
        .padding(16)
        .background(WorkoutPalette.surfaceVariant)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: Exercise content

    @ViewBuilder
    private func exerciseContent(fields: IosExerciseBlockFields) -> some View {
        // Name row
        Button {
            showNameDialog = true
        } label: {
            HStack {
                Text(fields.name)
                    .font(.title3)
                    .foregroundStyle(WorkoutPalette.onSurface)
                Spacer()
                Image(systemName: "pencil")
                    .font(.caption)
                    .foregroundStyle(WorkoutPalette.onSurfaceMuted)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(WorkoutPalette.surface)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .alert(L10n.tr("dialog_exercise_name_title"), isPresented: $showNameDialog) {
            NameEditAlert(
                currentName: fields.name,
                onConfirm: { newName in
                    controller.dispatchUpdateExerciseBlock(
                        store: store,
                        index: Int32(index),
                        id: fields.id,
                        orderIndex: fields.orderIndex,
                        name: newName,
                        workDurationSeconds: fields.workDurationSeconds,
                        restDurationSeconds: fields.restDurationSeconds,
                        repeats: fields.repeats
                    )
                }
            )
        }

        Spacer().frame(height: 16)

        // Work & Rest chips
        HStack(spacing: 12) {
            DurationChipView(
                label: L10n.tr("work_label"),
                seconds: Int(fields.workDurationSeconds),
                color: WorkoutPalette.timerWorkOrange
            ) {
                showWorkPicker = true
            }

            DurationChipView(
                label: L10n.tr("rest_label"),
                seconds: Int(fields.restDurationSeconds),
                color: WorkoutPalette.timerRestGreen
            ) {
                showRestPicker = true
            }
        }
        .sheet(isPresented: $showWorkPicker) {
            DurationPickerSheet(
                title: L10n.tr("work_label"),
                seconds: Int(fields.workDurationSeconds)
            ) { newSeconds in
                controller.dispatchUpdateExerciseBlock(
                    store: store,
                    index: Int32(index),
                    id: fields.id,
                    orderIndex: fields.orderIndex,
                    name: fields.name,
                    workDurationSeconds: Int32(newSeconds),
                    restDurationSeconds: fields.restDurationSeconds,
                    repeats: fields.repeats
                )
            }
        }
        .sheet(isPresented: $showRestPicker) {
            DurationPickerSheet(
                title: L10n.tr("rest_label"),
                seconds: Int(fields.restDurationSeconds)
            ) { newSeconds in
                controller.dispatchUpdateExerciseBlock(
                    store: store,
                    index: Int32(index),
                    id: fields.id,
                    orderIndex: fields.orderIndex,
                    name: fields.name,
                    workDurationSeconds: fields.workDurationSeconds,
                    restDurationSeconds: Int32(newSeconds),
                    repeats: fields.repeats
                )
            }
        }

        Spacer().frame(height: 16)

        Divider()
            .overlay(Color.white.opacity(0.15))

        Spacer().frame(height: 12)

        // Repeats row
        HStack {
            Text(L10n.tr("repeats_label"))
                .font(.body)
                .foregroundStyle(WorkoutPalette.onSurfaceMuted)
            Spacer()
            HStack(spacing: 0) {
                RepeatButtonView(label: "\u{2212}", enabled: fields.repeats > 1) {
                    controller.dispatchUpdateExerciseBlock(
                        store: store,
                        index: Int32(index),
                        id: fields.id,
                        orderIndex: fields.orderIndex,
                        name: fields.name,
                        workDurationSeconds: fields.workDurationSeconds,
                        restDurationSeconds: fields.restDurationSeconds,
                        repeats: fields.repeats - 1
                    )
                }
                Text("\(fields.repeats)")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundStyle(WorkoutPalette.onSurface)
                    .frame(width: 48)
                    .multilineTextAlignment(.center)
                RepeatButtonView(label: "+", enabled: true) {
                    controller.dispatchUpdateExerciseBlock(
                        store: store,
                        index: Int32(index),
                        id: fields.id,
                        orderIndex: fields.orderIndex,
                        name: fields.name,
                        workDurationSeconds: fields.workDurationSeconds,
                        restDurationSeconds: fields.restDurationSeconds,
                        repeats: fields.repeats + 1
                    )
                }
            }
        }
    }

    // MARK: Rest content

    @ViewBuilder
    private func restContent(fields: IosRestBlockFields) -> some View {
        DurationChipView(
            label: L10n.tr("duration_label"),
            seconds: Int(fields.durationSeconds),
            color: WorkoutPalette.timerRestGreen
        ) {
            showDurationPicker = true
        }
        .sheet(isPresented: $showDurationPicker) {
            DurationPickerSheet(
                title: L10n.tr("duration_label"),
                seconds: Int(fields.durationSeconds)
            ) { newSeconds in
                controller.dispatchUpdateRestBlock(
                    store: store,
                    index: Int32(index),
                    id: fields.id,
                    orderIndex: fields.orderIndex,
                    durationSeconds: Int32(newSeconds)
                )
            }
        }
    }
}

// MARK: - Duration Chip

private struct DurationChipView: View {
    let label: String
    let seconds: Int
    let color: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                Text(label)
                    .font(.caption2)
                    .fontWeight(.semibold)
                    .foregroundStyle(color)
                Text(formatTime(seconds))
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundStyle(WorkoutPalette.onSurface)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .padding(.horizontal, 16)
            .background(color.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    private func formatTime(_ sec: Int) -> String {
        String(format: "%02d:%02d", sec / 60, sec % 60)
    }
}

// MARK: - Repeat Button

private struct RepeatButtonView: View {
    let label: String
    let enabled: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundStyle(enabled ? WorkoutPalette.onSurface : WorkoutPalette.onSurfaceMuted)
                .frame(width: 40, height: 40)
                .background(enabled ? WorkoutPalette.surface : WorkoutPalette.surfaceVariant)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

// MARK: - Name Edit Alert

private struct NameEditAlert: View {
    let currentName: String
    let onConfirm: (String) -> Void

    @State private var text = ""

    var body: some View {
        TextField(L10n.tr("dialog_exercise_name_title"), text: $text)
            .autocorrectionDisabled(false)
            .textInputAutocapitalization(.sentences)
            .onAppear { text = currentName }
        Button(L10n.tr("cancel"), role: .cancel) {}
        Button(L10n.tr("done")) {
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty {
                onConfirm(trimmed)
            }
        }
    }
}

// MARK: - Duration Picker Sheet

private struct DurationPickerSheet: View {
    let title: String
    let seconds: Int
    let onConfirm: (Int) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var minutes: Int = 0
    @State private var secs: Int = 0

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Spacer()
                HStack(spacing: 0) {
                    Picker("", selection: $minutes) {
                        ForEach(0..<100, id: \.self) { m in
                            Text(String(format: "%02d", m))
                                .tag(m)
                                .foregroundStyle(WorkoutPalette.onSurface)
                        }
                    }
                    .pickerStyle(.wheel)
                    .frame(width: 100)
                    .clipped()

                    Text(":")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundStyle(WorkoutPalette.onSurface)

                    Picker("", selection: $secs) {
                        ForEach(0..<60, id: \.self) { s in
                            Text(String(format: "%02d", s))
                                .tag(s)
                                .foregroundStyle(WorkoutPalette.onSurface)
                        }
                    }
                    .pickerStyle(.wheel)
                    .frame(width: 100)
                    .clipped()
                }
                Spacer()
            }
            .frame(maxWidth: .infinity)
            .background(WorkoutPalette.background)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("done")) {
                        onConfirm(minutes * 60 + secs)
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            minutes = seconds / 60
            secs = seconds % 60
        }
        .presentationDetents([.medium])
    }
}
