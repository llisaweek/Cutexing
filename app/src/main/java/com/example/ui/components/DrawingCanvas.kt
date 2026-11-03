package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CutestarPrimary

data class DrawingStroke(
  val points: List<Offset>,
  val color: Color,
  val strokeWidth: Float,
)

@Composable
fun DrawingCanvas(
  strokes: List<DrawingStroke>,
  onStrokesChange: (List<DrawingStroke>) -> Unit,
  modifier: Modifier = Modifier,
  canvasBackgroundColor: Color = Color.White,
) {
  var selectedColor by remember { mutableStateOf(Color(0xFF202124)) }
  var selectedStrokeWidth by remember { mutableFloatStateOf(8f) }
  val currentStrokePoints = remember { mutableStateListOf<Offset>() }

  val paletteColors = listOf(
    Color(0xFF202124), // Charcoal
    CutestarPrimary, // Rose pink
    Color(0xFF8B5CF6), // Lavender
    Color(0xFF0EA5E9), // Sky blue
    Color(0xFF10B981), // Emerald mint
    Color(0xFFF59E0B), // Amber yellow
    canvasBackgroundColor, // Eraser
  )

  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    // Canvas Surface
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(260.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        .background(canvasBackgroundColor)
        .testTag("drawing_canvas_surface"),
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(selectedColor, selectedStrokeWidth) {
            detectDragGestures(
              onDragStart = { offset ->
                currentStrokePoints.clear()
                currentStrokePoints.add(offset)
              },
              onDrag = { change, _ ->
                change.consume()
                currentStrokePoints.add(change.position)
              },
              onDragEnd = {
                if (currentStrokePoints.isNotEmpty()) {
                  val newStroke = DrawingStroke(
                    points = currentStrokePoints.toList(),
                    color = selectedColor,
                    strokeWidth = selectedStrokeWidth,
                  )
                  onStrokesChange(strokes + newStroke)
                  currentStrokePoints.clear()
                }
              },
              onDragCancel = {
                currentStrokePoints.clear()
              },
            )
          },
      ) {
        // Draw completed strokes
        strokes.forEach { stroke ->
          if (stroke.points.size > 1) {
            val path = Path().apply {
              moveTo(stroke.points.first().x, stroke.points.first().y)
              for (i in 1 until stroke.points.size) {
                lineTo(stroke.points[i].x, stroke.points[i].y)
              }
            }
            drawPath(
              path = path,
              color = stroke.color,
              style = Stroke(
                width = stroke.strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
              ),
            )
          } else if (stroke.points.size == 1) {
            drawCircle(
              color = stroke.color,
              radius = stroke.strokeWidth / 2f,
              center = stroke.points.first(),
            )
          }
        }

        // Draw current in-progress stroke
        if (currentStrokePoints.size > 1) {
          val activePath = Path().apply {
            moveTo(currentStrokePoints.first().x, currentStrokePoints.first().y)
            for (i in 1 until currentStrokePoints.size) {
              lineTo(currentStrokePoints[i].x, currentStrokePoints[i].y)
            }
          }
          drawPath(
            path = activePath,
            color = selectedColor,
            style = Stroke(
              width = selectedStrokeWidth,
              cap = StrokeCap.Round,
              join = StrokeJoin.Round,
            ),
          )
        } else if (currentStrokePoints.size == 1) {
          drawCircle(
            color = selectedColor,
            radius = selectedStrokeWidth / 2f,
            center = currentStrokePoints.first(),
          )
        }
      }

      if (strokes.isEmpty() && currentStrokePoints.isEmpty()) {
        Text(
          text = "Tap and drag to sketch or draw here ✏️",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier
            .align(Alignment.Center)
            .padding(16.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Toolbar: Palette, Stroke Thickness, Undo, Clear
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Color swatches
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          paletteColors.forEach { color ->
            val isEraser = color == canvasBackgroundColor
            val isSelected = selectedColor == color
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                  width = if (isSelected) 2.5.dp else 1.dp,
                  color = if (isSelected) CutestarPrimary else Color(0xFFCCCCCC),
                  shape = CircleShape,
                )
                .clickable { selectedColor = color },
              contentAlignment = Alignment.Center,
            ) {
              if (isEraser) {
                Text("🧹", fontSize = 10.sp)
              }
            }
          }
        }

        // Action controls: Width, Undo, Clear
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          // Thickness toggles
          listOf(4f to "Thin", 8f to "Med", 16f to "Thick").forEach { (width, label) ->
            val isSelected = selectedStrokeWidth == width
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) CutestarPrimary.copy(alpha = 0.2f) else Color.Transparent)
                .clickable { selectedStrokeWidth = width }
                .padding(horizontal = 6.dp, vertical = 4.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                ),
                color = if (isSelected) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Undo
          IconButton(
            onClick = {
              if (strokes.isNotEmpty()) {
                onStrokesChange(strokes.dropLast(1))
              }
            },
            enabled = strokes.isNotEmpty(),
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Undo,
              contentDescription = "Undo stroke",
              modifier = Modifier.size(16.dp),
              tint = if (strokes.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
          }

          // Clear
          IconButton(
            onClick = { onStrokesChange(emptyList()) },
            enabled = strokes.isNotEmpty(),
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Delete,
              contentDescription = "Clear canvas",
              modifier = Modifier.size(16.dp),
              tint = if (strokes.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
            )
          }
        }
      }
    }
  }
}

/**
 * Client-side Canvas Image Exporter
 * Renders vector drawing strokes onto an offscreen Bitmap and returns it.
 */
fun exportStrokesToBitmap(
  strokes: List<DrawingStroke>,
  width: Int = 800,
  height: Int = 600,
  backgroundColor: Int = android.graphics.Color.WHITE,
): Bitmap {
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = AndroidCanvas(bitmap)
  canvas.drawColor(backgroundColor)

  strokes.forEach { stroke ->
    if (stroke.points.size > 1) {
      val paint = AndroidPaint().apply {
        isAntiAlias = true
        color = stroke.color.toArgb()
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
        strokeWidth = stroke.strokeWidth * (width.toFloat() / 400f) // Scale for resolution
      }

      val path = AndroidPath()
      path.moveTo(stroke.points.first().x * (width.toFloat() / 400f), stroke.points.first().y * (height.toFloat() / 260f))
      for (i in 1 until stroke.points.size) {
        val pt = stroke.points[i]
        path.lineTo(pt.x * (width.toFloat() / 400f), pt.y * (height.toFloat() / 260f))
      }
      canvas.drawPath(path, paint)
    } else if (stroke.points.size == 1) {
      val paint = AndroidPaint().apply {
        isAntiAlias = true
        color = stroke.color.toArgb()
        style = AndroidPaint.Style.FILL
      }
      val pt = stroke.points.first()
      canvas.drawCircle(
        pt.x * (width.toFloat() / 400f),
        pt.y * (height.toFloat() / 260f),
        (stroke.strokeWidth / 2f) * (width.toFloat() / 400f),
        paint,
      )
    }
  }

  return bitmap
}
