package com.verselock.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.palette.graphics.Palette
import android.graphics.drawable.BitmapDrawable
import com.verselock.data.model.LyricsResult
import com.verselock.data.model.TrackInfo
import com.verselock.service.MediaListenerService
import com.verselock.ui.theme.DmSansFamily
import com.verselock.ui.theme.SpaceGroteskFamily
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LockScreenActivity : ComponentActivity() {

    private val viewModel: LockScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen and turn screen on
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide system navigation and status bars fully for immersive lock screen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val track by viewModel.currentTrack.collectAsStateWithLifecycle()
            val lyricIndex by viewModel.currentLyricIndex.collectAsStateWithLifecycle()
            val lyricsResult by viewModel.lyricsResult.collectAsStateWithLifecycle()
            val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

            // Finish activity if playback stops
            LaunchedEffect(isPlaying) {
                if (!isPlaying) {
                    // Give a 30s grace period before dismissing
                    kotlinx.coroutines.delay(30_000)
                    if (!viewModel.isPlaying.value) finish()
                }
            }

            LockScreenContent(
                track = track,
                lyricsResult = lyricsResult,
                activeIndex = lyricIndex,
                isPlaying = isPlaying
            )
        }
    }
}

@Composable
fun LockScreenContent(
    track: TrackInfo?,
    lyricsResult: LyricsResult,
    activeIndex: Int,
    isPlaying: Boolean
) {
    // Clock
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            time = Calendar.getInstance()
            kotlinx.coroutines.delay(1000)
        }
    }

    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF0A0F14)) }
    var vibrantColor by remember { mutableStateOf(Color(0xFF1A3A2A)) }
    var mutedColor by remember { mutableStateOf(Color(0xFF0D1F3C)) }

    LaunchedEffect(track?.albumArtUri) {
        if (track?.albumArtUri != null) {
            val request = ImageRequest.Builder(context)
                .data(track.albumArtUri)
                .allowHardware(false) // Required for Palette API
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.Builder(bitmap).generate()
                    palette.dominantSwatch?.rgb?.let { dominantColor = Color(it) }
                    palette.vibrantSwatch?.rgb?.let { vibrantColor = Color(it) }
                        ?: palette.lightVibrantSwatch?.rgb?.let { vibrantColor = Color(it) }
                    palette.mutedSwatch?.rgb?.let { mutedColor = Color(it) }
                        ?: palette.darkVibrantSwatch?.rgb?.let { mutedColor = Color(it) }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F14))
    ) {
        // Background blobs
        BackgroundBlobs(dominantColor, vibrantColor, mutedColor)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(70.dp)) // ~1.5cm clearance for hole-punch cameras

            // Clock
            ClockBlock(time)

            Spacer(Modifier.height(12.dp))

            // Track pill
            if (track != null) {
                TrackPill(track = track, isPlaying = isPlaying)
            }

            Spacer(Modifier.height(16.dp))

            // Lyrics
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = lyricsResult,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                    },
                    label = "Lyrics Transition"
                ) { state ->
                    when (state) {
                        is LyricsResult.Found -> {
                            LyricsScroller(
                                lines = state.lines.map { it.text },
                                activeIndex = activeIndex
                            )
                        }
                        LyricsResult.NotFound -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No lyrics found",
                                    color = Color.White.copy(alpha = 0.25f),
                                    fontFamily = DmSansFamily,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        LyricsResult.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Loading lyrics...",
                                    color = Color.White.copy(alpha = 0.25f),
                                    fontFamily = DmSansFamily,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun BackgroundBlobs(dominantColor: Color, vibrantColor: Color, mutedColor: Color) {
    val animDominant by animateColorAsState(targetValue = dominantColor, animationSpec = tween(durationMillis = 2000), label = "animDominant")
    val animVibrant by animateColorAsState(targetValue = vibrantColor, animationSpec = tween(durationMillis = 2000), label = "animVibrant")
    val animMuted by animateColorAsState(targetValue = mutedColor, animationSpec = tween(durationMillis = 2000), label = "animMuted")

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Top-left blob -> Muted
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(animMuted.copy(alpha = 0.7f), Color.Transparent),
                center = Offset(-60f, -60f),
                radius = 500f
            ),
            radius = 500f,
            center = Offset(-60f, -60f)
        )
        // Top-right blob -> Vibrant
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(animVibrant.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(size.width + 80f, size.height * 0.25f),
                radius = 400f
            ),
            radius = 400f,
            center = Offset(size.width + 80f, size.height * 0.25f)
        )
        // Bottom-left blob -> Dominant
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(animDominant.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(80f, size.height - 150f),
                radius = 450f
            ),
            radius = 450f,
            center = Offset(80f, size.height - 150f)
        )
    }
}

@Composable
fun ClockBlock(time: Calendar) {
    val hour = time.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minute = String.format("%02d", time.get(Calendar.MINUTE))
    val ampm = if (time.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val dateStr = dateFormat.format(time.time)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$hour:$minute",
                fontSize = 76.sp,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.88f),
                letterSpacing = (-3).sp
            )
            Text(
                text = " $ampm",
                fontSize = 22.sp,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        Text(
            text = dateStr,
            fontSize = 13.sp,
            fontFamily = DmSansFamily,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.35f)
        )
    }
}

@Composable
fun TrackPill(track: TrackInfo, isPlaying: Boolean) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .widthIn(max = 340.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Album art
        if (track.albumArtUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(track.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4A7C5A), Color(0xFF2D5A8A), Color(0xFF7A4A2A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("♪", fontSize = 14.sp, color = Color.White)
            }
        }

        // Title + artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 12.sp,
                fontFamily = DmSansFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = buildString {
                append(track.artist)
                if (track.album.isNotBlank()) append(" · ${track.album}")
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontFamily = DmSansFamily,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.40f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Play/pause controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    context.startService(Intent(context, MediaListenerService::class.java).apply { action = "com.verselock.ACTION_PREV" })
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            IconButton(
                onClick = {
                    val action = if (isPlaying) "com.verselock.ACTION_PAUSE" else "com.verselock.ACTION_PLAY"
                    context.startService(Intent(context, MediaListenerService::class.java).apply { this.action = action })
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = {
                    context.startService(Intent(context, MediaListenerService::class.java).apply { action = "com.verselock.ACTION_NEXT" })
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LyricsScroller(lines: List<String>, activeIndex: Int) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to active line
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lines.size) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -400 // negative offset to center the line
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 40.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                val diff = index - activeIndex
                val isActive = diff == 0

                // Animated opacity
                val targetOpacity = when {
                    isActive -> 1.0f
                    diff == -1 || diff == 1 -> if (diff < 0) 0.38f else 0.45f
                    diff == -2 || diff == 2 -> 0.28f
                    else -> 0.18f
                }
                val animatedOpacity by animateFloatAsState(
                    targetValue = targetOpacity,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "lyric_opacity_$index"
                )

                val targetSize = when {
                    isActive -> 26f
                    kotlin.math.abs(diff) == 1 -> 23f
                    else -> 22f
                }
                val animatedSize by animateFloatAsState(
                    targetValue = targetSize,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "lyric_size_$index"
                )

                val targetOffset = if (isActive) 2f else 0f
                val animatedOffset by animateFloatAsState(
                    targetValue = targetOffset,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "lyric_offset_$index"
                )

                // Pulse glow for active line
                val infiniteTransition = rememberInfiniteTransition(label = "glow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = if (isActive) 0.35f else 0.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow_alpha"
                )

                Text(
                    text = line,
                    fontSize = animatedSize.sp,
                    fontFamily = DmSansFamily,
                    fontWeight = when {
                        isActive -> FontWeight.Bold
                        index < activeIndex -> FontWeight.Normal
                        else -> FontWeight.Medium
                    },
                    color = Color.White.copy(alpha = animatedOpacity),
                    lineHeight = (animatedSize * 1.35f).sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = animatedOffset.dp,
                            bottom = if (isActive) 22.dp else 18.dp
                        )
                )
            }
        }

        // Top fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A0F14).copy(alpha = 0.90f), Color.Transparent)
                    )
                )
        )

        // Bottom fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF0A0F14).copy(alpha = 0.95f))
                    )
                )
        )
    }
}
