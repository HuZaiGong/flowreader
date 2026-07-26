# FlowReader Architecture

FlowReader v51 moves the project from a single application module toward an explicit multi-module Clean Architecture layout.

## Modules

- `:app`: Android application, Hilt graph, navigation shell, widgets, and legacy feature assembly while screen code is migrated.
- `:core`: shared Android/Kotlin utilities that are allowed to be used by data and feature modules.
- `:domain`: domain models and repository contracts. It has no dependency on app, data, Room, Compose UI, or Hilt.
- `:data`: Room/DataStore/repository implementation boundary. New persistence code should live here before being wired into `:app`.
- `:feature:library`: library feature boundary for book shelf UI and ViewModel migration.
- `:feature:reader`: reader feature boundary for reader UI, ViewModel, search, bookmarks, TTS controls, and reading progress migration.

## Dependency Direction

Allowed dependency direction is:

`feature:* -> core/domain`, `data -> core/domain`, and `app -> core/data/domain/feature:*`.

Feature modules must not depend on `:app`. Data modules must not depend on feature modules. Domain remains the stable contract layer.

## v51 Migration Rule

The v51 build includes the target modules and makes them part of normal Gradle, ktlint, test, and assemble verification. Existing app-resident reader/library implementations remain functional while code is moved incrementally behind the new module boundaries.
