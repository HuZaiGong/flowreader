package com.flowreader.app.core.util

import com.flowreader.app.core.designsystem.reader.ReaderPalettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The point of these tests is the claim the whole feature rests on: with the scrim in place, body
 * text over a user-supplied image is still measurably readable. If that stops holding, importing a
 * background silently defeats `ReaderPaletteContrastTest`.
 */
class ReaderBackgroundImageTest {

    @Test
    fun scrimAlphaIsClampedIntoTheReadableRange() {
        assertEquals(ReaderBackgroundImage.MIN_SCRIM_ALPHA, ReaderBackgroundImage.clampScrimAlpha(0f), 1e-6f)
        assertEquals(ReaderBackgroundImage.MIN_SCRIM_ALPHA, ReaderBackgroundImage.clampScrimAlpha(-3f), 1e-6f)
        assertEquals(ReaderBackgroundImage.MAX_SCRIM_ALPHA, ReaderBackgroundImage.clampScrimAlpha(2f), 1e-6f)
        assertEquals(0.8f, ReaderBackgroundImage.clampScrimAlpha(0.8f), 1e-6f)
    }

    /** NaN reaching a Compose alpha would render nothing at all, so it must not pass through. */
    @Test
    fun nanAlphaFallsBackToTheDefault() {
        assertEquals(
            ReaderBackgroundImage.DEFAULT_SCRIM_ALPHA,
            ReaderBackgroundImage.clampScrimAlpha(Float.NaN),
            1e-6f
        )
    }

    @Test
    fun defaultAlphaSitsInsideTheAllowedRange() {
        assertTrue(ReaderBackgroundImage.DEFAULT_SCRIM_ALPHA >= ReaderBackgroundImage.MIN_SCRIM_ALPHA)
        assertTrue(ReaderBackgroundImage.DEFAULT_SCRIM_ALPHA <= ReaderBackgroundImage.MAX_SCRIM_ALPHA)
    }

    @Test
    fun blankPathsAreNotAnActiveBackground() {
        assertFalse(ReaderBackgroundImage.isActive(null))
        assertFalse(ReaderBackgroundImage.isActive(""))
        assertFalse(ReaderBackgroundImage.isActive("   "))
        assertTrue(ReaderBackgroundImage.isActive("/data/user/0/app/files/backgrounds/bg_1.webp"))
    }

    @Test
    fun fullyOpaqueScrimReproducesThePaletteBackgroundExactly() {
        val palette = ReaderPalettes.Paper
        val effective = ReaderBackgroundImage.effectiveBackgroundArgb(
            scrimArgb = palette.backgroundArgb,
            worstCaseUnderlyingArgb = 0xFF000000L,
            scrimAlpha = 1.0f
        )
        assertEquals(palette.backgroundArgb, effective)
    }

    /**
     * Verifies the compositing arithmetic itself. Uses 0.6 rather than 0.5 because
     * [ReaderBackgroundImage.effectiveBackgroundArgb] clamps its alpha first — asking for 0.5 would
     * silently be answered at the floor, and the assertion would be testing the clamp, not the mix.
     */
    @Test
    fun alphaBlendsTowardTheUnderlyingColor() {
        val effective = ReaderBackgroundImage.effectiveBackgroundArgb(
            scrimArgb = 0xFFFFFFFFL,
            worstCaseUnderlyingArgb = 0xFF000000L,
            scrimAlpha = 0.6f
        )
        assertEquals(0xFFL, (effective shr 24) and 0xFF)
        listOf(16, 8, 0).forEach { shift ->
            // 255 * 0.6 + 0 * 0.4 = 153, truncated by the Long conversion.
            assertEquals(153L, (effective shr shift) and 0xFF)
        }
    }

    @Test
    fun compositedResultIsAlwaysOpaque() {
        val effective = ReaderBackgroundImage.effectiveBackgroundArgb(0x00123456L, 0x00654321L, 0.6f)
        assertEquals(0xFFL, (effective shr 24) and 0xFF)
    }

    /**
     * The core guarantee, asserted for all 18 palettes rather than a sampled few: at the default
     * alpha, body text clears AA even against the worst possible image underneath.
     */
    @Test
    fun bodyTextClearsAaOnEveryPaletteAtTheDefaultAlpha() {
        ReaderPalettes.all.forEach { palette ->
            assertTrue(
                "${palette.id} body text fails AA at the default scrim alpha",
                ReaderBackgroundImage.bodyTextStaysReadable(
                    textArgb = palette.textArgb,
                    scrimArgb = palette.backgroundArgb,
                    scrimAlpha = ReaderBackgroundImage.DEFAULT_SCRIM_ALPHA,
                    paletteIsDark = palette.isDark
                )
            )
        }
    }

    @Test
    fun everyPaletteHasSomeReadableAlpha() {
        ReaderPalettes.all.forEach { palette ->
            assertNotNull(
                "${palette.id} has no readable scrim alpha at all",
                ReaderBackgroundImage.minimumReadableAlpha(
                    textArgb = palette.textArgb,
                    scrimArgb = palette.backgroundArgb,
                    paletteIsDark = palette.isDark
                )
            )
        }
    }

    @Test
    fun minimumReadableAlphaNeverDropsBelowTheFloor() {
        ReaderPalettes.all.forEach { palette ->
            val minimum = requireNotNull(
                ReaderBackgroundImage.minimumReadableAlpha(
                    textArgb = palette.textArgb,
                    scrimArgb = palette.backgroundArgb,
                    paletteIsDark = palette.isDark
                )
            ) { "${palette.id} returned null — no alpha satisfies the contrast requirement" }
            assertTrue(
                "${palette.id} reported $minimum which is below the floor",
                minimum >= ReaderBackgroundImage.MIN_SCRIM_ALPHA
            )
        }
    }

    /**
     * The floor is a UI range bound, not a safety guarantee — two palettes need 0.8. Pinning the
     * two worst cases keeps that honest: if someone lowers [ReaderBackgroundImage.MIN_SCRIM_ALPHA]
     * expecting it to be universally safe, this test says otherwise.
     */
    @Test
    fun theFloorAloneIsNotSafeForEveryPalette() {
        val strictest = listOf(ReaderPalettes.SolarizedDark, ReaderPalettes.Nord)
        strictest.forEach { palette ->
            assertFalse(
                "${palette.id} unexpectedly passes at the floor; MIN_SCRIM_ALPHA may have changed",
                ReaderBackgroundImage.bodyTextStaysReadable(
                    textArgb = palette.textArgb,
                    scrimArgb = palette.backgroundArgb,
                    scrimAlpha = ReaderBackgroundImage.MIN_SCRIM_ALPHA,
                    paletteIsDark = palette.isDark
                )
            )
            val minimum = requireNotNull(
                ReaderBackgroundImage.minimumReadableAlpha(
                    textArgb = palette.textArgb,
                    scrimArgb = palette.backgroundArgb,
                    paletteIsDark = palette.isDark
                )
            ) { "${palette.id} returned null — no alpha satisfies the contrast requirement" }
            assertTrue(
                "${palette.id} should need more than the floor, reported $minimum",
                minimum > ReaderBackgroundImage.MIN_SCRIM_ALPHA
            )
        }
    }

    /** A light palette's worst case is a *white* image, not black; verify that direction too. */
    @Test
    fun lightPaletteStaysReadableAgainstAWhiteImageAtTheDefault() {
        val palette = ReaderPalettes.Paper
        val effective = ReaderBackgroundImage.effectiveBackgroundArgb(
            scrimArgb = palette.backgroundArgb,
            worstCaseUnderlyingArgb = 0xFFFFFFFFL,
            scrimAlpha = ReaderBackgroundImage.DEFAULT_SCRIM_ALPHA
        )
        val ratio = ColorContrast.ratio(palette.textArgb, effective)
        assertTrue("expected the white worst case to stay readable, got $ratio", ratio >= ColorContrast.AA_BODY_TEXT)
    }

    @Test
    fun sampleSizeIsOneForImagesAlreadySmallEnough() {
        assertEquals(1, ReaderBackgroundImage.sampleSizeFor(1080, 1920))
        assertEquals(1, ReaderBackgroundImage.sampleSizeFor(ReaderBackgroundImage.MAX_EDGE_PX, 100))
    }

    @Test
    fun sampleSizeHalvesUntilTheLongestEdgeFits() {
        assertEquals(2, ReaderBackgroundImage.sampleSizeFor(4096, 2048))
        assertEquals(4, ReaderBackgroundImage.sampleSizeFor(8192, 100))
        val sample = ReaderBackgroundImage.sampleSizeFor(12000, 9000)
        assertTrue("sample size must be a power of two, was $sample", sample > 0 && (sample and (sample - 1)) == 0)
        assertTrue(12000 / sample <= ReaderBackgroundImage.MAX_EDGE_PX)
    }

    /** A failed bounds read gives 0; returning 0 would be silently reinterpreted as 1 downstream. */
    @Test
    fun sampleSizeRejectsNonPositiveDimensions() {
        assertEquals(1, ReaderBackgroundImage.sampleSizeFor(0, 0))
        assertEquals(1, ReaderBackgroundImage.sampleSizeFor(-100, 500))
    }

    @Test
    fun importLimitMatchesTheParsersSingleImageCap() {
        assertEquals(24L * 1024 * 1024, ReaderBackgroundImage.MAX_IMAGE_BYTES)
    }
}
