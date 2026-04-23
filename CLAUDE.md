# Workout App — контекст для Claude

Kotlin Multiplatform (KMP): общая логика и Compose UI в `:shared`, данные в `:core`, Android-специфика в `:androidApp`, iOS — SwiftUI + KMM-фреймворк `Shared`.

Kotlin **2.3.0**, AGP **9.0.1** (`gradle/libs.versions.toml`).

---

## Модули

| Модуль | Назначение |
|--------|-----------|
| `:core` | Модели (`Block`, `Workout`), SQLDelight, `WorkoutRepository` |
| `:shared` | MVI-сторы, таймер, Compose Multiplatform UI, платформенные абстракции |
| `:androidApp` | `MainActivity`, Koin-инициализация, foreground-сервис, виджет, тайл, ProGuard |

---

## Архитектура

### MVI
Базовый класс: `shared/.../mvi/BaseStore.kt`. Каждая фича — триада `Contract.kt` / `Store.kt` / `ViewModel.kt`.

| Фича | Путь |
|------|------|
| Таймер | `shared/.../feature/timer/` |
| Домашний экран | `shared/.../feature/home/` |
| Создание тренировки | `shared/.../feature/createworkout/` |

### Платформенные абстракции
Интерфейс в `shared/commonMain/platform/`, Android-реализация в `shared/androidMain/platform/`, iOS — в `shared/iosMain/platform/`.

Ключевые абстракции: `AudioFeedback`, `HapticFeedback`, `TimerSettings`, `ForegroundTimerService`, `NotificationPermission`, `AppReview`, `TextSharer`, `JsonExporter/Importer`, `ScreenWakeLock`, `SoundPresets`.

### DI (Koin)
- `:core` — `core/.../di/CoreModule.kt`
- Android платформа — `shared/androidMain/di/AndroidPlatformModule.kt`
- iOS платформа — `shared/iosMain/di/IosPlatformModule.kt`
- Android app — `androidApp/.../WorkoutApp.kt` (инициализация)

---

## UI (Compose Multiplatform)

Все экраны в `shared/src/commonMain/kotlin/com/workout/shared/ui/`:

| Экран | Файл |
|-------|------|
| Домашний | `home/HomeScreen.kt` |
| Создание/редактирование | `createworkout/CreateWorkoutScreen.kt` |
| Таймер | `timer/TimerScreen.kt` |
| Настройки | `settings/SettingsScreen.kt` |
| Онбординг | `onboarding/OnboardingScreen.kt` |

Навигация: `ui/navigation/Screen.kt` (маршруты) + `AppNavigation.kt` (NavController) + `WorkoutApp.kt` (корень).

Компоненты: `ui/components/WorkoutCard.kt`, `WheelTimePicker.kt`, `ui/util/WorkoutDialog.kt`.

Тема: `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`.

Подробнее: [docs/ui.md](docs/ui.md)

---

## Таймер

Источник правды: `shared/.../feature/timer/TimerStore.kt` + `TimerContract.kt`.

- Тик — `TimerIntent.Tick` раз в секунду
- Пауза — отмена `timerJob`
- Фазовый прогресс: `TimerState.phaseProgress`, `overallProgress`
- Звук/вибрация перед сменой фазы: `feedbackBeforeUi = true` → эффект → setState
- Конец фазы Work: задержка `PHASE_END_UI_DELAY_MS`, повторные тики при нуле игнорируются
- Предупреждение: `TimerEffect.Alert10Seconds` за N секунд до конца Work-фазы

Подробнее: [docs/timer.md](docs/timer.md)

---

## Android-специфика

| Компонент | Путь |
|-----------|------|
| Foreground-сервис таймера | `androidApp/.../timer/WorkoutTimerForegroundService.kt` |
| BroadcastReceiver уведомлений | `androidApp/.../timer/TimerNotificationActionReceiver.kt` |
| App Widget (Glance) | `androidApp/.../widget/WorkoutWidget.kt` |
| Quick Settings Tile | `androidApp/.../tile/WorkoutQuickSettingsTileService.kt` |
| Права доступа | `androidApp/.../ui/permissions/AppEntryPermissionHandler.kt` |

ProGuard: `androidApp/proguard-rules.pro` — содержит правила для Room/WorkManager, kotlinx.serialization, навигационных маршрутов.

Подробнее: [docs/android.md](docs/android.md)

---

## Локализация

**Compose-ресурсы** (shared): `shared/src/commonMain/composeResources/values[-xx]/strings.xml`
**Android-ресурсы** (androidApp): `androidApp/src/androidMain/res/values[-xx]/strings.xml`

Языки: `en`, `ru`, `es`, `pt`, `hi`, `zh`. При добавлении строки обновлять **оба** места во всех языках.

---

## Сборка

```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:assembleRelease
./gradlew :shared:compileKotlinIosSimulatorArm64
```

iOS: Xcode-проект в `iosApp/WorkoutIos.xcodeproj`. После изменений sealed-классов Intent/Effect в `:shared` — пересобрать KMM-фреймворк.

---

## Правила для агента

- Менять только то, что относится к задаче — не раздувать дифф рефакторингом.
- Общая логика таймера — в `:shared`; платформенный звук/вибрация — в `androidApp` / iOS.
- При добавлении новой строки локализации — обновить все 6 языков в обоих местах.
- Composable-функции именуются с большой буквы.
- Сервисы Android: не использовать `CoroutineScope(Dispatchers.IO)` без сохранения ссылки и отмены в `onDestroy`.
- ProGuard: при добавлении новых `@Serializable`-классов или навигационных маршрутов — проверить `proguard-rules.pro`.
