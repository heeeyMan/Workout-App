import java.io.File
import java.util.Properties
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Abstract task with DirectoryProperty output – required by AGP addGeneratedSourceDirectory API.
// Configuration-cache-safe: no Project capture, all inputs/outputs are abstract properties.
abstract class CopyToDirectoryTask : DefaultTask() {
    @get:InputFiles
    abstract val inputDirs: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = outputDirectory.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        inputDirs.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val target = out.resolve(file.relativeTo(dir))
                        target.parentFile.mkdirs()
                        file.copyTo(target, overwrite = true)
                    }
            }
        }
    }
}

// Configuration that resolves Compose Multiplatform resources from :shared.
val sharedComposeAssets: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.workout.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.workout.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            kotlin.setSrcDirs(listOf("src/androidMain/kotlin"))
            res.setSrcDirs(listOf("src/androidMain/res"))
        }
    }

    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core"))
    sharedComposeAssets(project(":shared", "composeAndroidAssets"))

    // Koin
    implementation(libs.koin.android)

    // Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coroutines.core)
    implementation(libs.glance.appwidget)
    implementation(libs.play.review)
}

// Wire :shared Compose Multiplatform resources into each Android variant's asset source set.
androidComponents {
    onVariants { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val copyTask = tasks.register<CopyToDirectoryTask>("copySharedComposeAssetsFor${variantName}") {
            inputDirs.from(configurations["sharedComposeAssets"])
            outputDirectory.set(layout.buildDirectory.dir("generated/sharedComposeAssets/${variant.name}"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(copyTask, CopyToDirectoryTask::outputDirectory)
    }
}
