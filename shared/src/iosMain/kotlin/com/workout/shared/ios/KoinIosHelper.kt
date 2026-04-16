package com.workout.shared.ios

import com.workout.core.database.DatabaseDriverFactory
import com.workout.core.di.coreModule
import com.workout.shared.di.iosPlatformModule
import com.workout.shared.di.sharedModule
import org.koin.core.context.startKoin

fun initKoinIos() {
    startKoin {
        modules(
            coreModule(DatabaseDriverFactory()),
            sharedModule,
            iosPlatformModule
        )
    }
}
