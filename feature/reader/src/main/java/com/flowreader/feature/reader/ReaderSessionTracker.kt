package com.flowreader.feature.reader

/**
 * Tracks one reading session: start time, characters and pages consumed, reading speed (EMA)
 * and the pause-splitting rule that starts a new session after a long inactivity gap.
 *
 * Pure Kotlin with an injectable clock so the JVM unit tests can drive time explicitly.
 * Extracted from `ReaderViewModel` in v54.
 */
class ReaderSessionTracker(
    private val pauseThresholdMs: Long = DEFAULT_PAUSE_THRESHOLD_MS,
    private val emaAlpha: Float = DEFAULT_EMA_ALPHA,
    private val nowProvider: () -> Long = System::currentTimeMillis
) {

    /** Pages completed in the current session; reset on [takeSnapshotAndReset]. */
    var readPages: Int = 0
        private set

    /** The last scroll/page position the tracker has seen. */
    var lastPosition: Int = 0
        private set

    /** EMA reading speed in non-whitespace characters per minute. */
    var readingSpeed: Float = 0f
        private set

    /** Elapsed seconds of the current session. */
    val elapsedSeconds: Long
        get() = (nowProvider() - startTime) / 1000L

    private var startTime = 0L
    private var readChars = 0
    private var lastUpdateTime = 0L
    private var lastInteractionTime = 0L

    fun startSession() {
        val now = nowProvider()
        startTime = now
        lastInteractionTime = now
        lastUpdateTime = now
        readChars = 0
        readPages = 0
        lastPosition = 0
    }

    /**
     * Registers an interaction. Returns true when the gap since the last interaction exceeded
     * [pauseThresholdMs] and the session was split (caller should persist the old session).
     */
    fun recordInteraction(position: Int): Boolean {
        val now = nowProvider()
        val split = lastInteractionTime > 0 && now - lastInteractionTime > pauseThresholdMs
        if (split) {
            startSession()
            lastPosition = position
        }
        lastInteractionTime = now
        return split
    }

    /**
     * Advances the reading math for a new scroll [position] in characters (the `SLIDE` / `NONE`
     * page modes feed scroll pixels, which track character count closely enough). Returns the
     * number of completed pages since the last call — the caller only persists when > 0.
     *
     * `PAGED` and comic reading report a **page index**, not a character offset; those must call
     * [recordPageProgress] instead. Passing a page index here yields a delta of 1 per page turn,
     * which never reaches one page worth of characters, so no reading time is ever persisted.
     */
    fun recordProgress(position: Int, content: String?, charsPerPage: Int): Int {
        val now = nowProvider()
        val positionDelta = (position - lastPosition).coerceAtLeast(0)
        val timeDelta = now - lastUpdateTime

        if (lastUpdateTime > 0 && positionDelta > 0 && timeDelta > 0) {
            val charsPerMinute = positionDelta.toFloat() / (timeDelta / 1000f) * 60f
            readingSpeed = if (readingSpeed > 0f) {
                emaAlpha * charsPerMinute + (1 - emaAlpha) * readingSpeed
            } else {
                charsPerMinute
            }

            val readableDelta = content
                ?.substring(lastPosition.coerceIn(0, content.length), position.coerceIn(0, content.length))
                ?.count { !it.isWhitespace() }
                ?: positionDelta
            readChars += readableDelta
        }

        lastPosition = position
        lastUpdateTime = now

        val pageSize = charsPerPage.coerceAtLeast(1)
        val completedPages = readChars / pageSize
        if (completedPages > 0) {
            readPages += completedPages
            readChars %= pageSize
        }
        return completedPages
    }

    /**
     * Advances the reading math when [pageIndex] is a rendered page index rather than a character
     * offset (`PAGED` mode and comic pages). One forward step is one completed page; [charsPerPage]
     * only scales the speed estimate so it stays in characters per minute.
     *
     * Returns the number of pages completed since the last call.
     */
    fun recordPageProgress(pageIndex: Int, charsPerPage: Int): Int {
        val now = nowProvider()
        val pageDelta = (pageIndex - lastPosition).coerceAtLeast(0)
        val timeDelta = now - lastUpdateTime

        if (lastUpdateTime > 0 && pageDelta > 0 && timeDelta > 0) {
            val charsPerMinute = pageDelta.toFloat() * charsPerPage.coerceAtLeast(1) / (timeDelta / 1000f) * 60f
            readingSpeed = if (readingSpeed > 0f) {
                emaAlpha * charsPerMinute + (1 - emaAlpha) * readingSpeed
            } else {
                charsPerMinute
            }
        }

        lastPosition = pageIndex
        lastUpdateTime = now
        readPages += pageDelta
        return pageDelta
    }

    /** Consumes the current session and returns (pages, seconds) for persistence. */
    fun takeSnapshotAndReset(): Pair<Int, Long> {
        val snapshot = readPages to elapsedSeconds
        startSession()
        return snapshot
    }

    companion object {
        const val DEFAULT_PAUSE_THRESHOLD_MS = 5 * 60 * 1000L
        const val DEFAULT_EMA_ALPHA = 0.3f
    }
}
