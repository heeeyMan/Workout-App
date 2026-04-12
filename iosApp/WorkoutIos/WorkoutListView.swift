import Shared
import SwiftUI

private func workoutListState(_ store: WorkoutListStore) -> WorkoutListState? {
    store.state.value as? WorkoutListState
}

/// Экран списка тренировок на [WorkoutListStore] (как `WorkoutListScreen` на Android).
struct WorkoutListView: View {
    @EnvironmentObject private var appModel: AppModel
    @Environment(\.dismiss) private var dismiss

    @State private var tick = 0
    @State private var store: WorkoutListStore?
    @State private var cancelEffects: (() -> Void)?
    @State private var timerWorkoutId: Int64?
    @State private var showCreateFromEffect = false

    private var pulse: Timer.TimerPublisher {
        Timer.publish(every: 0.25, on: .main, in: .common)
    }

    var body: some View {
        let _ = tick
        let listStore = store
        let state = listStore.flatMap { workoutListState($0) }

        return Group {
            if let listStore, let state {
                listContent(store: listStore, state: state)
            } else {
                ProgressView(L10n.tr("loading"))
                    .tint(WorkoutPalette.primary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(WorkoutPalette.background)
        .navigationTitle(L10n.tr("my_workouts"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .tint(WorkoutPalette.primary)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(L10n.tr("back")) { dismiss() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink {
                    CreateWorkoutView(workoutId: 0)
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel(L10n.tr("new_workout"))
            }
        }
        .onAppear {
            guard store == nil else { return }
            let s = appModel.controller.createWorkoutListStore()
            store = s
            cancelEffects = appModel.controller.observeWorkoutListEffects(store: s) { effect in
                if let nav = effect as? WorkoutListEffectNavigateToTimer {
                    timerWorkoutId = nav.workoutId
                } else if effect is WorkoutListEffectNavigateToCreateWorkout {
                    showCreateFromEffect = true
                }
            }
        }
        .onDisappear {
            cancelEffects?()
            store?.destroy()
            store = nil
        }
        .onReceive(pulse.autoconnect()) { _ in tick &+= 1 }
        .fullScreenCover(
            isPresented: Binding(
                get: { timerWorkoutId != nil },
                set: { if !$0 { timerWorkoutId = nil } }
            ),
            onDismiss: { timerWorkoutId = nil },
            content: {
                if let id = timerWorkoutId {
                    TimerView(workoutId: id)
                        .environmentObject(appModel)
                }
            }
        )
        .sheet(isPresented: $showCreateFromEffect) {
            NavigationStack {
                CreateWorkoutView(workoutId: 0)
                    .environmentObject(appModel)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button(L10n.tr("close")) { showCreateFromEffect = false }
                        }
                    }
            }
        }
        .alert(
            L10n.tr("confirm_delete_workout_title"),
            isPresented: Binding(
                get: { listStore.flatMap { workoutListState($0)?.pendingDeleteId != nil } ?? false },
                set: { newVal in
                    if !newVal, let s = store {
                        s.dispatch(intent: WorkoutListIntentCancelDelete.shared)
                    }
                }
            )
        ) {
            Button(L10n.tr("delete"), role: .destructive) {
                store?.dispatch(intent: WorkoutListIntentConfirmDelete.shared)
            }
            Button(L10n.tr("cancel"), role: .cancel) {
                store?.dispatch(intent: WorkoutListIntentCancelDelete.shared)
            }
        } message: {
            Text(L10n.tr("confirm_delete_workout_message"))
        }
    }

    @ViewBuilder
    private func listContent(store: WorkoutListStore, state: WorkoutListState) -> some View {
        if state.isLoading {
            ProgressView(L10n.tr("loading"))
                .tint(WorkoutPalette.primary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if state.workouts.isEmpty {
            Text(L10n.tr("no_workouts"))
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                .padding()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            List {
                ForEach(state.workouts as [CoreWorkout], id: \.id) { w in
                    WorkoutCellRow(
                        workout: w,
                        onStart: { store.dispatch(intent: WorkoutListIntentSelectWorkout(workoutId: w.id)) },
                        onDelete: { store.dispatch(intent: WorkoutListIntentRequestDelete(workoutId: w.id)) }
                    )
                }
            }
            .scrollContentBackground(.hidden)
            .listStyle(.plain)
            .background(WorkoutPalette.background)
        }
    }
}
