package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.ShapeLine
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CutestarPrimary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class DrawingTool(val displayName: String) {
  PEN("Pen"),
  HIGHLIGHTER("Highlighter"),
  MARKER("Marker"),
  SHAPE_LINE("Line"),
  SHAPE_ARROW("Arrow"),
  SHAPE_RECTANGLE("Rectangle"),
  SHAPE_CIRCLE("Circle"),
  ERASER("Eraser"),
}

enum class PaperStyle(val displayName: String) {
  BLANK("Blank"),
  RULED("Lined"),
  GRID("Grid"),
  DOTTED("Dotted"),
}

data class DrawingStroke(
  val points: List<Offset>,
  val color: Color,
  val strokeWidth: Float,
  val tool: DrawingTool = DrawingTool.PEN,
  val alpha: Float = 1.0f,
)

data class CanvasBounds(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float,
) {
  val width: Float get() = max(100f, right - left)
  val height: Float get() = max(100f, bottom - top)
}

/**
 * Dedicated image-drawing canvas.
 * - Single-finger drawing with automatic dynamic expansion when available drawing area becomes insufficient.
 * - Expansion ceases immediately once the user stops drawing.
 * - Two-finger pan: the entire image moves in the direction of the finger movement.
 * - Practical features: Multiple tools (Pen, Highlighter, Marker, Shapes, Eraser), Paper styles (Blank, Lined, Grid, Dotted), and rich palette.
 */
@Composable
fun DrawingCanvas(
  strokes: List<DrawingStroke>,
  onStrokesChange: (List<DrawingStroke>) -> Unit,
  modifier: Modifier = Modifier,
  canvasBackgroundColor: Color = Color.White,
  initialBounds: CanvasBounds? = null,
  onBoundsChange: ((CanvasBounds) -> Unit)? = null,
  paperStyle: PaperStyle = PaperStyle.BLANK,
  onPaperStyleChange: ((PaperStyle) -> Unit)? = null,
) {
  var currentTool by remember { mutableStateOf(DrawingTool.PEN) }
  var currentPaperStyle by remember { mutableStateOf(paperStyle) }
  var selectedColor by remember { mutableStateOf(Color(0xFF202124)) }
  var selectedStrokeWidth by remember { mutableFloatStateOf(8f) }
  var showShapesMenu by remember { mutableStateOf(false) }

  val currentStrokePoints = remember { mutableStateListOf<Offset>() }
  val redoStack = remember { mutableStateListOf<DrawingStroke>() }

  // Canvas translation (two-finger pan offset)
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  // Canvas bounds (auto-expanding drawing area)
  var canvasLeft by remember { mutableFloatStateOf(initialBounds?.left ?: 0f) }
  var canvasTop by remember { mutableFloatStateOf(initialBounds?.top ?: 0f) }
  var canvasRight by remember { mutableFloatStateOf(initialBounds?.right ?: 900f) }
  var canvasBottom by remember { mutableFloatStateOf(initialBounds?.bottom ?: 1200f) }

  // Status indicators
  var isActivelyDrawing by remember { mutableStateOf(false) }
  var isPanningActive by remember { mutableStateOf(false) }
  var hasInitializedBounds by remember { mutableStateOf(initialBounds != null) }

  val paletteColors = listOf(
    Color(0xFF202124), // Charcoal
    CutestarPrimary,   // Rose Pink
    Color(0xFF9C27B0), // Vivid Purple
    Color(0xFF1E88E5), // Vivid Blue
    Color(0xFF00897B), // Vivid Teal
    Color(0xFF43A047), // Vivid Green
    Color(0xFFFB8C00), // Vivid Amber
    Color(0xFFE53935), // Vivid Crimson
    Color(0xFF8D6E63), // Warm Clay
    Color(0xFF78909C), // Slate
  )

  fun notifyBoundsChanged() {
    onBoundsChange?.invoke(CanvasBounds(canvasLeft, canvasTop, canvasRight, canvasBottom))
  }

  fun commitActiveStroke() {
    if (currentStrokePoints.isNotEmpty()) {
      val effectiveColor = if (currentTool == DrawingTool.ERASER) canvasBackgroundColor else selectedColor
      val effectiveAlpha = if (currentTool == DrawingTool.HIGHLIGHTER) 0.38f else 1.0f
      val effectiveWidth = when (currentTool) {
        DrawingTool.HIGHLIGHTER -> selectedStrokeWidth * 2.5f
        DrawingTool.MARKER -> selectedStrokeWidth * 1.5f
        DrawingTool.ERASER -> selectedStrokeWidth * 2.0f
        else -> selectedStrokeWidth
      }

      val newStroke = DrawingStroke(
        points = currentStrokePoints.toList(),
        color = effectiveColor,
        strokeWidth = effectiveWidth,
        tool = currentTool,
        alpha = effectiveAlpha,
      )
      onStrokesChange(strokes + newStroke)
      redoStack.clear()
      currentStrokePoints.clear()
    }
  }

  Column(
    modifier = modifier.fillMaxSize(),
  ) {
    // Canvas Viewport Area
    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clipToBounds()
        .background(Color(0xFFF0F2F5)) // Modern subtle drafting background
        .testTag("drawing_canvas_viewport"),
    ) {
      val viewportWidth = maxWidth.value * 2.5f
      val viewportHeight = maxHeight.value * 2.5f

      LaunchedEffect(maxWidth, maxHeight) {
        if (!hasInitializedBounds && maxWidth.value > 0f) {
          canvasRight = max(canvasRight, viewportWidth)
          canvasBottom = max(canvasBottom, viewportHeight)
          hasInitializedBounds = true
          notifyBoundsChanged()
        }
      }

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(currentTool, selectedColor, selectedStrokeWidth) {
            awaitPointerEventScope {
              var isPanning = false

              while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val pressedPointers = event.changes.filter { it.pressed }
                val pressedCount = pressedPointers.size

                if (pressedCount >= 2) {
                  // TWO-FINGER PAN
                  isPanning = true

                  if (isActivelyDrawing) {
                    commitActiveStroke()
                    isActivelyDrawing = false
                  }
                  isPanningActive = true

                  val p1 = pressedPointers[0]
                  val p2 = pressedPointers[1]
                  val prevCentroid = (p1.previousPosition + p2.previousPosition) / 2f
                  val currCentroid = (p1.position + p2.position) / 2f
                  val panDelta = currCentroid - prevCentroid

                  panOffset += panDelta
                  p1.consume()
                  p2.consume()
                } else if (pressedCount == 1) {
                  if (isPanning) {
                    pressedPointers[0].consume()
                  } else {
                    val change = pressedPointers[0]
                    val screenPos = change.position
                    val canvasPos = screenPos - panOffset

                    if (!isActivelyDrawing) {
                      isActivelyDrawing = true
                      isPanningActive = false
                      currentStrokePoints.clear()
                      currentStrokePoints.add(canvasPos)
                      change.consume()
                    } else {
                      if (change.positionChanged()) {
                        currentStrokePoints.add(canvasPos)

                        // AUTOMATIC CANVAS EXPANSION TOWARD CURRENT DRAWING AREA:
                        val edgeThreshold = 70f
                        val expansionStep = 180f
                        var didExpand = false

                        if (canvasPos.x >= canvasRight - edgeThreshold) {
                          canvasRight = max(canvasRight, canvasPos.x + expansionStep)
                          didExpand = true
                        }
                        if (canvasPos.x <= canvasLeft + edgeThreshold) {
                          canvasLeft = min(canvasLeft, canvasPos.x - expansionStep)
                          didExpand = true
                        }
                        if (canvasPos.y >= canvasBottom - edgeThreshold) {
                          canvasBottom = max(canvasBottom, canvasPos.y + expansionStep)
                          didExpand = true
                        }
                        if (canvasPos.y <= canvasTop + edgeThreshold) {
                          canvasTop = min(canvasTop, canvasPos.y - expansionStep)
                          didExpand = true
                        }

                        if (didExpand) {
                          notifyBoundsChanged()
                        }

                        change.consume()
                      }
                    }
                  }
                } else {
                  // pressedCount == 0: fingers lifted!
                  // Expansion ceases immediately once the user stops drawing
                  isPanning = false
                  isPanningActive = false

                  if (isActivelyDrawing) {
                    commitActiveStroke()
                    isActivelyDrawing = false
                    notifyBoundsChanged()
                  }
                }
              }
            }
          },
      ) {
        // 1. Draw Paper Sheet Area
        val sheetLeft = canvasLeft + panOffset.x
        val sheetTop = canvasTop + panOffset.y
        val sheetWidth = max(100f, canvasRight - canvasLeft)
        val sheetHeight = max(100f, canvasBottom - canvasTop)

        // Drop shadow
        drawRoundRect(
          color = Color(0x18000000),
          topLeft = Offset(sheetLeft - 4f, sheetTop - 4f),
          size = Size(sheetWidth + 8f, sheetHeight + 8f),
          cornerRadius = CornerRadius(14f, 14f),
        )

        // Paper base
        drawRoundRect(
          color = canvasBackgroundColor,
          topLeft = Offset(sheetLeft, sheetTop),
          size = Size(sheetWidth, sheetHeight),
          cornerRadius = CornerRadius(14f, 14f),
        )

        // Draw Paper Texture / Guidelines (Ruled, Grid, Dotted)
        when (currentPaperStyle) {
          PaperStyle.RULED -> {
            val lineSpacing = 44f
            var y = sheetTop + lineSpacing
            while (y < sheetTop + sheetHeight) {
              drawLine(
                color = Color(0x221E88E5),
                start = Offset(sheetLeft, y),
                end = Offset(sheetLeft + sheetWidth, y),
                strokeWidth = 1f,
              )
              y += lineSpacing
            }
          }
          PaperStyle.GRID -> {
            val gridSpacing = 40f
            var x = sheetLeft + gridSpacing
            while (x < sheetLeft + sheetWidth) {
              drawLine(
                color = Color(0x1A000000),
                start = Offset(x, sheetTop),
                end = Offset(x, sheetTop + sheetHeight),
                strokeWidth = 1f,
              )
              x += gridSpacing
            }
            var y = sheetTop + gridSpacing
            while (y < sheetTop + sheetHeight) {
              drawLine(
                color = Color(0x1A000000),
                start = Offset(sheetLeft, y),
                end = Offset(sheetLeft + sheetWidth, y),
                strokeWidth = 1f,
              )
              y += gridSpacing
            }
          }
          PaperStyle.DOTTED -> {
            val dotSpacing = 36f
            var y = sheetTop + dotSpacing
            while (y < sheetTop + sheetHeight) {
              var x = sheetLeft + dotSpacing
              while (x < sheetLeft + sheetWidth) {
                drawCircle(
                  color = Color(0x35000000),
                  radius = 1.5f,
                  center = Offset(x, y),
                )
                x += dotSpacing
              }
              y += dotSpacing
            }
          }
          PaperStyle.BLANK -> { /* Clean plain paper */ }
        }

        // Clean border
        drawRoundRect(
          color = Color(0x20000000),
          topLeft = Offset(sheetLeft, sheetTop),
          size = Size(sheetWidth, sheetHeight),
          cornerRadius = CornerRadius(14f, 14f),
          style = Stroke(width = 1.5f),
        )

        // 2. Render Completed Vector Drawing Strokes
        withTransform({
          translate(panOffset.x, panOffset.y)
        }) {
          strokes.forEach { stroke ->
            renderStroke(stroke)
          }

          // Render active stroke currently being drawn
          if (currentStrokePoints.isNotEmpty()) {
            val effectiveColor = if (currentTool == DrawingTool.ERASER) canvasBackgroundColor else selectedColor
            val effectiveAlpha = if (currentTool == DrawingTool.HIGHLIGHTER) 0.38f else 1.0f
            val effectiveWidth = when (currentTool) {
              DrawingTool.HIGHLIGHTER -> selectedStrokeWidth * 2.5f
              DrawingTool.MARKER -> selectedStrokeWidth * 1.5f
              DrawingTool.ERASER -> selectedStrokeWidth * 2.0f
              else -> selectedStrokeWidth
            }
            val activeStroke = DrawingStroke(
              points = currentStrokePoints.toList(),
              color = effectiveColor,
              strokeWidth = effectiveWidth,
              tool = currentTool,
              alpha = effectiveAlpha,
            )
            renderStroke(activeStroke)
          }
        }
      }

      // Gesture feedback overlay badge
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 10.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = when {
              isPanningActive -> Icons.Outlined.PanTool
              currentTool == DrawingTool.HIGHLIGHTER -> Icons.Outlined.Highlight
              currentTool == DrawingTool.MARKER -> Icons.Outlined.Brush
              currentTool == DrawingTool.ERASER -> Icons.Outlined.Delete
              currentTool.name.startsWith("SHAPE") -> Icons.Outlined.ShapeLine
              else -> Icons.Outlined.Edit
            },
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isPanningActive || isActivelyDrawing) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = if (isPanningActive) {
              "Panning image with two fingers"
            } else if (isActivelyDrawing) {
              "${currentTool.displayName} • Auto-expanding canvas"
            } else {
              "Draw with 1 finger • Pan with 2 fingers"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = if (isPanningActive || isActivelyDrawing) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Re-center View Button (Float on Canvas)
      IconButton(
        onClick = { panOffset = Offset.Zero },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(16.dp)
          .size(42.dp)
          .shadow(3.dp, CircleShape)
          .background(MaterialTheme.colorScheme.surface, CircleShape),
      ) {
        Icon(
          imageVector = Icons.Outlined.CenterFocusStrong,
          contentDescription = "Center canvas view",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    // Bottom Drawing Toolbar: Tools, Paper Styles, Colors, Stroke Widths, History
    Surface(
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        // Row 1: Tool Selection Tabs (Pen, Highlighter, Marker, Shapes, Eraser) + Paper Style Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Tool Buttons
          ToolButton(
            icon = Icons.Outlined.Edit,
            label = "Pen",
            isSelected = currentTool == DrawingTool.PEN,
            onClick = { currentTool = DrawingTool.PEN },
          )

          ToolButton(
            icon = Icons.Outlined.Highlight,
            label = "Highlighter",
            isSelected = currentTool == DrawingTool.HIGHLIGHTER,
            onClick = { currentTool = DrawingTool.HIGHLIGHTER },
          )

          ToolButton(
            icon = Icons.Outlined.Brush,
            label = "Marker",
            isSelected = currentTool == DrawingTool.MARKER,
            onClick = { currentTool = DrawingTool.MARKER },
          )

          // Shapes Menu
          Box {
            ToolButton(
              icon = Icons.Outlined.ShapeLine,
              label = if (currentTool.name.startsWith("SHAPE")) currentTool.displayName else "Shapes",
              isSelected = currentTool.name.startsWith("SHAPE"),
              onClick = { showShapesMenu = !showShapesMenu },
            )
            DropdownMenu(
              expanded = showShapesMenu,
              onDismissRequest = { showShapesMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text("Straight Line") },
                onClick = {
                  currentTool = DrawingTool.SHAPE_LINE
                  showShapesMenu = false
                },
              )
              DropdownMenuItem(
                text = { Text("Arrow →") },
                onClick = {
                  currentTool = DrawingTool.SHAPE_ARROW
                  showShapesMenu = false
                },
              )
              DropdownMenuItem(
                text = { Text("Rectangle ▭") },
                onClick = {
                  currentTool = DrawingTool.SHAPE_RECTANGLE
                  showShapesMenu = false
                },
              )
              DropdownMenuItem(
                text = { Text("Circle / Oval ◯") },
                onClick = {
                  currentTool = DrawingTool.SHAPE_CIRCLE
                  showShapesMenu = false
                },
              )
            }
          }

          // Eraser
          ToolButton(
            icon = Icons.Outlined.Delete,
            label = "Eraser",
            isSelected = currentTool == DrawingTool.ERASER,
            onClick = { currentTool = DrawingTool.ERASER },
          )

          Spacer(modifier = Modifier.width(6.dp))

          // Paper style toggles
          PaperStyle.entries.forEach { style ->
            val isSelected = currentPaperStyle == style
            FilterChip(
              selected = isSelected,
              onClick = {
                currentPaperStyle = style
                onPaperStyleChange?.invoke(style)
              },
              label = { Text(style.displayName, fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CutestarPrimary.copy(alpha = 0.2f),
                selectedLabelColor = CutestarPrimary,
              ),
              modifier = Modifier.height(30.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Color Palette Swatches
        AnimatedVisibility(visible = currentTool != DrawingTool.ERASER) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            paletteColors.forEach { color ->
              val isSelected = selectedColor == color
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(CircleShape)
                  .background(color)
                  .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) CutestarPrimary else Color(0xFFDDDDDD),
                    shape = CircleShape,
                  )
                  .clickable { selectedColor = color },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: Thickness presets + Undo / Redo / Clear
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Thickness options
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            listOf(3f to "Fine", 7f to "Med", 14f to "Bold", 26f to "X-Bold").forEach { (width, label) ->
              val isSelected = selectedStrokeWidth == width
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) CutestarPrimary.copy(alpha = 0.18f) else Color.Transparent)
                  .clickable { selectedStrokeWidth = width }
                  .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  ),
                  color = if (isSelected) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // Undo, Redo, Clear
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            // Undo
            IconButton(
              onClick = {
                if (strokes.isNotEmpty()) {
                  val last = strokes.last()
                  redoStack.add(last)
                  onStrokesChange(strokes.dropLast(1))
                }
              },
              enabled = strokes.isNotEmpty(),
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Undo,
                contentDescription = "Undo",
                tint = if (strokes.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp),
              )
            }

            // Redo
            IconButton(
              onClick = {
                if (redoStack.isNotEmpty()) {
                  val strokeToRestore = redoStack.removeLast()
                  onStrokesChange(strokes + strokeToRestore)
                }
              },
              enabled = redoStack.isNotEmpty(),
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Redo,
                contentDescription = "Redo",
                tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp),
              )
            }

            // Clear
            IconButton(
              onClick = {
                if (strokes.isNotEmpty()) {
                  redoStack.clear()
                  onStrokesChange(emptyList())
                }
              },
              enabled = strokes.isNotEmpty(),
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Clear canvas",
                tint = if (strokes.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ToolButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) CutestarPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CutestarPrimary) else null,
    modifier = Modifier.clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        modifier = Modifier.size(15.dp),
        tint = if (isSelected) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 11.sp,
          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        ),
        color = if (isSelected) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun DrawScope.renderStroke(stroke: DrawingStroke) {
  val strokeColor = stroke.color.copy(alpha = stroke.alpha)
  val points = stroke.points

  when (stroke.tool) {
    DrawingTool.SHAPE_LINE -> {
      if (points.isNotEmpty()) {
        val start = points.first()
        val end = points.last()
        drawLine(
          color = strokeColor,
          start = start,
          end = end,
          strokeWidth = stroke.strokeWidth,
          cap = StrokeCap.Round,
        )
      }
    }
    DrawingTool.SHAPE_ARROW -> {
      if (points.isNotEmpty()) {
        val start = points.first()
        val end = points.last()
        drawLine(
          color = strokeColor,
          start = start,
          end = end,
          strokeWidth = stroke.strokeWidth,
          cap = StrokeCap.Round,
        )
        // Arrow head
        val angle = atan2(end.y - start.y, end.x - start.x)
        val arrowLength = max(20f, stroke.strokeWidth * 3f)
        val wingAngle = Math.PI / 6.0
        val x1 = end.x - arrowLength * cos(angle - wingAngle).toFloat()
        val y1 = end.y - arrowLength * sin(angle - wingAngle).toFloat()
        val x2 = end.x - arrowLength * cos(angle + wingAngle).toFloat()
        val y2 = end.y - arrowLength * sin(angle + wingAngle).toFloat()

        drawLine(color = strokeColor, start = end, end = Offset(x1, y1), strokeWidth = stroke.strokeWidth, cap = StrokeCap.Round)
        drawLine(color = strokeColor, start = end, end = Offset(x2, y2), strokeWidth = stroke.strokeWidth, cap = StrokeCap.Round)
      }
    }
    DrawingTool.SHAPE_RECTANGLE -> {
      if (points.isNotEmpty()) {
        val start = points.first()
        val end = points.last()
        val left = min(start.x, end.x)
        val top = min(start.y, end.y)
        val width = max(1f, kotlin.math.abs(end.x - start.x))
        val height = max(1f, kotlin.math.abs(end.y - start.y))
        drawRoundRect(
          color = strokeColor,
          topLeft = Offset(left, top),
          size = Size(width, height),
          cornerRadius = CornerRadius(8f, 8f),
          style = Stroke(width = stroke.strokeWidth),
        )
      }
    }
    DrawingTool.SHAPE_CIRCLE -> {
      if (points.isNotEmpty()) {
        val start = points.first()
        val end = points.last()
        val left = min(start.x, end.x)
        val top = min(start.y, end.y)
        val width = max(1f, kotlin.math.abs(end.x - start.x))
        val height = max(1f, kotlin.math.abs(end.y - start.y))
        drawOval(
          color = strokeColor,
          topLeft = Offset(left, top),
          size = Size(width, height),
          style = Stroke(width = stroke.strokeWidth),
        )
      }
    }
    else -> {
      // Freeform paths (PEN, HIGHLIGHTER, MARKER, ERASER)
      if (points.size > 1) {
        val path = Path().apply {
          moveTo(points.first().x, points.first().y)
          for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
          }
        }
        drawPath(
          path = path,
          color = strokeColor,
          style = Stroke(
            width = stroke.strokeWidth,
            cap = if (stroke.tool == DrawingTool.HIGHLIGHTER) StrokeCap.Square else StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
        )
      } else if (points.size == 1) {
        drawCircle(
          color = strokeColor,
          radius = stroke.strokeWidth / 2f,
          center = points.first(),
        )
      }
    }
  }
}

/**
 * Bitmap exporter for drawing strokes.
 * Supports all tools (Pen, Highlighter, Marker, Shapes, Eraser) and expanded canvas coordinates.
 */
fun exportStrokesToBitmap(
  strokes: List<DrawingStroke>,
  canvasLeft: Float = 0f,
  canvasTop: Float = 0f,
  canvasRight: Float = 900f,
  canvasBottom: Float = 1200f,
  width: Int = 0,
  height: Int = 0,
  backgroundColor: Int = android.graphics.Color.WHITE,
): Bitmap {
  val rawWidth = if (width > 0) width.toFloat() else max(100f, canvasRight - canvasLeft)
  val rawHeight = if (height > 0) height.toFloat() else max(100f, canvasBottom - canvasTop)

  val maxDim = 1600f
  val scale = if (rawWidth > maxDim || rawHeight > maxDim) {
    min(maxDim / rawWidth, maxDim / rawHeight)
  } else {
    1f
  }

  val finalWidth = max(1, (rawWidth * scale).toInt())
  val finalHeight = max(1, (rawHeight * scale).toInt())

  val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
  val canvas = AndroidCanvas(bitmap)
  canvas.drawColor(backgroundColor)

  strokes.forEach { stroke ->
    val paint = AndroidPaint().apply {
      isAntiAlias = true
      color = stroke.color.toArgb()
      alpha = (stroke.alpha * 255).toInt().coerceIn(0, 255)
      style = AndroidPaint.Style.STROKE
      strokeCap = if (stroke.tool == DrawingTool.HIGHLIGHTER) AndroidPaint.Cap.SQUARE else AndroidPaint.Cap.ROUND
      strokeJoin = AndroidPaint.Join.ROUND
      strokeWidth = stroke.strokeWidth * scale
    }

    val pts = stroke.points
    if (pts.isEmpty()) return@forEach

    when (stroke.tool) {
      DrawingTool.SHAPE_LINE -> {
        val start = pts.first()
        val end = pts.last()
        canvas.drawLine(
          (start.x - canvasLeft) * scale,
          (start.y - canvasTop) * scale,
          (end.x - canvasLeft) * scale,
          (end.y - canvasTop) * scale,
          paint,
        )
      }
      DrawingTool.SHAPE_ARROW -> {
        val start = pts.first()
        val end = pts.last()
        val sx = (start.x - canvasLeft) * scale
        val sy = (start.y - canvasTop) * scale
        val ex = (end.x - canvasLeft) * scale
        val ey = (end.y - canvasTop) * scale
        canvas.drawLine(sx, sy, ex, ey, paint)

        val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
        val arrowLength = max(20f * scale, stroke.strokeWidth * scale * 3f)
        val wingAngle = Math.PI / 6.0
        val x1 = ex - (arrowLength * cos(angle - wingAngle)).toFloat()
        val y1 = ey - (arrowLength * sin(angle - wingAngle)).toFloat()
        val x2 = ex - (arrowLength * cos(angle + wingAngle)).toFloat()
        val y2 = ey - (arrowLength * sin(angle + wingAngle)).toFloat()

        canvas.drawLine(ex, ey, x1, y1, paint)
        canvas.drawLine(ex, ey, x2, y2, paint)
      }
      DrawingTool.SHAPE_RECTANGLE -> {
        val start = pts.first()
        val end = pts.last()
        val left = (min(start.x, end.x) - canvasLeft) * scale
        val top = (min(start.y, end.y) - canvasTop) * scale
        val right = (max(start.x, end.x) - canvasLeft) * scale
        val bottom = (max(start.y, end.y) - canvasTop) * scale
        canvas.drawRoundRect(RectF(left, top, right, bottom), 8f * scale, 8f * scale, paint)
      }
      DrawingTool.SHAPE_CIRCLE -> {
        val start = pts.first()
        val end = pts.last()
        val left = (min(start.x, end.x) - canvasLeft) * scale
        val top = (min(start.y, end.y) - canvasTop) * scale
        val right = (max(start.x, end.x) - canvasLeft) * scale
        val bottom = (max(start.y, end.y) - canvasTop) * scale
        canvas.drawOval(RectF(left, top, right, bottom), paint)
      }
      else -> {
        if (pts.size > 1) {
          val path = AndroidPath()
          path.moveTo((pts.first().x - canvasLeft) * scale, (pts.first().y - canvasTop) * scale)
          for (i in 1 until pts.size) {
            path.lineTo((pts[i].x - canvasLeft) * scale, (pts[i].y - canvasTop) * scale)
          }
          canvas.drawPath(path, paint)
        } else if (pts.size == 1) {
          paint.style = AndroidPaint.Style.FILL
          canvas.drawCircle(
            (pts.first().x - canvasLeft) * scale,
            (pts.first().y - canvasTop) * scale,
            (stroke.strokeWidth / 2f) * scale,
            paint,
          )
        }
      }
    }
  }

  return bitmap
}
