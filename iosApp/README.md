# Workout — iOS (SwiftUI)

Нативное iOS-приложение использует KMP-фреймворк **`Shared`** (и внутри него **`Core`**): та же БД и сторы, что на Android.

Минимальная версия: **iOS 16** (`NavigationStack`). В линковку таргета добавлен **`-lsqlite3`**, потому что SQLDelight/native driver тянет символы SQLite из системной библиотеки.

## Сборка

1. Откройте **`WorkoutIos.xcodeproj`** в Xcode (15+).
2. Выберите симулятор **iPhone** (например `arm64`).
3. **Product → Build** — в начале фазы **Run Script** выполнится  
   `./gradlew :shared:embedAndSignAppleFrameworkForXcode` из корня репозитория (нужны JDK и сеть при первом запуске Gradle).
4. При необходимости задайте **Signing Team** в настройках таргета **WorkoutIos** (`PRODUCT_BUNDLE_IDENTIFIER` по умолчанию `com.workout.ios`).

Из терминала (корень репозитория):

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

Путь к фреймворку: `shared/build/xcode-frameworks/<Debug|Release>/<iphoneos|iphonesimulator>/Shared.framework`.

## Редактирование тренировки

На главном экране у каждой строки есть **карандаш** → открывается **`CreateWorkoutView`** с `workoutId`.  
**Save** вызывает `CreateWorkoutIntent.Save` (запись в SQLite, как на Android). **Cancel** — `Discard`, данные в БД не меняются.

Имя и список блоков отображаются; детальное редактирование блоков на iOS пока только через общую модель (при необходимости расширьте форму в `CreateWorkoutView.swift`).

## Имена типов Kotlin в Swift

Если Xcode ругается на имена `HomeIntent…` / `CreateWorkoutIntent…`, откройте **`Shared.framework/Headers/Shared.h`** или используйте автодополнение после `import Shared`.

## Структура

| Файл | Назначение |
|------|------------|
| `WorkoutIos/WorkoutIosApp.swift` | `@main`, `WorkoutIosAppController` |
| `WorkoutIos/HomeView.swift` | Список, старт, **редактирование**, удаление |
| `WorkoutIos/CreateWorkoutView.swift` | Загрузка тренировки, Save / Cancel |
| `shared/.../ios/WorkoutIosAppController.kt` | Репозиторий, подписки на эффекты |
