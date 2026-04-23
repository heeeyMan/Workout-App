# Android — специфика платформы

## Структура androidApp

```
androidApp/src/androidMain/
├── kotlin/com/workout/android/
│   ├── MainActivity.kt               — единственная Activity, Compose window
│   ├── WorkoutApp.kt                 — Application class, Koin.startKoin()
│   ├── timer/
│   │   ├── WorkoutTimerForegroundService.kt   — Foreground Service таймера
│   │   └── TimerNotificationActionReceiver.kt — BroadcastReceiver кнопок уведомления
│   ├── widget/
│   │   ├── WorkoutWidget.kt          — Glance AppWidget
│   │   └── WorkoutWidgetReceiver.kt  — GlanceAppWidgetReceiver
│   ├── tile/
│   │   └── WorkoutQuickSettingsTileService.kt — Quick Settings Tile
│   └── ui/permissions/
│       └── AppEntryPermissionHandler.kt       — запрос разрешений при старте
└── res/
    ├── values[-xx]/strings.xml       — строки (6 языков)
    ├── raw/                          — MP3-звуки таймера
    ├── drawable/                     — иконки, фон виджета
    ├── xml/                          — widget_info, shortcuts, file_provider_paths
    └── mipmap-*/                     — иконки лаунчера
```

---

## Foreground Service таймера

**`WorkoutTimerForegroundService`**

- Запускается action `com.workout.android.timer.RUN`
- Уведомление: `NotificationCompat.Builder` + `MediaStyle` с `MediaSession`
- `MediaSession` обрабатывает кнопки гарнитуры/медиа (onPlay/onPause/onSkipToNext/onSkipToPrevious)
- Прямые обновления уведомления (без перезапуска сервиса): `buildNotificationStatic()` вызывается из `AndroidForegroundTimerService` через reflection
- При `onDestroy`: обновляет виджет через `GlanceAppWidgetManager` (scope отменяется после завершения корутины)

**Цвета уведомления** по фазе:
- Work → `#FF6B35` (оранжевый)
- Rest → `#4CAF50` (зелёный)
- Prep → `#9E9E9E` (серый)

---

## BroadcastReceiver уведомлений

**`TimerNotificationActionReceiver`**

Actions:
- `ACTION_TOGGLE_PAUSE` → `TimerIntent.TogglePause`
- `ACTION_SKIP_PHASE` → `TimerIntent.SkipPhase`
- `ACTION_PREVIOUS_PHASE` → `TimerIntent.PreviousPhase`

Вызывает `AndroidForegroundTimerService.dispatchCallback` (статический callback, выставляется из `TimerViewModel`).

---

## App Widget (Glance)

**`WorkoutWidget`** — показывает последнюю запущенную тренировку или подсказку создать.

- Данные: `WorkoutRepository.getWorkouts().first()` — последняя по `lastStartedAt`
- Клик → `MainActivity` с `EXTRA_WORKOUT_ID` или `EXTRA_OPEN_CREATE`
- Иконка play: `R.drawable.ic_widget_play` (44dp)
- Цвет текста: `R.color.widget_text_primary` / `widget_text_secondary`
- Фон: `R.drawable.widget_background`
- `defaultWeight()` доступен только внутри `Row { }` (scoped extension на `RowScope`)

Обновляется при: старте/остановке сервиса, явном `WorkoutWidget().update()`.

---

## Quick Settings Tile

**`WorkoutQuickSettingsTileService`**

- Показывает последнюю тренировку в тайле
- По нажатию запускает `MainActivity` с `EXTRA_WORKOUT_ID`
- Coroutine scope: `serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`, отменяется в `onDestroy`

---

## Разрешения

**`AppEntryPermissionHandler`** — запрашивает при первом запуске:
- `POST_NOTIFICATIONS` (Android 13+)
- `SCHEDULE_EXACT_ALARM` (для точных будильников)

Логика включения звука в настройках: если разрешение не выдано — либо `request()`, либо `openSettings()` (если `shouldOpenSettings == true`).

---

## ProGuard (`proguard-rules.pro`)

Правила, которые уже добавлены:

| Причина | Правило |
|---------|---------|
| kotlinx.serialization `$serializer` | `-if @Serializable class ** { static **$serializer INSTANCE; } -keep class <1>$serializer { *; }` |
| Room/WorkManager (через Glance) | `-keep class * extends RoomDatabase`, `-keep class androidx.work.**` |
| androidx.startup | `-keep class * implements Initializer` |
| Модели и репозиторий | `-keep class com.workout.core.model.**`, `.repository.**` |
| Навигационные маршруты | `-keep class com.workout.shared.ui.navigation.**` |
| Бэкап DTO | `-keep class com.workout.shared.backup.**` |

**При добавлении** новых `@Serializable` классов или маршрутов навигации — проверить, нужно ли расширять правила.

---

## Ресурсы: строки

Android-ресурсы (`res/values[-xx]/strings.xml`) — для уведомлений, виджета, тайла, системных элементов.

Compose-ресурсы (`composeResources/values[-xx]/strings.xml`) — для экранов приложения.

**Оба места** нужно обновлять при добавлении строк.

---

## Intent extras (MainActivity)

| Константа | Тип | Назначение |
|-----------|-----|-----------|
| `EXTRA_WORKOUT_ID` | Long | Открыть таймер с конкретной тренировкой |
| `EXTRA_OPEN_CREATE` | Boolean | Открыть экран создания тренировки |
