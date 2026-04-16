package com.workout.shared.di

import com.workout.shared.platform.AndroidAudioFeedback
import com.workout.shared.platform.AndroidForegroundTimerService
import com.workout.shared.platform.AndroidHapticFeedback
import com.workout.shared.platform.AndroidScreenWakeLock
import com.workout.shared.platform.AndroidTimerSettings
import com.workout.shared.platform.AudioFeedback
import com.workout.shared.platform.ForegroundTimerService
import com.workout.shared.platform.HapticFeedback
import com.workout.shared.platform.ScreenWakeLock
import com.workout.shared.platform.TimerSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidPlatformModule = module {
    single<TimerSettings> { AndroidTimerSettings(androidContext()) }
    single<AudioFeedback> { AndroidAudioFeedback(androidContext()) }
    single<HapticFeedback> { AndroidHapticFeedback(androidContext()) }
    single<ScreenWakeLock> { AndroidScreenWakeLock() }
    single<ForegroundTimerService> { AndroidForegroundTimerService(androidContext()) }
}
