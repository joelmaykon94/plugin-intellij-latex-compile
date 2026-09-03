package com.github.joelmaykon94.latex.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LatexScrollSyncCoordinatorTest {

    @Test
    fun `test calculateRatioFromScroll normal values`() {
        // Scroll at top (0px)
        val ratioTop = LatexScrollSyncCoordinator.calculateRatioFromScroll(
            scrollY = 0,
            contentHeight = 1000,
            viewportHeight = 200
        )
        assertEquals(0.0, ratioTop, 0.0001)

        // Scroll at middle (400px of 800px scrollable)
        val ratioMid = LatexScrollSyncCoordinator.calculateRatioFromScroll(
            scrollY = 400,
            contentHeight = 1000,
            viewportHeight = 200
        )
        assertEquals(0.5, ratioMid, 0.0001)

        // Scroll at bottom (800px of 800px scrollable)
        val ratioBottom = LatexScrollSyncCoordinator.calculateRatioFromScroll(
            scrollY = 800,
            contentHeight = 1000,
            viewportHeight = 200
        )
        assertEquals(1.0, ratioBottom, 0.0001)
    }

    @Test
    fun `test calculateRatioFromScroll edge cases`() {
        // Content fits in viewport (no scrollable area)
        val ratioNoScroll = LatexScrollSyncCoordinator.calculateRatioFromScroll(
            scrollY = 0,
            contentHeight = 300,
            viewportHeight = 500
        )
        assertEquals(0.0, ratioNoScroll, 0.0001)

        // Negative scroll (overscroll on bounce)
        val ratioNegative = LatexScrollSyncCoordinator.calculateRatioFromScroll(
            scrollY = -50,
            contentHeight = 1000,
            viewportHeight = 200
        )
        assertEquals(0.0, ratioNegative, 0.0001)

        // Beyond bottom (overscroll)
        val ratioBeyond = LatexScrollSyncCoordinator.calculateRatioFromScroll(
            scrollY = 1200,
            contentHeight = 1000,
            viewportHeight = 200
        )
        assertEquals(1.0, ratioBeyond, 0.0001)
    }

    @Test
    fun `test calculateRatioFromLine`() {
        // First line
        assertEquals(0.0, LatexScrollSyncCoordinator.calculateRatioFromLine(1, 100), 0.0001)

        // Last line
        assertEquals(1.0, LatexScrollSyncCoordinator.calculateRatioFromLine(100, 100), 0.0001)

        // Middle line (50 of 99 intervals)
        val ratio = LatexScrollSyncCoordinator.calculateRatioFromLine(50, 99)
        assertTrue(ratio in 0.49..0.51)

        // Single line document
        assertEquals(0.0, LatexScrollSyncCoordinator.calculateRatioFromLine(1, 1), 0.0001)

        // Out of bounds line
        assertEquals(0.0, LatexScrollSyncCoordinator.calculateRatioFromLine(-5, 50), 0.0001)
        assertEquals(1.0, LatexScrollSyncCoordinator.calculateRatioFromLine(200, 50), 0.0001)
    }

    @Test
    fun `test calculateTargetScrollY`() {
        val scrollHeight = 2000
        val viewportHeight = 500
        // maxScroll is 1500

        assertEquals(0, LatexScrollSyncCoordinator.calculateTargetScrollY(0.0, scrollHeight, viewportHeight))
        assertEquals(750, LatexScrollSyncCoordinator.calculateTargetScrollY(0.5, scrollHeight, viewportHeight))
        assertEquals(1500, LatexScrollSyncCoordinator.calculateTargetScrollY(1.0, scrollHeight, viewportHeight))

        // Ratio clamped
        assertEquals(0, LatexScrollSyncCoordinator.calculateTargetScrollY(-0.5, scrollHeight, viewportHeight))
        assertEquals(1500, LatexScrollSyncCoordinator.calculateTargetScrollY(1.5, scrollHeight, viewportHeight))
    }
}
