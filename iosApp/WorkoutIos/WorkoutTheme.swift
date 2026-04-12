import SwiftUI
import UIKit

/// Палитра как в `androidApp/.../theme/Color.kt` + тёмная схема Material3 из `Theme.kt`.
enum WorkoutPalette {
    static let background = Color(red: 0, green: 0, blue: 0)
    static let surface = Color(red: 13 / 255, green: 13 / 255, blue: 13 / 255)
    static let surfaceVariant = Color(red: 26 / 255, green: 26 / 255, blue: 26 / 255)
    static let onSurface = Color.white
    static let onSurfaceMuted = Color(red: 158 / 255, green: 158 / 255, blue: 158 / 255)
    /// primary в теме Android — белый (иконки, акценты).
    static let primary = Color.white
    static let dangerRed = Color(red: 229 / 255, green: 57 / 255, blue: 53 / 255)

    static let timerWorkOrange = Color(red: 255 / 255, green: 107 / 255, blue: 53 / 255)
    static let timerRestGreen = Color(red: 76 / 255, green: 175 / 255, blue: 80 / 255)
    static let timerPrepGray = Color(red: 158 / 255, green: 158 / 255, blue: 158 / 255)
}

enum WorkoutTheme {
    static func applyUIKitChrome() {
        let surface = uiColor(13, 13, 13)
        let bg = uiColor(0, 0, 0)
        let label = UIColor.white
        let nav = UINavigationBarAppearance()
        nav.configureWithOpaqueBackground()
        nav.backgroundColor = surface
        nav.titleTextAttributes = [.foregroundColor: label]
        nav.largeTitleTextAttributes = [.foregroundColor: label]

        let navBar = UINavigationBar.appearance()
        navBar.standardAppearance = nav
        navBar.scrollEdgeAppearance = nav
        navBar.compactAppearance = nav
        navBar.compactScrollEdgeAppearance = nav
        navBar.tintColor = .white

        UITableView.appearance().backgroundColor = bg
        UITableView.appearance().separatorColor = UIColor(white: 1, alpha: 0.08)

        let cellBg = UIColor(red: 26 / 255, green: 26 / 255, blue: 26 / 255, alpha: 1)
        UITableViewCell.appearance().backgroundColor = cellBg

        UIScrollView.appearance().indicatorStyle = .white
    }

    private static func uiColor(_ r: CGFloat, _ g: CGFloat, _ b: CGFloat) -> UIColor {
        UIColor(red: r / 255, green: g / 255, blue: b / 255, alpha: 1)
    }
}
