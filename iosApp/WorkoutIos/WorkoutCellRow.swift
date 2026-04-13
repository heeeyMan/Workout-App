import Shared
import SwiftUI

struct WorkoutCellRow: View {
    let workout: CoreWorkout
    let onStart: () -> Void
    let onDelete: () -> Void

    @EnvironmentObject private var appModel: AppModel
    @State private var navigateToEdit = false

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
                    let preview = structurePreview
                    if !preview.isEmpty {
                        Text(preview)
                            .font(.footnote)
                            .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            HStack(spacing: 8) {
                Button(action: onStart) {
                    Image(systemName: "play")
                        .font(.title3)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(.borderless)
                .tint(WorkoutPalette.primary)
                .accessibilityLabel(L10n.tr("cd_start_workout"))

                Button { navigateToEdit = true } label: {
                    Image(systemName: "pencil")
                        .font(.title3)
                        .foregroundStyle(WorkoutPalette.onSurfaceMuted)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(L10n.tr("cd_edit_workout"))
                .navigationDestination(isPresented: $navigateToEdit) {
                    CreateWorkoutView(workoutId: workout.id)
                }

                Button(action: onDelete) {
                    Image(systemName: "trash")
                        .font(.title3)
                        .foregroundStyle(WorkoutPalette.dangerRed)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(L10n.tr("cd_delete"))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
        .listRowBackground(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(WorkoutPalette.surface)
        )
        .listRowSeparator(.hidden)
    }

    private var workoutSubtitle: String {
        let blocksPart = L10n.blocksCount(Int(workout.blocks.count))
        return L10n.tr("workout_card_line", formatDuration(workout.totalDurationSeconds), blocksPart)
    }

    private var structurePreview: String {
        let blocks = workout.blocks
        guard !blocks.isEmpty else { return "" }
        let maxVisible = 3
        let items: [String] = blocks.prefix(maxVisible).compactMap { block in
            if let ex = appModel.controller.exerciseBlockFields(block: block) {
                let work = compactDuration(ex.workDurationSeconds)
                if ex.restDurationSeconds > 0 {
                    let rest = compactDuration(ex.restDurationSeconds)
                    return "\(ex.repeats)× \(work)/\(rest)"
                }
                return "\(ex.repeats)× \(work)"
            } else if let rest = appModel.controller.restBlockFields(block: block) {
                return "Rest \(compactDuration(rest.durationSeconds))"
            }
            return nil
        }
        let preview = items.joined(separator: " · ")
        let extra = blocks.count - maxVisible
        return extra > 0 ? "\(preview) +\(extra)" : preview
    }

    private func formatDuration(_ total: Int32) -> String {
        let s = max(0, Int(total))
        return String(format: "%02d:%02d", s / 60, s % 60)
    }

    private func compactDuration(_ sec: Int32) -> String {
        let s = max(0, Int(sec))
        if s < 60 { return "\(s)s" }
        let m = s / 60
        let r = s % 60
        return r == 0 ? "\(m)m" : "\(m):\(String(format: "%02d", r))"
    }
}
