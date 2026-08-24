package com.flowreader.app.di

import javax.inject.Qualifier

/**
 * A [kotlinx.coroutines.CoroutineScope] that lives as long as the process.
 *
 * For work that must finish even though whoever asked for it went away. Search-index maintenance is
 * the motivating case: it used to run inside the search screen's debounced `viewModelScope` job, so
 * every keystroke cancelled it and leaving the screen abandoned a partly-built index — which then
 * had to start over on the next search.
 *
 * Do not use this for work a screen owns; that belongs in `viewModelScope` so it stops when the
 * screen does.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope
