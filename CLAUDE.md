# Workout App — контекст для Claude

Kotlin Multiplatform: общая логика тренировок и таймера в `:shared`, данные в `:core`, Android UI в `:androidApp`, iOS в `iosApp/` (SwiftUI + фреймворк `Shared`).

## Модули

| Модуль       | Назначение |
|-------------|------------|
| `:core`     | Модели (`Block`, упражнения/отдых), SQLDelight, `WorkoutRepository`. |
| `:shared`   | MVI-сторы (`BaseStore`), таймер (`TimerStore`, `TimerContract`), списки/домашний экран/создание тренировки. |
| `:androidApp` | Jetpack Compose, Koin, `TimerViewModel`, `TimerScreen`, `TimerFeedback`, foreground service уведомления. |

Корень Gradle: `settings.gradle.kts` — `androidApp`, `core`, `shared`. Kotlin **2.3.0**, AGP **9.0.1** (`gradle/libs.versions.toml`).

## Таймер (важно для правок)

- **Источник правды**: `shared/src/commonMain/kotlin/com/workout/shared/feature/timer/TimerStore.kt` + `TimerContract.kt` (`TimerState`, `TimerIntent`, `TimerEffect`).
- Тик раз в секунду: `TimerIntent.Tick`; пауза отменяет `timerJob`.
- **Прогресс**: `TimerState.phaseProgress` и `overallProgress` (доля текущей фазы и всей тренировки). На экране «1» и при `secondsRemaining == 0` перед сменой фазы фазовая полоска считается заполненной (`1f`).
- **Конец фазы работы/отдыха** (не подготовка): краткая задержка `PHASE_END_UI_DELAY_MS` после `secondsRemaining = 0`, затем при `feedbackBeforeUi = true` сначала звук/вибрация, потом `setState` следующей фазы. Повторные тики при «зависшем» нуле игнорируются.
- **Предупреждение в конце работы**: последние N секунд фазы Work — эффект `TimerEffect.Alert10Seconds` (поле `secondsRemainingAfterTick` + `withVibration` для первого тика окна).
- **Android**: эффекты обрабатываются в `TimerViewModel`; звуки — `androidApp/.../feedback/TimerFeedback.kt`.
- **iOS**: `WorkoutIosAppController`, `TimerView.swift`, `TimerFeedback.swift`, настройки `TimerUserSettings.swift` (ключи как на Android `timer_settings`).

## Сборка

```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
```

iOS: Xcode-проект в `iosApp/WorkoutIos.xcodeproj`, зависимость от собранного KMM-фреймворка `Shared`.

## Правила для агента

- Менять только то, что относится к задаче; не раздувать дифф рефакторингом.
- Общая логика таймера — в `:shared`; платформенный звук/вибрация — в `androidApp` / iOS.
- После изменений sealed-классов эффектов/intent в shared пересобрать фреймворк для iOS.

## Полезные пути

- Android таймер UI: `androidApp/src/androidMain/kotlin/com/workout/android/ui/timer/TimerScreen.kt`
- Android настройки таймера: `.../ui/settings/SettingsScreen.kt`, `TimerPreferences.kt`
- Общий MVI: `shared/src/commonMain/kotlin/com/workout/shared/mvi/BaseStore.kt`
