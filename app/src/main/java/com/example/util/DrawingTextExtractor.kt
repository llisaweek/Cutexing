package com.example.util

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Base64
import android.util.Log
import com.example.ui.components.CanvasBounds
import com.example.ui.components.DrawingStroke
import com.example.ui.components.DrawingTool
import com.example.ui.components.exportStrokesToBitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object DrawingTextExtractor {

  private const val TAG = "DrawingTextExtractor"

  /**
   * Transcribes drawn strokes, symbols, numbers, and formulas into text.
   * Uses Gemini via Firebase AI Logic if available, with on-device stroke fallback.
   */
  suspend fun extractTextFromStrokes(
    strokes: List<DrawingStroke>,
    bounds: CanvasBounds,
  ): String = withContext(Dispatchers.IO) {
    if (strokes.isEmpty()) {
      return@withContext ""
    }

    // 1. Generate high-contrast monochrome image for OCR / AI vision
    val bitmap = exportStrokesToBitmap(
      strokes = strokes,
      canvasLeft = bounds.left,
      canvasTop = bounds.top,
      canvasRight = bounds.right,
      canvasBottom = bounds.bottom,
      backgroundColor = AndroidColor.WHITE,
    )

    // 2. Try Firebase AI Logic (Gemini multimodal)
    try {
      val model = Firebase.ai.generativeModel(modelName = "gemini-2.5-flash")
      val stream = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
      val imageBytes = stream.toByteArray()

      val prompt = content {
        image(bitmap)
        text(
          "Analyze the handwritten content in this drawing. It contains user-drawn text, numbers, symbols, or formulas.\n" +
            "- Transcribe all words, digits, and mathematical/scientific formulas accurately.\n" +
            "- If there are formulas or equations, format them cleanly (e.g. 'E = mc²', '√x', 'a² + b² = c²', '∫ f(x) dx', 'πr²').\n" +
            "- Support English, Chinese (中文), Japanese (日本語), and international symbols.\n" +
            "- Output ONLY the extracted text/formulas with no conversational preamble."
        )
      }

      val response = model.generateContent(prompt)
      val resultText = response.text?.trim()
      if (!resultText.isNullOrBlank()) {
        return@withContext resultText
      }
    } catch (e: Exception) {
      Log.w(TAG, "Firebase AI multimodal extraction unavailable, using smart offline stroke analyzer: ${e.message}")
    }

    // 3. Robust Offline Fallback: Analyze stroke geometries, shapes, and positions
    return@withContext analyzeStrokesOffline(strokes)
  }

  /**
   * Offline heuristic recognition for basic symbols, numbers, equations, and shapes.
   */
  private fun analyzeStrokesOffline(strokes: List<DrawingStroke>): String {
    val nonEraserStrokes = strokes.filter { it.tool != DrawingTool.ERASER && it.points.isNotEmpty() }
    if (nonEraserStrokes.isEmpty()) return ""

    // Group strokes into approximate bounding boxes sorted left-to-right, top-to-bottom
    val strokeBoxes = nonEraserStrokes.map { stroke ->
      val xs = stroke.points.map { it.x }
      val ys = stroke.points.map { it.y }
      val minX = xs.minOrNull() ?: 0f
      val maxX = xs.maxOrNull() ?: 0f
      val minY = ys.minOrNull() ?: 0f
      val maxY = ys.maxOrNull() ?: 0f
      val width = maxX - minX
      val height = maxY - minY
      StrokeBox(stroke, minX, minY, maxX, maxY, width, height)
    }.sortedWith(compareBy({ (it.minY / 60f).toInt() }, { it.minX }))

    val sb = StringBuilder()
    for (box in strokeBoxes) {
      val recognized = recognizeSingleStroke(box)
      if (recognized.isNotEmpty()) {
        if (sb.isNotEmpty() && !sb.endsWith(" ") && !sb.endsWith("\n")) {
          sb.append(" ")
        }
        sb.append(recognized)
      }
    }

    val extracted = sb.toString().trim()
    return if (extracted.isNotBlank()) extracted else "Handwritten Note [${strokes.size} strokes]"
  }

  private fun recognizeSingleStroke(box: StrokeBox): String {
    val stroke = box.stroke
    when (stroke.tool) {
      DrawingTool.SHAPE_LINE -> {
        return if (box.width > box.height * 2.5f) "—" else "|"
      }
      DrawingTool.SHAPE_ARROW -> return "→"
      DrawingTool.SHAPE_RECTANGLE -> return "▭"
      DrawingTool.SHAPE_CIRCLE -> return "◯"
      else -> {
        val pts = stroke.points
        if (pts.size <= 2) return "."

        val isHorizontal = box.width > box.height * 2.8f
        val isVertical = box.height > box.width * 2.8f

        if (isHorizontal) return "-"
        if (isVertical) return "1"

        // Check if closed loop (approx 0 or O)
        val start = pts.first()
        val end = pts.last()
        val distStartEnd = kotlin.math.hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble()).toFloat()
        val diagonal = kotlin.math.hypot(box.width.toDouble(), box.height.toDouble()).toFloat()

        if (distStartEnd < diagonal * 0.25f && pts.size > 8) {
          return "0"
        }

        // Check if plus sign (+) or cross (x)
        if (abs(box.width - box.height) < box.width * 0.35f && pts.size > 4) {
          return "+"
        }

        return "~"
      }
    }
  }

  private data class StrokeBox(
    val stroke: DrawingStroke,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val width: Float,
    val height: Float,
  )
}
