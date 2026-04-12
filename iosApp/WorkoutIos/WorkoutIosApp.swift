import Shared
import SwiftUI

@main
struct WorkoutIosApp: App {
    @StateObject private var appModel = AppModel()

    init() {
        WorkoutTheme.applyUIKitChrome()
    }

    var body: some Scene {
        WindowGroup {
            HomeView()
                .environmentObject(appModel)
                .preferredColorScheme(.dark)
                .tint(WorkoutPalette.primary)
        }
    }
}

/// Держит [WorkoutIosAppController] на время работы приложения.
final class AppModel: ObservableObject {
    let controller = WorkoutIosAppController()
}
