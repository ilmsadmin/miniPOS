package com.minipos.ui.scanner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipos.R
import java.io.File

/**
 * Full-screen image viewer with swipe between images and pinch-to-zoom.
 *
 * @param images list of image file paths
 * @param initialIndex the index of the image to show first
 * @param onClose callback when the viewer is dismissed
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewerScreen(
    images: List<String>,
    initialIndex: Int = 0,
    onClose: () -> Unit,
) {
    if (images.isEmpty()) {
        onClose()
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Pager with zoomable images
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { images[it] },
        ) { page ->
            ZoomableImage(imagePath = images[page])
        }

        // Top bar with close button and counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                ),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_cd_img),
                    tint = Color.White,
                )
            }

            if (images.size > 1) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${images.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }

            // Spacer for balance
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Page indicators at bottom
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(images.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val size by animateFloatAsState(
                        targetValue = if (isSelected) 10f else 6f,
                        label = "indicator_size",
                    )
                    Box(
                        modifier = Modifier
                            .size(size.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * A zoomable image that supports pinch-to-zoom and double-tap-to-zoom.
 * When not zoomed (scale == 1f), horizontal drags pass through to the HorizontalPager.
 */
@Composable
private fun ZoomableImage(imagePath: String) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        label = "zoom_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Custom pointer input that only consumes pan when zoomed
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()

                        // Always handle pinch zoom (2+ fingers)
                        if (zoom != 1f) {
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            if (newScale <= 1f) {
                                offset = Offset.Zero
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                        // Only handle pan when zoomed in
                        else if (scale > 1.01f && pan != Offset.Zero) {
                            val maxX = (size.width * (scale - 1f)) / 2f
                            val maxY = (size.height * (scale - 1f)) / 2f
                            val newOffset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                            )
                            // Only consume horizontal pan if we're not at the edge
                            // (allow pager to take over when panned to edge)
                            val atLeftEdge = offset.x >= maxX - 1f && pan.x > 0
                            val atRightEdge = offset.x <= -maxX + 1f && pan.x < 0
                            if (!atLeftEdge && !atRightEdge) {
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                            offset = newOffset
                        }
                        // When not zoomed: don't consume — let HorizontalPager handle swipe
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.5f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 3f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(imagePath))
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
            contentScale = ContentScale.Fit,
        )
    }
}
