package com.example

import com.example.ui.components.CanvasBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `canvas bounds computes correct dimensions`() {
    val bounds = CanvasBounds(left = 0f, top = 0f, right = 800f, bottom = 1200f)
    assertEquals(800f, bounds.width)
    assertEquals(1200f, bounds.height)
  }

  @Test
  fun `canvas bounds expands toward drawing coordinates`() {
    var bounds = CanvasBounds(left = 0f, top = 0f, right = 800f, bottom = 1000f)
    val margin = 70f
    val step = 180f

    // Simulate drawing near right edge: point x = 760f
    val drawPointX = 760f
    val shouldExpandRight = drawPointX >= bounds.right - margin
    assertTrue(shouldExpandRight)

    bounds = bounds.copy(right = max(bounds.right, drawPointX + step))
    assertEquals(940f, bounds.right)
    assertTrue(bounds.width > 800f)

    // Simulate drawing near bottom edge: point y = 980f
    val drawPointY = 980f
    val shouldExpandBottom = drawPointY >= bounds.bottom - margin
    assertTrue(shouldExpandBottom)

    bounds = bounds.copy(bottom = max(bounds.bottom, drawPointY + step))
    assertEquals(1160f, bounds.bottom)
    assertTrue(bounds.height > 1000f)
  }
}

