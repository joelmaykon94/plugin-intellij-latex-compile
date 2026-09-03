package com.github.joelmaykon94.latex.editor

/**
 * Coordinate and calculate synchronized scroll positions between the LaTeX editor
 * and the PDF preview panel.
 */
object LatexScrollSyncCoordinator {

    /**
     * Calculates the normalized scroll ratio [0.0, 1.0] from editor vertical scroll offset.
     */
    fun calculateRatioFromScroll(
        scrollY: Int,
        contentHeight: Int,
        viewportHeight: Int
    ): Double {
        val scrollableHeight = contentHeight - viewportHeight
        if (scrollableHeight <= 0) return 0.0
        val rawRatio = scrollY.toDouble() / scrollableHeight
        return rawRatio.coerceIn(0.0, 1.0)
    }

    /**
     * Calculates the normalized scroll ratio [0.0, 1.0] from line numbers.
     */
    fun calculateRatioFromLine(
        currentLine: Int,
        totalLines: Int
    ): Double {
        if (totalLines <= 1) return 0.0
        val clampedLine = currentLine.coerceIn(1, totalLines)
        return ((clampedLine - 1).toDouble() / (totalLines - 1)).coerceIn(0.0, 1.0)
    }

    /**
     * Calculates the target pixel scroll offset within a scrollable container given a ratio.
     */
    fun calculateTargetScrollY(
        ratio: Double,
        scrollHeight: Int,
        viewportHeight: Int
    ): Int {
        val maxScroll = (scrollHeight - viewportHeight).coerceAtLeast(0)
        return (ratio.coerceIn(0.0, 1.0) * maxScroll).toInt()
    }
}
