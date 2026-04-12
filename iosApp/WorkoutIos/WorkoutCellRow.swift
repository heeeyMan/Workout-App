import Shared
import SwiftUI

/// Ячейка как `WorkoutCard` на Android: тап по названию/подзаголовку = старт, затем Play, Edit, Delete.
struct WorkoutCellRow: View {
    let workout: CoreWorkout
    let onStart: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Button(action: onStart) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(workout.name)
                        .font(.title3.weight(.medium))
                        .foregroundStyle(WorkoutPalette.onSurface)
                    Text(workoutSubtitle)
                        .font(.body)
                        .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Button(action: onStart) {
                Image(systemName: "play")
                    .font(.title3)
            }
            .buttonStyle(.borderless)
            .tint(WorkoutPalette.primary)
            .accessibilityLabel(L10n.tr("cd_start_workout"))

            NavigationLink {
                CreateWorkoutView(workoutId: workout.id)
            } label: {
                Image(systemName: "pencil")
                    .font(.title3)
                    .foregroundStyle(WorkoutPalette.onSurfaceMuted)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(L10n.tr("cd_edit_workout"))

            Button(action: onDelete) {
                Image(systemName: "trash")
                    .font(.title3)
                    .foregroundStyle(WorkoutPalette.dangerRed)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(L10n.tr("cd_delete"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
        .listRowBackground(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(WorkoutPalette.surfaceVariant)
        )
        .listRowSeparator(.hidden)
    }

    private var workoutSubtitle: String {
        let blocksPart = L10n.blocksCount(Int(workout.blocks.count))
        return L10n.tr("workout_card_line", formatDuration(workout.totalDurationSeconds), blocksPart)
    }

    private func formatDuration(_ total: Int32) -> String {
        let s = max(0, Int(total))
        return String(format: "%02d:%02d", s / 60, s % 60)
    }
}
