package com.example.dfusetoneforge

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.dfusetoneforge.ui.theme.DfuseToneforgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max


class AudioEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(
            WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
        )
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val audioPath = intent.getStringExtra("audioPath")

        setContent {
            DfuseToneforgeTheme {
                AudioEditorScreen(
                    audioPath = audioPath,
                    onBackClick = { finish() },
                    onDoneClick = { startMs, endMs ->
                        val result = Intent().apply {
                            putExtra("startMs", startMs)
                            putExtra("endMs", endMs)
                        }

                        setResult(Activity.RESULT_OK, result)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AudioEditorScreen(
    audioPath: String?,
    onBackClick: () -> Unit,
    onDoneClick: (Long, Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoopPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val fileName = audioPath?.let { File(it).name } ?: "No audio loaded"
    val audioInfo = remember(audioPath) { readEditorAudioInfo(audioPath) }

    var startMs by remember(audioPath) { mutableLongStateOf(0L) }
    var endMs by remember(audioPath, audioInfo.durationMs) {
        mutableLongStateOf(audioInfo.durationMs.coerceAtLeast(1L))
    }

    LaunchedEffect(audioInfo.durationMs) {
        startMs = 0L
        endMs = audioInfo.durationMs.coerceAtLeast(1L)
    }

    fun stopCurrentPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        isLoopPlaying = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF090511)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TopEditorBar(
                onBackClick = {
                    stopCurrentPlayer()
                    onBackClick()
                },
                isPlaying = isPlaying,
                isLoopPlaying = isLoopPlaying,
                onPlayClick = {
                    if (audioPath == null) return@TopEditorBar

                    mediaPlayer?.let { player ->
                        if (player.isPlaying && !isLoopPlaying) {
                            player.pause()
                            isPlaying = false
                            return@TopEditorBar
                        }
                    }

                    try {
                        stopCurrentPlayer()

                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioPath)
                            prepare()
                            start()

                            setOnCompletionListener {
                                isPlaying = false
                                isLoopPlaying = false
                            }
                        }

                        isPlaying = true
                        isLoopPlaying = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        stopCurrentPlayer()
                    }
                }
            )

            TrackInfoRow(
                fileName = fileName,
                durationText = audioInfo.durationText,
                formatText = audioInfo.format,
                bitrateText = audioInfo.bitrate
            )

            WaveformCard(
                audioPath = audioPath,
                startMs = startMs,
                endMs = endMs,
                durationMs = audioInfo.durationMs.coerceAtLeast(1L),
                onTrimChanged = { newStart, newEnd ->
                    startMs = newStart
                    endMs = newEnd
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            BottomControls(
                startText = formatEditorDuration(startMs),
                endText = formatEditorDuration(endMs),
                isLoopPlaying = isLoopPlaying,

                onLoopPlayClick = {
                    if (audioPath == null) return@BottomControls

                    mediaPlayer?.let { player ->
                        if (player.isPlaying && isLoopPlaying) {
                            player.pause()
                            isPlaying = false
                            isLoopPlaying = false
                            return@BottomControls
                        }
                    }

                    try {
                        stopCurrentPlayer()

                        val loopStart = startMs
                        val loopEnd = endMs

                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioPath)
                            prepare()
                            seekTo(loopStart.toInt())
                            start()
                        }

                        isPlaying = true
                        isLoopPlaying = true

                        val player = mediaPlayer

                        scope.launch {
                            while (player != null && player.isPlaying && isLoopPlaying) {

                                if (player.currentPosition >= loopEnd) {
                                    player.pause()
                                    player.seekTo(loopStart.toInt())

                                    isPlaying = false
                                    isLoopPlaying = false
                                    break
                                }

                                delay(40)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        stopCurrentPlayer()
                    }
                },


                onResetClick = {
                    stopCurrentPlayer()
                    startMs = 0L
                    endMs = audioInfo.durationMs.coerceAtLeast(1L)
                },

                onDoneClick = {
                    stopCurrentPlayer()
                    onDoneClick(startMs, endMs)
                }
            )
        }
    }
}
@Composable
fun TopEditorBar(
    onBackClick: () -> Unit,
    isPlaying: Boolean,
    isLoopPlaying: Boolean,
    onPlayClick: () -> Unit
){
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹ Back",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .weight(1f)
                .clickable { onBackClick() }
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Audio Editor",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Drag handles to select start and end",
                color = Color(0xFFC8A7FF),
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            OutlinedButton(
                onClick = onPlayClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, Color(0xFF7C3AED))
            ) {
                Text(
                    text = if (isPlaying && !isLoopPlaying) "⏸ Pause All" else "▷ Play All",
                    color = Color(0xFFC8A7FF),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun TrackInfoRow(
    fileName: String,
    durationText: String,
    formatText: String,
    bitrateText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF17121F)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF5B21B6)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔨", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Track info",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = cleanEditorDisplayName(fileName),
                    color = Color(0xFFB8AEC7),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StatBox("Duration", durationText)
            StatBox("Format", formatText)
            StatBox("Bitrate", bitrateText)
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFFB8AEC7),
            fontSize = 9.sp
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WaveformCard(
    audioPath: String?,
    startMs: Long,
    endMs: Long,
    durationMs: Long,
    onTrimChanged: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var amplitudes by remember(audioPath) { mutableStateOf<List<Float>>(emptyList()) }
    var draggingHandle by remember { mutableStateOf<TrimHandle?>(null) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val currentStartMs by rememberUpdatedState(startMs)
    val currentEndMs by rememberUpdatedState(endMs)

    LaunchedEffect(audioPath) {
        amplitudes = loadWaveformAmplitudes(audioPath)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15101D))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .pointerInput(safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat().coerceAtLeast(1f)

                            val startX = (currentStartMs.toFloat() / safeDuration.toFloat()) * width
                            val endX = (currentEndMs.toFloat() / safeDuration.toFloat()) * width

                            draggingHandle =
                                if (abs(offset.x - startX) <= abs(offset.x - endX)) {
                                    TrimHandle.START
                                } else {
                                    TrimHandle.END
                                }
                        },
                        onDragEnd = { draggingHandle = null },
                        onDragCancel = { draggingHandle = null },
                        onDrag = { change, _ ->
                            change.consume()

                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val minGap = 1_500L

                            val touchedMs = ((change.position.x / width) * safeDuration)
                                .toLong()
                                .coerceIn(0L, safeDuration)

                            when (draggingHandle) {
                                TrimHandle.START -> {
                                    val maxStart = currentEndMs - minGap
                                    val newStart = touchedMs.coerceIn(0L, maxStart)
                                    onTrimChanged(newStart, currentEndMs)
                                }

                                TrimHandle.END -> {
                                    val minEnd = currentStartMs + minGap
                                    val newEnd = touchedMs.coerceIn(minEnd, safeDuration)
                                    onTrimChanged(currentStartMs, newEnd)
                                }

                                null -> Unit
                            }
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            val startProgress = (startMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            val endProgress = (endMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

            val startX = startProgress * width
            val endX = endProgress * width

            drawRect(Color(0xFF120A1B))

            if (amplitudes.isNotEmpty()) {
                val barWidth = width / amplitudes.size

                amplitudes.forEachIndexed { i, amp ->
                    val x = i * barWidth + barWidth / 2f
                    val barHeight = height * amp.coerceIn(0.08f, 1f)

                    val isSelected = x in startX..endX
                    val color = if (isSelected) Color(0xFFB178FF) else Color(0xFF5F5A6D)

                    drawLine(
                        color = color,
                        start = Offset(x, centerY - barHeight / 2f),
                        end = Offset(x, centerY + barHeight / 2f),
                        strokeWidth = max(2.2f, barWidth * 0.45f),
                        cap = StrokeCap.Round
                    )
                }
            }

            drawRect(
                color = Color(0x33B178FF),
                topLeft = Offset(startX, 0f),
                size = Size(endX - startX, height)
            )

            val handleWidth = 18f

            drawRoundRect(
                color = Color(0xFFC8A7FF),
                topLeft = Offset(startX - handleWidth / 2f, 0f),
                size = Size(handleWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )

            drawRoundRect(
                color = Color(0xFFC8A7FF),
                topLeft = Offset(endX - handleWidth / 2f, 0f),
                size = Size(handleWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
        }
    }
}

enum class TrimHandle {
    START,
    END
}

@Composable
fun BottomControls(
    startText: String,
    endText: String,
    isLoopPlaying: Boolean,
    onLoopPlayClick: () -> Unit,
    onResetClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        OutlinedButton(
            onClick = onResetClick,
            border = BorderStroke(1.dp, Color(0xFF4D4658))
        ) {
            Text("Reset")
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$startText  →  $endText",
            color = Color(0xFFB8AEC7),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.weight(1f))


        OutlinedButton(
            onClick = onLoopPlayClick,
            modifier = Modifier.size(72.dp),
            contentPadding = PaddingValues(0.dp),
            border = BorderStroke(1.dp, Color(0xFF7C3AED))
        ) {
            Icon(
                imageVector =
                    if (isLoopPlaying)
                        Icons.Default.Pause
                    else
                        Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = Color(0xFFC8A7FF)

            )
        }

        Spacer(modifier = Modifier.weight(1f))


        Button(
            onClick = onDoneClick
        ) {
            Text("✓ Done")
        }
    }
}

suspend fun loadWaveformAmplitudes(
    audioPath: String?,
    bars: Int = 190
): List<Float> = withContext(Dispatchers.IO) {
    if (audioPath == null) return@withContext emptyList()

    val extractor = MediaExtractor()

    try {
        extractor.setDataSource(audioPath)

        var audioTrackIndex = -1

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                break
            }
        }

        if (audioTrackIndex == -1) {
            return@withContext emptyList()
        }

        extractor.selectTrack(audioTrackIndex)

        val sampleSizes = mutableListOf<Int>()
        val buffer = ByteBuffer.allocate(256 * 1024)

        while (true) {
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)

            if (size <= 0) break

            sampleSizes.add(size)
            extractor.advance()
        }

        if (sampleSizes.isEmpty()) {
            return@withContext emptyList()
        }

        val grouped = MutableList(bars) { 0f }
        val counts = MutableList(bars) { 0 }

        sampleSizes.forEachIndexed { index, size ->
            val bucket = ((index.toFloat() / sampleSizes.size.toFloat()) * bars)
                .toInt()
                .coerceIn(0, bars - 1)

            grouped[bucket] += size.toFloat()
            counts[bucket] += 1
        }

        val averaged = grouped.mapIndexed { index, value ->
            if (counts[index] == 0) 0f else value / counts[index]
        }

        val maxValue = averaged.maxOrNull()?.coerceAtLeast(1f) ?: 1f

        averaged.map { value ->
            (value / maxValue).coerceIn(0.08f, 1f)
        }
    } catch (e: Exception) {
        emptyList()
    } finally {
        extractor.release()
    }
}

data class EditorAudioInfo(
    val durationText: String,
    val durationMs: Long,
    val format: String,
    val bitrate: String
)

fun readEditorAudioInfo(audioPath: String?): EditorAudioInfo {
    if (audioPath == null) {
        return EditorAudioInfo(
            durationText = "0:00",
            durationMs = 0L,
            format = "--",
            bitrate = "--"
        )
    }

    val file = File(audioPath)
    val retriever = MediaMetadataRetriever()

    return try {
        retriever.setDataSource(audioPath)

        val durationMs =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L

        val bitrate =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull()
                ?.let { "${it / 1000}" }
                ?: "--"

        EditorAudioInfo(
            durationText = formatEditorDuration(durationMs),
            durationMs = durationMs,
            format = file.extension.ifBlank { "audio" },
            bitrate = bitrate
        )
    } catch (e: Exception) {
        EditorAudioInfo(
            durationText = "0:00",
            durationMs = 0L,
            format = file.extension.ifBlank { "audio" },
            bitrate = "--"
        )
    } finally {
        retriever.release()
    }
}

private fun cleanEditorDisplayName(name: String): String {
    return name
        .removeSuffix(".m4a")
        .removeSuffix(".mp4")
        .removeSuffix(".webm")
        .removeSuffix(".opus")
        .replace("-web_audio", "")
        .replace("-android_audio", "")
        .replace("_audio", "")
        .replace("_ringtone", "")
        .trim()
}

private fun formatEditorDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
