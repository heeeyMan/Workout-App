import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.workout.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            api(project(":core"))
            implementation(libs.koin.core)
            implementation(libs.coroutines.core)
            implementation(libs.datetime)
            implementation(libs.serialization.json)

            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            api(libs.navigation.compose)
            api(libs.lifecycle.viewmodel.compose)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Workaround: com.android.kotlin.multiplatform.library does not wire Compose Multiplatform
// resources into the Android asset pipeline (CMP-4237 / AGP 9.0 incompatibility).
// Expose prepared resources via a dedicated configuration so :androidApp can consume them.
val composeAndroidAssets by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

val prepareComposeAndroidAssets by tasks.registering(Sync::class) {
    dependsOn("prepareComposeResourcesTaskForCommonMain")
    from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources")) {
        into("composeResources/workoutapp.shared.generated.resources")
    }
    into(layout.buildDirectory.dir("generated/compose/androidAssets"))
}

artifacts {
    add(composeAndroidAssets.name, layout.buildDirectory.dir("generated/compose/androidAssets")) {
        builtBy(prepareComposeAndroidAssets)
    }
}
