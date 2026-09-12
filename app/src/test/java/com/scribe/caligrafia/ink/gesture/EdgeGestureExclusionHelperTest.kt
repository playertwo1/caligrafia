package com.scribe.caligrafia.ink.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeGestureExclusionHelperTest {

    @Test
    fun computeEdgeExclusionBounds_returns_left_and_right_bounds_for_valid_dimensions() {
        val width = 1440
        val height = 3120
        val density = 3.0f

        val bounds = EdgeGestureExclusionHelper.computeEdgeExclusionBounds(width, height, density)

        assertEquals(2, bounds.size)
        val left = bounds[0]
        val right = bounds[1]

        // 56 * 3.0 = 168 px
        assertEquals(0, left.left)
        assertEquals(0, left.top)
        assertEquals(168, left.right)
        assertEquals(3120, left.bottom)

        assertEquals(1440 - 168, right.left)
        assertEquals(0, right.top)
        assertEquals(1440, right.right)
        assertEquals(3120, right.bottom)
    }

    @Test
    fun computeEdgeExclusionBounds_returns_empty_when_dimensions_are_zero_or_negative() {
        assertTrue(EdgeGestureExclusionHelper.computeEdgeExclusionBounds(0, 1000, 2.0f).isEmpty())
        assertTrue(EdgeGestureExclusionHelper.computeEdgeExclusionBounds(1000, 0, 2.0f).isEmpty())
        assertTrue(EdgeGestureExclusionHelper.computeEdgeExclusionBounds(-10, -20, 2.0f).isEmpty())
    }

    @Test
    fun computeEdgeExclusionBounds_enforces_minimum_and_maximum_width_bounds() {
        // Teste de largura mínima com densidade muito baixa (1.0f -> 56px < min 120px)
        val boundsLowDensity = EdgeGestureExclusionHelper.computeEdgeExclusionBounds(1000, 2000, 1.0f)
        assertEquals(120, boundsLowDensity[0].right)

        // Teste de contenção para tela estreita (width / 3)
        val boundsNarrow = EdgeGestureExclusionHelper.computeEdgeExclusionBounds(300, 600, 3.0f)
        // 300 / 3 = 100
        assertEquals(100, boundsNarrow[0].right)
        assertEquals(200, boundsNarrow[1].left)
    }
}
