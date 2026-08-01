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
     * Advances the reading math for a new [position] (scroll pixels or page index). Returns the
     * number of completed pages since the last call — the caller only persists when > 0.
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

        val completedPages = readChars / charsPerPage.coerceAtLeast(1)
        if (completedPages > 0) {
            readPages += completedPages
            readChars %= charsPerPage
        }
        return completedPages
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
