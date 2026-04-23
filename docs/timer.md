# Таймер — детальная архитектура

## Ключевые файлы

| Файл | Назначение |
|------|-----------|
| `shared/.../feature/timer/TimerContract.kt` | `TimerState`, `TimerIntent`, `TimerEffect` |
| `shared/.../feature/timer/TimerStore.kt` | Вся логика таймера |
| `shared/.../feature/timer/TimerViewModel.kt` | Мост между Store и Compose UI |
| `shared/.../ui/timer/TimerScreen.kt` | Compose UI таймера |
| `shared/androidMain/.../platform/AndroidForegroundTimerService.kt` | Связь со службой Android |
| `androidApp/.../timer/WorkoutTimerForegroundService.kt` | Android Foreground Service |

---

## Фазы тренировки

Порядок: **Prep → Work → Rest → Work → Rest → ...** (по блокам/повторениям)

- `Block.Exercise`: фазы Prep (опционально) → Work → Rest × repeats
- `Block.Rest`: одна фаза Rest

---

## Логика тиков

```
TimerIntent.Tick
  ├── secondsRemaining > 0 → декремент, обновить прогресс
  │     └── если Work и попали в окно предупреждения → TimerEffect.Alert10Seconds
  └── secondsRemaining == 0 → конец фазы
        ├── задержка PHASE_END_UI_DELAY_MS
        ├── feedbackBeforeUi = true → звук/вибрация (эффект PlaySound / Vibrate)
        └── setState следующей фазы (или TimerEffect.WorkoutFinished)
```

Повторные тики при `secondsRemaining == 0` игнорируются флагом `phaseEndHandled`.

---

## Прогресс

- `phaseProgress` — доля прошедшего времени текущей фазы (`0f..1f`)
- `overallProgress` — доля всей тренировки
- При `secondsRemaining == 0` перед сменой фазы оба возвращают `1f`

---

## Эффекты (TimerEffect)

| Эффект | Когда |
|--------|-------|
| `PlaySound(preset)` | Начало фазы Work / Rest / Finish |
| `Vibrate(pattern)` | Вместе или вместо звука |
| `Alert10Seconds(secondsRemaining, withVibration)` | Последние N секунд Work-фазы |
| `WorkoutFinished` | Конец последней фазы |
| `NavigateBack` | После завершения (по кнопке) |

---

## Настройки таймера (TimerSettings)

Хранятся в `SharedPreferences` (Android) / `UserDefaults` (iOS) с префиксом `timer_settings`.

| Ключ | Описание |
|------|---------|
| `soundEnabled` | Глобальный флаг звука |
| `vibrationEnabled` | Глобальный флаг вибрации |
| `blockPrepDurationSeconds` | Время подготовки перед фазой |
| `workPhaseEndWarningSeconds` | За сколько секунд предупреждать |
| `timerQuickAdjustEnabled` | Кнопки ±10 сек |
| `workStartSoundPresetId` | Пресет звука начала Work |
| `restStartSoundPresetId` | Пресет звука начала Rest |
| `workoutFinishSoundPresetId` | Пресет звука завершения |
| `workPhaseWarningSoundPresetId` | Пресет звука предупреждения |
| `onboardingCompleted` | Флаг онбординга |

---

## Foreground Service (Android)

`WorkoutTimerForegroundService` запускается при старте таймера:
- Показывает уведомление с MediaStyle (кнопки: предыдущая / пауза / следующая)
- Обновляется через `buildNotificationStatic()` без перезапуска сервиса
- При уничтожении обновляет виджет через `GlanceAppWidgetManager`

Управление из уведомления: `TimerNotificationActionReceiver` → `AndroidForegroundTimerService.dispatchCallback`.
