// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}

tasks.register("verifyKotlinStyle") {
    group = "verification"
    description = "Lightweight Kotlin style gate: no tabs or trailing whitespace."
    doLast {
        val violations = fileTree(rootDir) {
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", ".gradle/**")
        }.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                when {
                    line.contains('\t') -> "${file.relativeTo(rootDir)}:${index + 1}: tab character"
                    line.endsWith(" ") || line.endsWith("\t") -> "${file.relativeTo(rootDir)}:${index + 1}: trailing whitespace"
                    else -> null
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(violations.joinToString("\n"))
        }
    }
}

tasks.register("coverageSummary") {
    group = "verification"
    description = "Reports JVM test breadth for the v50 rebuilt core modules and enforces a lightweight 40% file target."
    doLast {
        val sourceFiles = listOf(
            file("app/src/main/java/com/flowreader/app/data/repository/AnnotationRepositoryImpl.kt"),
            file("app/src/main/java/com/flowreader/app/data/repository/BookmarkRepositoryImpl.kt"),
            file("app/src/main/java/com/flowreader/app/data/repository/ReadingStatsRepositoryImpl.kt"),
            file("app/src/main/java/com/flowreader/app/data/repository/SearchRepositoryImpl.kt"),
            file("app/src/main/java/com/flowreader/app/util/FullTextSearch.kt"),
            file("app/src/main/java/com/flowreader/app/util/TtsManager.kt"),
            file("domain/src/main/java/com/flowreader/app/domain/model/WheelItem.kt")
        ).filter { it.exists() }
        val testFiles = fileTree(rootDir) {
            include("app/src/test/java/**/*.kt", "domain/src/test/java/**/*.kt")
            exclude("**/build/**")
        }.files
        val ratio = if (sourceFiles.isEmpty()) 1.0 else testFiles.size.toDouble() / sourceFiles.size.toDouble()
        println("Test breadth: ${testFiles.size}/${sourceFiles.size} files = ${"%.1f".format(ratio * 100)}%")
        if (ratio < 0.40) {
            throw GradleException("Test breadth below 40% file target")
        }
    }
}
