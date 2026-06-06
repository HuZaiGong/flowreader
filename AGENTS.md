# Repository Guidelines

## Project Overview

**FlowReader** is an offline-first Android e-book reader supporting EPUB, TXT, PDF, and Markdown formats. It is built with Jetpack Compose and follows Clean Architecture with MVVM. All data is local; there are no network features.

- **Package**: `com.flowreader.app`
- **Min SDK**: 26, **Target/Compile SDK**: 35
- **License**: GPL-3.0

---

## Architecture & Data Flow

The project uses **Clean Architecture** layered as follows:

```
UI (Compose Screens + ViewModels)
  ↕
Domain (Models, Repository Interfaces, UseCases)
  ↕
Data (Repository Impl, Room DB, DAOs, Entities)
```

- **UI Layer**: `ui/screens/` contains one package per screen with its Composable and `*ViewModel`. Root navigation is in `Navigation.kt`.
- **Domain Layer**: `domain/model/` holds data classes and sealed classes; `domain/repository/` holds interfaces; `domain/usecase/` holds business logic like `GetBookUseCase`.
- **Data Layer**: `data/local/` (Room DB, DAOs, entities) and `data/repository/` (implementations). `BackupRepository.kt` also lives here.

Data flow: **Composable → ViewModel → UseCase/Repository → Room DAO → SQLite**.

Key DI wiring: `di/AppModule.kt` contains both `DatabaseModule` (`@Provides`) and `RepositoryModule` (`@Binds`).

---

## Key Directories

| Directory | Purpose |
|-----------|---------|
| `app/src/main/java/com/flowreader/app/` | All Kotlin source code |
| `data/local/entity/` | Room entities (6 tables) |
| `data/local/dao/` | Room DAOs |
| `data/repository/` | Repository implementations |
| `domain/model/` | Domain models and `AppException` |
| `domain/usecase/` | Business logic / UseCases |
| `ui/screens/` | Screen packages (`library/`, `reader/`, `bookdetail/`, `settings/`, `stats/`, `wheel/`) |
| `ui/theme/` | Compose theme (`Color.kt`, `Theme.kt`, `Typography.kt`) |
| `util/` | Utility classes: `BookParser`, `BookLoader`, `TtsManager`, `FullTextSearch`, `MemoryManager`, `CacheManager` |
| `app/src/test/java/...` | Unit tests (JUnit 4) |
| `.github/workflows/` | CI/CD (GitHub Actions) |

---

## Development Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (R8 minified)
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest

# Clean build
./gradlew clean
```

**Environment**: JDK 17 required. Android SDK 35.

---

## Code Conventions & Common Patterns

### Naming
- **Packages**: all lowercase, matching directory structure.
- **Classes**: `PascalCase` — e.g., `BookRepositoryImpl`, `ReaderViewModel`.
- **Functions / Variables**: `camelCase`.
- **Constants**: `SCREAMING_SNAKE_CASE` or top-level `val` in objects.

### Dependency Injection
- Uses **Hilt** for DI.
- `FlowReaderApplication.kt` is annotated with `@HiltAndroidApp`.
- `di/AppModule.kt` contains both `@Provides` (DatabaseModule) and `@Binds` (RepositoryModule).
- Inject ViewModels with `hiltViewModel()`.

### State Management
- Compose `StateFlow` exposed from ViewModels, collected in Composables.
- `derivedStateOf` used for expensive UI computations.
- Progress save uses a **3-second debounce** to reduce DB writes.

### Error Handling
- `AppException` is a sealed class for domain errors (`DatabaseError`, `FileError`, `ParseError`, etc.).
- Use `Result<T>` wrapper for operations that can fail.

### Async Patterns
- Kotlin **Coroutines + Flow** for asynchronous work.
- Room queries return `Flow<T>`.
- `viewModelScope.launch` for ViewModel-bound work.

### Compose Conventions
- Root composable: `FlowReaderRoot()` (in `ui/FlowReaderApp.kt`)
- Navigation uses a `sealed class Screen` with route definitions.
- Bottom navigation has 4 tabs: Library, Wheel, Stats, Settings.
- Material 3 theming with dynamic colors (Material You).

---

## Important Files

| File | Role |
|------|------|
| `app/build.gradle.kts` | App-level build config (AGP 8.6.0, Kotlin 2.0.21, Compose BOM 2024.12.01) |
| `build.gradle.kts` | Root project plugins (Hilt, KSP, Compose) |
| `settings.gradle.kts` | Project name and included modules |
| `gradle.properties` | Gradle JVM args, AndroidX, caching flags |
| `app/src/main/AndroidManifest.xml` | App manifest, permissions, MainActivity |
| `FlowReaderApplication.kt` | `@HiltAndroidApp` entry point |
| `MainActivity.kt` | Sets Compose content root, edge-to-edge |
| `Navigation.kt` | `NavHost`, `BottomNavigation`, `Screen` sealed class |
| `di/AppModule.kt` | Hilt modules for DB and Repositories |
| `data/local/AppDatabase.kt` | Room DB (v4), `flowreader_db` |
| `proguard-rules.pro` | R8 ProGuard rules for release builds |

---

## Runtime/Tooling Preferences

- **Build System**: Gradle (wrapper 8.7)
- **AGP**: 8.6.0
- **Kotlin**: 2.0.21 (KSP 2.0.21-1.0.27)
- **JDK**: 17
- **No extra runtime** (no Bun, Node, Python, etc.)
- `coreLibraryDesugaring` enabled for `java.time` backport

---

## Testing & QA

- **Framework**: JUnit 4 + MockK
- **Coroutines Test**: `kotlinx-coroutines-test`
- **Location**: `app/src/test/java/com/flowreader/app/util/BookParserTest.kt`
- **CI**: GitHub Actions (`build.yml`)
  - PRs: run unit tests + build debug APK + upload artifact
  - Push to `main`: run unit tests + build debug + build release + create GitHub release
  - Uses JDK 17 Temurin, caches Gradle packages

### Running Tests
```bash
./gradlew testDebugUnitTest    # Unit tests only
./gradlew test                 # All tests (unit + instrumented if connected)
```

No lint/detekt/ktlint configured; only `.editorconfig` for code style.
