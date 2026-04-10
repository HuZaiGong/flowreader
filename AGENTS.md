# PROJECT KNOWLEDGE BASE

**Generated:** 2026-04-08
**Commit:** 65d4620
**Branch:** main

## OVERVIEW
FlowReader (心流阅读) - Android ebook reader built with Jetpack Compose, supporting EPUB/TXT/PDF/Markdown formats with rich customization.

## STRUCTURE
```
./
├── .github/workflows/       # CI: build.yml (auto-publish main)
├── app/src/main/
│   ├── java/com/flowreader/app/
│   │   ├── data/           # Room DB, repositories
│   │   ├── di/             # Hilt modules
│   │   ├── domain/         # Models, repo interfaces
│   │   ├── ui/             # Compose screens, theme
│   │   ├── util/           # BookParser, TTS, cache managers
│   │   ├── MainActivity.kt
│   │   └── FlowReaderApp.kt
│   └── assets/books/       # Preloaded books
├── build.gradle.kts         # Root (AGP 8.6.0, Kotlin 2.0.21, Hilt 2.50)
└── gradle/                  # Gradle wrapper 8.7
```

## WHERE TO LOOK
| Task | Path | Notes |
|------|------|-------|
| Add screen | `ui/screens/` | Screen+ViewModel pattern |
| Database | `data/local/dao/` + `entity/` | Room entities & DAOs |
| DI config | `di/AppModule.kt` | Hilt module |
| Reading settings | `domain/model/ReadingSettings.kt` | Font, theme, spacing |
| Book parsing | `util/BookParser.kt` | JSoup-based EPUB/TXT |
| Navigation | `ui/Navigation.kt` | Compose Navigation |

## CODE MAP
| Symbol | Type | Location |
|--------|------|----------|
| MainActivity | Activity | app/src/main/java/com/flowreader/app/MainActivity.kt |
| FlowReaderApp | Application @HiltAndroidApp | app/src/main/java/com/flowreader/app/FlowReaderApp.kt |
| FlowReaderApp (UI) | Composable | app/src/main/java/com/flowreader/app/ui/FlowReaderApp.kt |
| AppDatabase | Room DB | app/src/main/java/com/flowreader/app/data/local/AppDatabase.kt |

## CONVENTIONS
- MVVM + Clean Architecture (data/domain/ui layers)
- Hilt DI with `@HiltAndroidApp`; ViewModels via `hiltViewModel()`
- Room + DataStore for persistence
- StateFlow for UI state
- 3-second debounce for progress save
- **Naming collision**: `FlowReaderApp` used for both Application class AND root composable (non-standard)

## ANTI-PATTERNS (THIS PROJECT)
- No DO NOT/NEVER comments in source code
- **Tests exist** at `app/src/test/java/com/flowreader/app/util/BookParserTest.kt` (contradicts old doc)
- CI uses broad `permissions: contents: write` (security concern)
- No .editorconfig, lint.xml, or detekt configs

## BUILD COMMANDS
```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease   # Release APK
./gradlew :app:testDebugUnitTest  # Run unit tests
```

## NOTES
- Preloaded books: `app/src/main/assets/books/诡秘之主.txt`, `神秘复苏.txt`
- 5 reading themes: light, dark, sepia, eye-care, AMOLED black
- PDF uses Android native renderer (no external lib)
- Min SDK 26, Target/Compile SDK 34