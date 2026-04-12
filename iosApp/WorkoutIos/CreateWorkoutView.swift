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
    @State private var exerciseEditor: ExerciseEditorPayload?
    @State private var restEditor: RestEditorPayload?

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
        .sheet(item: $exerciseEditor) { payload in
            Group {
                if let store {
                    ExerciseBlockEditorView(
                        controller: appModel.controller,
                        store: store,
                        payload: payload,
                        onDone: { exerciseEditor = nil }
                    )
                }
            }
        }
        .sheet(item: $restEditor) { payload in
            Group {
                if let store {
                    RestBlockEditorView(
                        controller: appModel.controller,
                        store: store,
                        payload: payload,
                        onDone: { restEditor = nil }
                    )
                }
            }
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
                        blockRow(
                            store: store,
                            block: block,
                            index: index,
                            count: blocks.count
                        )
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
                Button(L10n.tr("cancel")) {
                    store.dispatch(intent: CreateWorkoutIntentDiscard.shared)
                }
                .disabled(state.isSaving)
            }
            ToolbarItem(placement: .confirmationAction) {
                Button(L10n.tr("save")) {
                    store.dispatch(intent: CreateWorkoutIntentSave.shared)
                }
                .disabled(state.isSaving)
            }
        }
    }

    @ViewBuilder
    private func blockRow(store: CreateWorkoutStore, block: CoreBlock, index: Int, count: Int) -> some View {
        let kind = appModel.controller.blockKind(block: block)
        HStack(alignment: .center, spacing: 10) {
            VStack(alignment: .leading, spacing: 4) {
                Text(appModel.controller.blockSummaryLine(block: block))
                    .font(.subheadline)
            }
            Spacer(minLength: 0)
            Menu {
                Button(L10n.tr("menu_edit")) {
                    if kind == "exercise", let f = appModel.controller.exerciseBlockFields(block: block) {
                        exerciseEditor = ExerciseEditorPayload(index: index, fields: f)
                    } else if kind == "rest", let f = appModel.controller.restBlockFields(block: block) {
                        restEditor = RestEditorPayload(index: index, fields: f)
                    }
                }
                Button(L10n.tr("menu_duplicate")) {
                    store.dispatch(intent: CreateWorkoutIntentDuplicateBlock(index: Int32(index)))
                }
                if index > 0 {
                    Button(L10n.tr("menu_move_up")) {
                        store.dispatch(intent: CreateWorkoutIntentMoveBlock(fromIndex: Int32(index), toIndex: Int32(index - 1)))
                    }
                }
                if index < count - 1 {
                    Button(L10n.tr("menu_move_down")) {
                        store.dispatch(intent: CreateWorkoutIntentMoveBlock(fromIndex: Int32(index), toIndex: Int32(index + 1)))
                    }
                }
                Divider()
                Button(L10n.tr("menu_delete"), role: .destructive) {
                    store.dispatch(intent: CreateWorkoutIntentRemoveBlock(index: Int32(index)))
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .imageScale(.large)
            }
        }
    }
}

private struct ExerciseEditorPayload: Identifiable {
    var id: String { "ex-\(index)-\(fields.id)" }
    let index: Int
    let fields: IosExerciseBlockFields
}

private struct ExerciseBlockEditorView: View {
    let controller: WorkoutIosAppController
    let store: CreateWorkoutStore
    let payload: ExerciseEditorPayload
    let onDone: () -> Void

    @State private var name: String = ""
    @State private var workSec: Int = 40
    @State private var restSec: Int = 20
    @State private var repeats: Int = 3

    var body: some View {
        NavigationStack {
            Form {
                Section(L10n.tr("edit_exercise_title")) {
                    TextField(L10n.tr("dialog_exercise_name_title"), text: $name)
                    Stepper("\(L10n.tr("work_label")): \(workSec) \(L10n.tr("seconds_short"))", value: $workSec, in: 1...3600, step: 1)
                    Stepper("\(L10n.tr("rest_label")): \(restSec) \(L10n.tr("seconds_short"))", value: $restSec, in: 0...3600, step: 1)
                    Stepper("\(L10n.tr("repeats_label")): \(repeats)", value: $repeats, in: 1...99, step: 1)
                }
            }
            .navigationTitle(L10n.tr("edit_exercise_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("cancel"), action: onDone)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("save")) {
                        controller.dispatchUpdateExerciseBlock(
                            store: store,
                            index: Int32(payload.index),
                            id: payload.fields.id,
                            orderIndex: payload.fields.orderIndex,
                            name: name,
                            workDurationSeconds: Int32(workSec),
                            restDurationSeconds: Int32(restSec),
                            repeats: Int32(repeats)
                        )
                        onDone()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .onAppear {
                name = payload.fields.name
                workSec = Int(payload.fields.workDurationSeconds)
                restSec = Int(payload.fields.restDurationSeconds)
                repeats = Int(payload.fields.repeats)
            }
        }
    }
}

private struct RestEditorPayload: Identifiable {
    var id: String { "rest-\(index)-\(fields.id)" }
    let index: Int
    let fields: IosRestBlockFields
}

private struct RestBlockEditorView: View {
    let controller: WorkoutIosAppController
    let store: CreateWorkoutStore
    let payload: RestEditorPayload
    let onDone: () -> Void

    @State private var duration: Int = 60

    var body: some View {
        NavigationStack {
            Form {
                Section(L10n.tr("edit_rest_title")) {
                    Stepper("\(L10n.tr("duration_label")): \(duration) \(L10n.tr("seconds_short"))", value: $duration, in: 1...7200, step: 1)
                }
            }
            .navigationTitle(L10n.tr("edit_rest_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.tr("cancel"), action: onDone)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.tr("save")) {
                        controller.dispatchUpdateRestBlock(
                            store: store,
                            index: Int32(payload.index),
                            id: payload.fields.id,
                            orderIndex: payload.fields.orderIndex,
                            durationSeconds: Int32(duration)
                        )
                        onDone()
                    }
                }
            }
            .onAppear {
                duration = Int(payload.fields.durationSeconds)
            }
        }
    }
}
