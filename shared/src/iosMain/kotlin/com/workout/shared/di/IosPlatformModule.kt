package com.workout.shared.di

import com.workout.shared.platform.AudioFeedback
import com.workout.shared.platform.ForegroundTimerService
import com.workout.shared.platform.HapticFeedback
import com.workout.shared.platform.IosAudioFeedback
import com.workout.shared.platform.IosForegroundTimerService
import com.workout.shared.platform.IosHapticFeedback
import com.workout.shared.platform.IosScreenWakeLock
import com.workout.shared.platform.IosTimerSettings
import com.workout.shared.platform.ScreenWakeLock
import com.workout.shared.platform.TimerSettings
import org.koin.dsl.module

val iosPlatformModule = module {
    single<TimerSettings> { IosTimerSettings() }
    single<AudioFeedback> { IosAudioFeedback() }
    single<HapticFeedback> { IosHapticFeedback() }
    single<ScreenWakeLock> { IosScreenWakeLock() }
    single<ForegroundTimerService> { IosForegroundTimerService() }
}
