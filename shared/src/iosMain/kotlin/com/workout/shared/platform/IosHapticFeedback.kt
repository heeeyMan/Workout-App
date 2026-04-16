package com.workout.shared.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

class IosHapticFeedback : HapticFeedback {

    override fun vibrateShort() {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
            .impactOccurred()
    }

    override fun vibratePrepEnd() {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
            .impactOccurred()
    }

    override fun vibrateAlert() {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid)
            .impactOccurred()
    }

    override fun vibrateFinish() {
        UINotificationFeedbackGenerator()
            .notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }
}
