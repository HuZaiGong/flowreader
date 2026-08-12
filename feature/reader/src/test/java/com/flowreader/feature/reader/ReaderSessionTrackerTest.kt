package com.flowreader.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSessionTrackerTest {

    private var now = 1_000_000L

    private fun freshTracker(): ReaderSessionTracker {
        now = 1_000_000L
        return ReaderSessionTracker(
            pauseThresholdMs = 300_000L,
            emaAlpha = 0.3f,
            nowProvider = { now }
        )
    }

    private fun tick(ms: Long) {
        now += ms
    }

    @Test
    fun pagesAccumulateFromReadCharacters() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        val content = "a".repeat(500)
        // move 300 chars in one update -> 300/400 = 0 pages yet
        tick(1_000L)
        assertEquals(0, tracker.recordProgress(300, content, 400))
        // next 300 chars -> 600 total -> 1 full page (600/400)
        tick(1_000L)
        assertEquals(1, tracker.recordProgress(600, content, 400))
        assertEquals(1, tracker.readPages)
    }

    @Test
    fun pagesCarryOverModuloBetweenUpdates() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        val content = "x".repeat(1000)
        tick(1_000L)
        tracker.recordProgress(250, content, 400)
        tick(1_000L)
        tracker.recordProgress(600, content, 400) // 600 chars total -> 1 page + 200 carry
        assertEquals(1, tracker.readPages)
        tick(1_000L)
        tracker.recordProgress(750, content, 400) // 350 chars -> still 1 page
        assertEquals(1, tracker.readPages)
        tick(1_000L)
        tracker.recordProgress(950, content, 400) // 550 chars -> 2nd page
        assertEquals(2, tracker.readPages)
    }

    @Test
    fun whitespaceCharsDoNotCountAsRead() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        val content = "a".repeat(100) + " ".repeat(400) + "b".repeat(100)
        // 600 raw chars but only 200 non-whitespace
        tick(1_000L)
        assertEquals(0, tracker.recordProgress(600, content, 400))
        assertEquals(0, tracker.readPages)
    }

    @Test
    fun speedEmasTowardsInstantCharsPerMinute() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        val content = "a".repeat(1000)
        tick(1_000L)
        tracker.recordProgress(200, content, 400)
        // 200 chars over 2s since startSession = 6000 chars/min, no EMA smoothing yet
        assertEquals(6000f, tracker.readingSpeed, 0.5f)
        tick(1_000L)
        tracker.recordProgress(400, content, 400)
        // 200 chars over 1s = 12000 chars/min -> 0.3*12000 + 0.7*6000
        assertEquals(0.3f * 12000f + 0.7f * 6000f, tracker.readingSpeed, 0.5f)
        tick(1_000L)
        tracker.recordProgress(450, content, 400)
        // 50 chars over 1s = 3000/min -> 0.3*3000 + 0.7*previous
        assertEquals(0.3f * 3000f + 0.7f * (0.3f * 12000f + 0.7f * 6000f), tracker.readingSpeed, 0.5f)
    }

    @Test
    fun interactionBeyondPauseThresholdSplitsSession() {
        val tracker = freshTracker()
        tracker.startSession()
        assertFalse(tracker.recordInteraction(10))
        tick(301_000L)
        assertTrue(tracker.recordInteraction(50))
        // split restarted the session: pages reset
        assertEquals(0, tracker.readPages)
    }

    @Test
    fun interactionBelowPauseThresholdDoesNotSplit() {
        val tracker = freshTracker()
        tracker.startSession()
        assertFalse(tracker.recordInteraction(10))
        tick(299_000L)
        assertFalse(tracker.recordInteraction(50))
    }

    @Test
    fun snapshotReturnsPagesAndSecondsThenResets() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(10_000L)
        val content = "a".repeat(1000)
        tick(1_000L)
        tracker.recordProgress(800, content, 400)
        assertEquals(2, tracker.readPages)

        tick(60_000L)
        val (pages, seconds) = tracker.takeSnapshotAndReset()
        assertEquals(2, pages)
        assertEquals(71L, seconds)
        assertEquals(0, tracker.readPages)
        // new session starts with zero elapsed time
        assertEquals(0L, tracker.elapsedSeconds)
    }

    /**
     * The PAGED / comic regression: a page index moves by 1 per turn, so feeding it to
     * [ReaderSessionTracker.recordProgress] never completes a page and the caller's
     * `pages > 0` gate drops the whole session — no reading time, no pages.
     */
    @Test
    fun pageIndexThroughCharacterPathNeverCompletesAPage() {
        val tracker = freshTracker()
        tracker.startSession()
        val content = "a".repeat(100_000)
        repeat(30) { page ->
            tick(5_000L)
            tracker.recordProgress(page + 1, content, 900)
        }
        assertEquals(0, tracker.readPages)
    }

    @Test
    fun pageProgressCountsOnePagePerTurn() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        assertEquals(1, tracker.recordPageProgress(1, 900))
        tick(1_000L)
        assertEquals(1, tracker.recordPageProgress(2, 900))
        assertEquals(2, tracker.readPages)
    }

    @Test
    fun pageProgressCountsMultiPageJumps() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        assertEquals(4, tracker.recordPageProgress(4, 900))
        assertEquals(4, tracker.readPages)
    }

    @Test
    fun pageProgressIgnoresBackwardTurns() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        tracker.recordPageProgress(5, 900)
        tick(1_000L)
        assertEquals(0, tracker.recordPageProgress(2, 900))
        assertEquals(5, tracker.readPages)
        // The tracker follows the reader back, so re-reading forward counts again.
        tick(1_000L)
        assertEquals(1, tracker.recordPageProgress(3, 900))
    }

    @Test
    fun pageProgressSpeedScalesByCharsPerPage() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        // 1 page of 900 chars over 1s -> 54000 chars/min, no EMA smoothing on the first sample.
        tracker.recordPageProgress(1, 900)
        assertEquals(54_000f, tracker.readingSpeed, 1f)
    }

    @Test
    fun pageProgressSnapshotPersistsPagesAndSeconds() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        tracker.recordPageProgress(3, 900)
        tick(59_000L)
        val (pages, seconds) = tracker.takeSnapshotAndReset()
        assertEquals(3, pages)
        assertEquals(60L, seconds)
    }

    /** `charsPerPage` was coerced for the division but not for the modulo. */
    @Test
    fun zeroCharsPerPageDoesNotDivideByZero() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        tracker.recordProgress(500, "a".repeat(500), 0)
        tracker.recordPageProgress(2, 0)
    }

    @Test
    fun backwardScrollDoesNotCountChars() {
        val tracker = freshTracker()
        tracker.startSession()
        tick(1_000L)
        val content = "a".repeat(500)
        tick(1_000L)
        tracker.recordProgress(400, content, 400)
        assertEquals(1, tracker.readPages)
        tick(1_000L)
        tracker.recordProgress(100, content, 400) // scrolled back
        assertEquals(1, tracker.readPages)
    }
}
