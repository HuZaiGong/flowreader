# FlowReader

Offline-first Android e-book reader (EPUB/TXT/PDF/Markdown). Single-module, Clean Architecture + MVVM.

## Architecture

```
Composable → ViewModel → UseCase/Repository → Room DAO → SQLite
  ↕              ↕               ↕               ↕
ui/screens    domain/usecase  data/repository  data/local/dao
```

- **DI**: Hilt (`di/AppModule.kt`) — DatabaseModule (`@Provides`) provides 6 DAOs; RepositoryModule (`@Binds`) binds 7 repos (Book, Chapter, Bookmark, Annotation, Category, ReadingStats, Backup).
- **Navigation**: `sealed class Screen` in `ui/Navigation.kt`. Routes: `library`, `wheel`, `stats`, `settings`, `book_detail/{bookId}`, `reader/{bookId}`. 4 bottom tabs (Library, Wheel, Stats, Settings).
- **State**: Each ViewModel exposes a `StateFlow<XxxUiState>`. Private `_uiState` pattern. Progress save uses 3-second debounce (`debouncedSaveProgress()` in `ReaderViewModel.kt:156`).
- **Error handling**: Uses `kotlin.Result` (built-in); custom `com.flowreader.app.domain.model.Result<T>` in `AppException.kt` is unused dead code.
- **Settings**: DataStore Preferences via `SettingsRepository`.
- **Concurrency**: `ChapterRepositoryImpl.contentCache` uses `ConcurrentHashMap`; `BookLoader` uses single `CoroutineScope` instead of per-call scopes.

## Room DB

- 6 entities: Book, Chapter, Bookmark, Annotation, Category, ReadingStats. DB version 4, `exportSchema=false`. KSP schemaLocation arg in build.gradle is **unused** because `exportSchema = false`.
- `data/local/entity/` and `data/local/dao/` mirror each other 1:1.

## Key Classes

| File | Role |
|------|------|
| `util/BookParser.kt` | Parses books from `Uri`, injected into ViewModels |
| `util/BookLoader.kt` | Preloads chapter content; injected into ReaderViewModel |
| `util/CacheManager.kt` | Memory-aware chapter caching |
| `util/TtsManager.kt` | TTS via Android `TextToSpeech` engine; injected into ReaderViewModel |
| `util/FullTextSearch.kt` | Search across all books |
| `util/MemoryManager.kt` | Memory pressure handling |
| `data/repository/BackupRepository.kt` | Import/export backup (domain interface at `domain/repository/BackupRepository.kt`) |
| `data/repository/SettingsRepository.kt` | DataStore wrapper; no domain interface layer |

## Build & Test

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease         # release APK (R8 minified, shrink resources)
./gradlew testDebugUnitTest       # unit tests only
./gradlew clean
```

- **No lint/detekt/ktlint** configured — only `.editorconfig` for style.
- **Pure JUnit 4 + MockK** (`kotlinx-coroutines-test`). Single test file: `util/BookParserTest.kt`.
- Room generates files under `app/build/generated/ksp/` for DAO implementations.
- `coreLibraryDesugaring` enabled (`java.time` backport).

## CI (GitHub Actions)

- **PR**: `testDebugUnitTest` + `assembleDebug` + upload artifact.
- **Push to `main`**: same + `assembleRelease` + create GitHub Release with version from `versionName`.

## Layout Conventions

- Each screen is a package under `ui/screens/` containing `*Screen.kt` and `*ViewModel.kt`.
- Domain interfaces in `domain/repository/`, implementations in `data/repository/`.
- All Kotlin source under `app/src/main/java/com/flowreader/app/`.
