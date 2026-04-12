import Shared
import SwiftUI

private func homeState(_ store: HomeStore) -> HomeState? {
    store.state.value as? HomeState
}

struct HomeView: View {
    @EnvironmentObject private var appModel: AppModel
    @State private var tick = 0
    @State private var timerWorkoutId: Int64?
    @State private var cancelHomeEffects: (() -> Void)?

    private var timer: Timer.TimerPublisher {
        Timer.publish(every: 0.25, on: .main, in: .common)
    }

    var body: some View {
        let _ = tick
        let homeStore = appModel.controller.homeStore
        let state = homeState(homeStore)

        return NavigationStack {
            Group {
                if state == nil {
                    ProgressView(L10n.tr("loading"))
                        .tint(WorkoutPalette.primary)
                } else if state!.isLoading {
                    ProgressView(L10n.tr("loading"))
                        .tint(WorkoutPalette.primary)
                } else if state!.workouts.isEmpty {
                    VStack(spacing: 16) {
                        Text(L10n.tr("no_workouts"))
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(WorkoutPalette.onSurface)
                        Text(L10n.tr("empty_home_subtitle"))
                            .font(.subheadline)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .padding()
                } else {
                    workoutList(homeStore: homeStore, workouts: state!.workouts as [CoreWorkout])
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(WorkoutPalette.background)
            .navigationTitle(L10n.tr("my_workouts"))
            .toolbarColorScheme(.dark, for: .navigationBar)
            .tint(WorkoutPalette.primary)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    HStack(spacing: 16) {
                        NavigationLink {
                            WorkoutListView()
                                .environmentObject(appModel)
                        } label: {
                            Image(systemName: "list.bullet.rectangle")
                        }
                        .accessibilityLabel(L10n.tr("workout_list_nav"))
                        NavigationLink {
                            SettingsView()
                        } label: {
                            Image(systemName: "gearshape")
                        }
                        .accessibilityLabel(L10n.tr("settings"))
                    }
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
            .onReceive(timer.autoconnect()) { _ in tick &+= 1 }
            .onAppear {
                cancelHomeEffects = appModel.controller.observeHomeEffects { effect in
                    if let nav = effect as? HomeEffectNavigateToTimer {
                        timerWorkoutId = nav.workoutId
                    }
                }
            }
            .onDisappear {
                cancelHomeEffects?()
            }
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
            .alert(
                L10n.tr("confirm_delete_workout_title"),
                isPresented: Binding(
                    get: { homeState(homeStore)?.pendingDeleteId != nil },
                    set: { newVal in
                        if !newVal {
                            homeStore.dispatch(intent: HomeIntentCancelDelete.shared)
                        }
                    }
                )
            ) {
                Button(L10n.tr("delete"), role: .destructive) {
                    homeStore.dispatch(intent: HomeIntentConfirmDelete.shared)
                }
                Button(L10n.tr("cancel"), role: .cancel) {
                    homeStore.dispatch(intent: HomeIntentCancelDelete.shared)
                }
            } message: {
                Text(L10n.tr("confirm_delete_workout_message"))
            }
        }
    }

    @ViewBuilder
    private func workoutList(homeStore: HomeStore, workouts: [CoreWorkout]) -> some View {
        let last = lastStartedWorkout(from: workouts)
        List {
            if let last {
                Section {
                    WorkoutCellRow(
                        workout: last,
                        onStart: { homeStore.dispatch(intent: HomeIntentStartWorkout(workoutId: last.id)) },
                        onDelete: { homeStore.dispatch(intent: HomeIntentRequestDelete(workoutId: last.id)) }
                    )
                } header: {
                    Text(L10n.tr("last_workout"))
                        .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                        .font(.subheadline.weight(.semibold))
                }
            }
            Section {
                ForEach(workouts, id: \.id) { w in
                    WorkoutCellRow(
                        workout: w,
                        onStart: { homeStore.dispatch(intent: HomeIntentStartWorkout(workoutId: w.id)) },
                        onDelete: { homeStore.dispatch(intent: HomeIntentRequestDelete(workoutId: w.id)) }
                    )
                }
            } header: {
                Text(L10n.tr("all_workouts"))
                    .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                    .font(.subheadline.weight(.semibold))
            }
        }
        .scrollContentBackground(.hidden)
        .listStyle(.plain)
        .background(WorkoutPalette.background)
    }

    private func lastStartedWorkout(from workouts: [CoreWorkout]) -> CoreWorkout? {
        workouts
            .filter { $0.lastStartedAt != nil }
            .max(by: { lastStartedMillis($0) < lastStartedMillis($1) })
    }

    private func lastStartedMillis(_ w: CoreWorkout) -> Int64 {
        guard let ls = w.lastStartedAt else { return 0 }
        return (ls as NSNumber).int64Value
    }
}
