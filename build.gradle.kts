// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.dagger.hilt.android") version "2.55" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
}

subprojects {
    if (path != ":app") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
    }
}

tasks.register("verifyKotlinStyle") {
    group = "verification"
    description = "Runs ktlint and a lightweight whitespace gate."
    dependsOn(subprojects.filter { it.path != ":app" }.map { it.tasks.named("ktlintCheck") })
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
    description = "Reports JVM test breadth for Repository, ViewModel, domain and :core targets and enforces a 40% file target."
    doLast {
        // v52: the caliber now covers :core and :feature too. Without this, moving a ViewModel or
        // a pure function out of :app would quietly drop it from the denominator and let the gate
        // pass on refactoring alone.
        val sourceFiles = fileTree(rootDir) {
            include(
                "app/src/main/java/**/data/repository/*Repository*.kt",
                "app/src/main/java/**/ui/screens/**/*ViewModel.kt",
                "feature/*/src/main/java/**/*ViewModel.kt",
                "core/src/main/java/**/*.kt",
                "domain/src/main/java/**/repository/*.kt",
                "domain/src/main/java/**/model/*.kt"
            )
            exclude("**/build/**")
        }.files
        val testFiles = fileTree(rootDir) {
            include(
                "app/src/test/java/**/*.kt",
                "core/src/test/java/**/*.kt",
                "feature/*/src/test/java/**/*.kt",
                "domain/src/test/java/**/*.kt"
            )
            exclude("**/build/**")
        }.files
        val ratio = if (sourceFiles.isEmpty()) 1.0 else testFiles.size.toDouble() / sourceFiles.size.toDouble()
        println("Test breadth: ${testFiles.size}/${sourceFiles.size} files = ${"%.1f".format(ratio * 100)}%")
        if (ratio < 0.40) {
            throw GradleException("Test breadth below 40% file target")
        }
    }
}
