package com.flowreader.app.domain.model

import java.lang.reflect.Modifier
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the promise made by `compose_compiler_config.conf` at the repo root.
 *
 * `:domain` deliberately has no Compose compiler plugin, so the Compose compiler cannot see these
 * classes and infers every one of them as unstable. That made `book: Book` a composable parameter
 * no skip check could ever pass — every `LibraryUiState` change re-executed every visible book
 * card. The config file fixes that by asserting `com.flowreader.app.domain.model.*` is stable.
 *
 * Nothing enforces that assertion at compile time. If someone adds a `var`, or exposes a
 * `MutableList`, Compose keeps believing the old value and the UI goes stale **without crashing** —
 * the worst possible failure mode. These tests are that missing enforcement, and they live in
 * `:domain` so they run without an Android or Compose dependency.
 */
class ModelStabilityContractTest {

    private val models = listOf(
        Book::class.java,
        Chapter::class.java,
        Category::class.java,
        Annotation::class.java,
        SearchResult::class.java,
        SearchQuery::class.java,
        Bookmark::class.java,
        ReadingList::class.java,
        ReadingListEntry::class.java,
        ReadingListBook::class.java,
        ReadingSettings::class.java,
        GestureSettings::class.java,
        AppSettings::class.java,
        ReadingStats::class.java,
        DailyStats::class.java,
        ReadingSummary::class.java,
        ReadingReport::class.java,
        WheelItem::class.java,
        WheelConfig::class.java,
        WheelResult::class.java,
        GlobalSearchResult::class.java
    )

    @Test
    fun everyModelFieldIsFinal() {
        val offenders = models.flatMap { type ->
            type.declaredFields
                .filterNot { it.isSynthetic }
                .filterNot { Modifier.isStatic(it.modifiers) }
                .filterNot { Modifier.isFinal(it.modifiers) }
                .map { "${type.simpleName}.${it.name}" }
        }

        assertTrue(
            "compose_compiler_config.conf declares these types stable, so every property must be " +
                "a val. Mutable properties found: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun noModelExposesAMutableCollection() {
        val mutableTypes = setOf(
            "java.util.ArrayList",
            "java.util.HashMap",
            "java.util.HashSet",
            "java.util.LinkedHashMap",
            "java.util.LinkedHashSet"
        )

        val offenders = models.flatMap { type ->
            type.declaredFields
                .filterNot { it.isSynthetic }
                .filter { it.type.name in mutableTypes }
                .map { "${type.simpleName}.${it.name}: ${it.type.name}" }
        }

        // Kotlin's read-only List/Set/Map erase to the java.util interfaces, which is fine — the
        // declared Kotlin type prevents writes. A concrete ArrayList field is not fine: it hands
        // callers a mutable reference that Compose would never see change.
        assertTrue(
            "Declared-mutable collection fields break the stability promise: $offenders",
            offenders.isEmpty()
        )
    }
}
