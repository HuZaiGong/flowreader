package com.flowreader.app.ui.screens.reader.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.flowreader.app.domain.model.Annotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF viewer with region-based annotation (v55): `PdfRenderer` has no text layer, so highlights
 * are rectangles. A region is packed into the annotation's start/end positions as
 * `x * 10000 + y` where x/y are 0..999 normalized page coordinates.
 */
object PdfRegionCodec {
    private const val SCALE = 10_000

    fun pack(x: Int, y: Int): Int = x.coerceIn(0, 999) * SCALE + y.coerceIn(0, 999)

    fun unpackX(packed: Int): Int = packed / SCALE

    fun unpackY(packed: Int): Int = packed % SCALE

    fun unpackRect(packedStart: Int, packedEnd: Int): IntArray {
        val x0 = unpackX(packedStart)
        val y0 = unpackY(packedStart)
        val x1 = unpackX(packedEnd)
        val y1 = unpackY(packedEnd)
        return intArrayOf(minOf(x0, x1), minOf(y0, y1), maxOf(x0, x1), maxOf(y0, y1))
    }
}

@Composable
fun PdfViewer(
    filePath: String,
    currentPage: Int,
    textColor: Color,
    backgroundColor: Color,
    annotations: List<Annotation> = emptyList(),
    onAddPdfAnnotation: (Int, Int, Int, Int, Int) -> Unit,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var annotationMode by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }

    var loadError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(filePath, currentPage, retryTrigger) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        loadError = false
        if (filePath.isBlank()) {
            loadError = true
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) { loadError = true }
                    return@withContext
                }
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount
                        if (currentPage in 0 until pageCount) {
                            renderer.openPage(currentPage).use { page ->
                                val bitmap = Bitmap.createBitmap(
                                    page.width,
                                    page.height,
                                    Bitmap.Config.ARGB_8888
                                )
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                pdfBitmap?.recycle()
                                pdfBitmap = bitmap
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PdfViewer", "Failed to render PDF page", e)
                withContext(Dispatchers.Main) { loadError = true }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val viewWidthPx = with(density) { maxWidth.toPx() }
        val viewHeightPx = with(density) { maxHeight.toPx() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (annotationMode) return@detectTapGestures
                            val tapZoneWidth = size.width * 0.3f
                            when {
                                offset.x < tapZoneWidth && currentPage > 0 -> onPageChange(currentPage - 1)
                                offset.x > size.width - tapZoneWidth && currentPage < pageCount - 1 -> onPageChange(currentPage + 1)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (annotationMode) return@detectTransformGestures
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(annotationMode) {
                    if (!annotationMode) return@pointerInput
                    detectDragGestures(
                        onDragStart = { dragStart = it },
                        onDrag = { change, _ ->
                            change.consume()
                            dragCurrent = change.position
                        },
                        onDragEnd = {
                            val start = dragStart
                            val current = dragCurrent
                            if (start != null && current != null && (current - start).getDistance() > 24f) {
                                val bitmap = pdfBitmap
                                if (bitmap != null) {
                                    val scaleFactor = viewWidthPx / bitmap.width.toFloat()
                                    val imageHeight = bitmap.height * scaleFactor
                                    val imageTop = (size.height - imageHeight) / 2f
                                    fun toPage(offset: Offset): Pair<Int, Int> {
                                        val px = (offset.x / scaleFactor).coerceIn(0f, bitmap.width.toFloat())
                                        val py = ((offset.y - imageTop) / scaleFactor).coerceIn(0f, bitmap.height.toFloat())
                                        return (px / bitmap.width * 999).toInt() to (py / bitmap.height * 999).toInt()
                                    }
                                    val (x0, y0) = toPage(start)
                                    val (x1, y1) = toPage(current)
                                    onAddPdfAnnotation(currentPage, x0, y0, x1, y1)
                                }
                            }
                            dragStart = null
                            dragCurrent = null
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
        if (loadError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PDF 文件无法加载",
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { retryTrigger++ }) {
                        Text("重试")
                    }
                }
            }
        } else {
            val bitmap = pdfBitmap
            if (bitmap != null) {
                val scaleFactor = viewWidthPx / bitmap.width.toFloat()
                val imageHeight = bitmap.height * scaleFactor
                val imageTop = (viewHeightPx - imageHeight) / 2f

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF第${currentPage + 1}页",
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val highlight = textColor.copy(alpha = 0.35f)
                        annotations.forEach { annotation ->
                            val (x0, y0, x1, y1) = PdfRegionCodec.unpackRect(annotation.startPosition, annotation.endPosition).toList()
                            drawRect(
                                color = highlight,
                                topLeft = Offset(x0 / 999f * viewWidthPx, imageTop + y0 / 999f * imageHeight),
                                size = Size(
                                    (x1 - x0) / 999f * viewWidthPx,
                                    (y1 - y0) / 999f * imageHeight
                                )
                            )
                        }
                        val start = dragStart
                        val current = dragCurrent
                        if (start != null && current != null && annotationMode) {
                            drawRect(
                                color = highlight,
                                topLeft = Offset(minOf(start.x, current.x), minOf(start.y, current.y)),
                                size = Size(
                                    kotlin.math.abs(current.x - start.x),
                                    kotlin.math.abs(current.y - start.y)
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(3f)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = textColor)
                }
            }
        }

        if (!loadError) {
            FloatingActionButton(
                onClick = { annotationMode = !annotationMode },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(50),
                containerColor = if (annotationMode) MaterialTheme.colorScheme.primary else backgroundColor.copy(alpha = 0.9f)
            ) {
                Icon(
                    imageVector = Icons.Default.BorderColor,
                    contentDescription = if (annotationMode) "退出标注模式" else "标注模式",
                    tint = if (annotationMode) MaterialTheme.colorScheme.onPrimary else textColor
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(backgroundColor.copy(alpha = 0.8f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = currentPage.toFloat(),
                onValueChange = { onPageChange(it.toInt()) },
                valueRange = 0f..(pageCount - 1).coerceAtLeast(0).toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = textColor,
                    activeTrackColor = textColor
                )
            )
            Text(
                text = "${currentPage + 1} / $pageCount",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
        }
    }
}
