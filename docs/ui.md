# UI — экраны, навигация, компоненты

## Экраны

Все в `shared/src/commonMain/kotlin/com/workout/shared/ui/`:

### HomeScreen (`home/HomeScreen.kt`)
Список тренировок. Composable-функции:
- `HomeScreen` — точка входа, VM, эффекты
- `DeleteConfirmDialog` — диалог удаления
- `HomeTopBar` — TopAppBar с кнопкой настроек
- `LoadingContent` / `EmptyContent` — состояния загрузки и пустого списка
- `WorkoutListContent` — LazyColumn с секциями «Последняя» и «Все»

### CreateWorkoutScreen (`createworkout/CreateWorkoutScreen.kt`)
Создание и редактирование тренировки. Composable-функции:
- `CreateWorkoutScreen` — точка входа, drag-and-drop state
- `CreateWorkoutTopBar` — TopAppBar с заголовком и суммарным временем
- `WorkoutNameField` — поле названия
- `AddBlockButtons` — кнопки «+ Упражнение» / «+ Отдых»
- `BlockCard` — карточка блока с drag-and-drop
- `BlockCardHeader` — шапка карточки (цвет-индикатор, тип, drag handle, иконки)
- `ExerciseBlockContent` / `RestBlockContent` — содержимое блоков
- `RepeatsRow` — строка количества повторений
- `DurationChip` — тап-чип для выбора длительности
- `RepeatButton` — кнопка ±
- `NameEditDialog` / `DurationPickerDialog` — диалоги редактирования

### TimerScreen (`timer/TimerScreen.kt`)
Экран таймера. Ключевые детали:
- Gym mode (полноэкранный режим)
- Блокировка кнопок (long press для разблокировки)
- Кнопки ±10 секунд (если включено в настройках)
- Прогресс-бар фазы и общий прогресс

### SettingsScreen (`settings/SettingsScreen.kt`)
Настройки таймера. Composable-функции:
- `SettingsScreen` — точка входа, state
- `SoundPickerSheet` — ModalBottomSheet выбора звука
- `ImportResultDialog` / `PermissionRationaleDialog` / `PrepTimeDialog` / `WorkPhaseWarnDialog`
- `SecondsInputField` — общее поле ввода секунд (чёрные цвета для белого диалога)
- `SettingsSwitchRow` / `SettingsValueRow` / `SettingsClickRow` / `SettingsSectionHeader` / `SettingsPresetRow`

### OnboardingScreen (`onboarding/OnboardingScreen.kt`)
Шаблоны тренировок при первом запуске.
- Одна кнопка: «Пропустить» (если ничего не выбрано) / «Добавить (N)» (если выбраны шаблоны)
- `windowInsetsPadding(WindowInsets.navigationBars)` — кнопка не перекрывается системной навигацией

---

## Навигация

```
shared/.../ui/navigation/
├── Screen.kt          — @Serializable sealed class (маршруты)
├── AppNavigation.kt   — NavHost + composable destinations
└── WorkoutApp.kt      — корневой Composable (тема, onboarding gate)
```

Маршруты: `HomeRoute`, `CreateWorkoutRoute(workoutId)`, `TimerRoute(workoutId)`, `SettingsRoute`.

---

## Переиспользуемые компоненты

| Компонент | Файл | Назначение |
|-----------|------|-----------|
| `WorkoutCard` | `components/WorkoutCard.kt` | Карточка тренировки в списке |
| `WheelTimePicker` | `components/WheelTimePicker.kt` | Барабанный выбор минут/секунд |
| `WorkoutDialog` | `util/WorkoutDialog.kt` | Базовый AlertDialog с confirm/dismiss |
| `BackHandler` | `util/BackHandler.kt` | expect/actual перехват кнопки назад |

---

## Тема (`ui/theme/`)

| Файл | Содержимое |
|------|-----------|
| `Color.kt` | Кастомные цвета: `TimerWorkOrange`, `TimerRestGreen`, `BrownContainer`, `OnBrownContainer`, `SurfaceVariant` |
| `Theme.kt` | `WorkoutAppTheme` — Material3, dark/light |
| `Type.kt` | Типографика |

---

## Диалоги с текстовыми полями

Все `OutlinedTextField` внутри диалогов (белый фон) требуют явных цветов, иначе текст невидим в тёмной теме:

```kotlin
CompositionLocalProvider(
    LocalTextSelectionColors provides TextSelectionColors(
        handleColor = Color.Black,
        backgroundColor = Color.Black.copy(alpha = 0.3f)
    )
) {
    OutlinedTextField(
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedBorderColor = Color.Black,
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.Black
        )
    )
}
```

Для позиции курсора в конце текста при открытии: `TextFieldValue(text, selection = TextRange(text.length))`.

---

## Локализация

Языки: `en` (дефолт), `ru`, `es`, `pt`, `hi`, `zh`.

При добавлении новой строки обновить **оба** места:
1. `shared/src/commonMain/composeResources/values[-xx]/strings.xml`
2. `androidApp/src/androidMain/res/values[-xx]/strings.xml`

Строки для уведомлений/виджета/тайла живут только в Android-ресурсах.
