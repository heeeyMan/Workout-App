import SwiftUI
import Shared

@main
struct WorkoutIosApp: App {
    init() {
        KoinIosHelperKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ComposeViewRepresentable()
                .ignoresSafeArea()
                .preferredColorScheme(.dark)
        }
    }
}

struct ComposeViewRepresentable: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
