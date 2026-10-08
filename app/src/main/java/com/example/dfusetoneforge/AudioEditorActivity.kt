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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import java.nio.ByteOrder
import android.media.AudioFormat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
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
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import android.media.MediaCodec
import androidx.compose.ui.graphics.nativeCanvas


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
                    initialStartMs = intent.getLongExtra("startMs", 0L),
                    initialEndMs = intent.getLongExtra("endMs", -1L),
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
    onDoneClick: (Long, Long) -> Unit,
    initialStartMs: Long = 0L,
    initialEndMs: Long = -1L
) {
    val editorContext = LocalContext.current
    val loopPreview = remember { editorContext.getSharedPreferences("dfuse_prefs", android.content.Context.MODE_PRIVATE).getBoolean("loopPreview", false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoopPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    var audioInfo by remember(audioPath) { mutableStateOf(EditorAudioInfo("0:00", 0L, "--", "--")) }
    var startMs by remember(audioPath) { mutableLongStateOf(0L) }
    var endMs by remember(audioPath) { mutableLongStateOf(0L) }
    val latestStart by rememberUpdatedState(startMs)
    val latestEnd by rememberUpdatedState(endMs)
    val ready = audioInfo.durationMs > 0L
    val lifecycleOwner = LocalLifecycleOwner.current

    fun stopPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        isLoopPlaying = false
    }

    LaunchedEffect(audioPath) {
        audioInfo = withContext(Dispatchers.IO) { readEditorAudioInfo(audioPath) }
        endMs = if (initialEndMs > 0) initialEndMs.coerceAtMost(audioInfo.durationMs) else audioInfo.durationMs
        startMs = initialStartMs.coerceIn(0L, (endMs - 1).coerceAtLeast(0L))
        if (audioInfo.durationMs <= 0) error = "Unable to open this audio. Go back and choose another track."
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                stopPlayer()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopPlayer()
        }
    }
    LaunchedEffect(isPlaying, mediaPlayer) {
        while (isPlaying) {
            mediaPlayer?.let { player ->
                positionMs = player.currentPosition.toLong()
                if (isLoopPlaying && positionMs >= latestEnd) {
                    player.seekTo(latestStart.toInt())
                    positionMs = latestStart
                    if (!loopPreview) {
                        player.pause()
                        isPlaying = false
                        isLoopPlaying = false
                    }
                }
            }
            delay(25)
        }
    }
    fun play(selection: Boolean) {
        if (!ready || audioPath == null) return
        if (isPlaying && selection == isLoopPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            return
        }
        stopPlayer()
        error = null
        try {
            val player = MediaPlayer()
            mediaPlayer = player
            player.setDataSource(audioPath)
            player.setOnPreparedListener {
                positionMs = if (selection) startMs else positionMs.coerceIn(0L, audioInfo.durationMs - 1)
                it.seekTo(positionMs.toInt())
                isLoopPlaying = selection
                it.start()
                isPlaying = true
            }
            player.setOnCompletionListener {
                if (selection && loopPreview) {
                    it.seekTo(latestStart.toInt())
                    positionMs = latestStart
                    it.start()
                } else {
                    isPlaying = false
                    isLoopPlaying = false
                    positionMs = audioInfo.durationMs
                }
            }
            player.setOnErrorListener { _, _, _ ->
                error = "Playback failed. Try reopening the track."
                stopPlayer()
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            stopPlayer()
            error = "Playback failed: ${e.message ?: "unsupported audio"}"
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { stopPlayer(); onBackClick() }) { Text("‹ Back") }
                Text(audioPath?.let { cleanEditorDisplayName(File(it).name) } ?: "Audio editor",
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${audioInfo.format} · ${audioInfo.bitrate}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                TextButton(onClick = { play(false) }, enabled = ready) { Text(if (isPlaying && !isLoopPlaying) "Pause" else "Play all") }
            }
            error?.let { Text(it, color = Color(0xFFFFB4AB), fontSize = 12.sp) }
            WaveformCard(
                audioPath, startMs, endMs, audioInfo.durationMs.coerceAtLeast(1L),
                onTrimChanged = { start, end ->
                    if (isLoopPlaying) stopPlayer()
                    startMs = start; endMs = end
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                positionMs = positionMs,
                onSeek = { value ->
                    if (isLoopPlaying) stopPlayer()
                    positionMs = value
                    mediaPlayer?.seekTo(value.toInt())
                }
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Start", color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { startMs = (startMs - 100).coerceAtLeast(0) }, enabled = ready) { Text("−0.1s") }
                Text(formatEditorDuration(startMs), color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { startMs = (startMs + 100).coerceAtMost((endMs - 1).coerceAtLeast(0)) }, enabled = ready) { Text("+0.1s") }
                Text("End", color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { endMs = (endMs - 100).coerceAtLeast(startMs + 1) }, enabled = ready) { Text("−0.1s") }
                Text(formatEditorDuration(endMs), color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { endMs = (endMs + 100).coerceAtMost(audioInfo.durationMs) }, enabled = ready) { Text("+0.1s") }
                Text("Selected ${formatEditorDuration(endMs - startMs)}", color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { stopPlayer(); startMs = 0; endMs = audioInfo.durationMs; positionMs = 0 }, enabled = ready) { Text("Reset") }
                Text("${formatEditorDuration(positionMs)} / ${audioInfo.durationText}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                OutlinedButton(onClick = { play(true) }, enabled = ready) {
                    Icon(if (isPlaying && isLoopPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Preview selection")
                    Text(if (isPlaying && isLoopPlaying) "Pause" else "Preview selection")
                }
                Button(onClick = { stopPlayer(); onDoneClick(startMs, endMs) }, enabled = ready && endMs > startMs) { Text("Use selection") }
            }
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
            color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Drag handles to select start and end",
                color = MaterialTheme.colorScheme.primary,
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (isPlaying && !isLoopPlaying) "⏸ Pause All" else "▷ Play All",
                    color = MaterialTheme.colorScheme.primary,
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("🔨", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Track info",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = cleanEditorDisplayName(fileName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )

        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WaveformCard(
    audioPath: String?, startMs: Long, endMs: Long, durationMs: Long,
    onTrimChanged: (Long, Long) -> Unit, modifier: Modifier = Modifier,
    positionMs: Long = 0L, onSeek: (Long) -> Unit = {}
) {
    var amplitudes by remember(audioPath) { mutableStateOf<List<Float>>(emptyList()) }
    var loading by remember(audioPath) { mutableStateOf(true) }
    var progress by remember(audioPath) { mutableFloatStateOf(0f) }
    val waveformCacheDir = LocalContext.current.cacheDir
    val waveformColors = MaterialTheme.colorScheme
    var zoom by remember(audioPath) { mutableFloatStateOf(1f) }
    var viewport by remember(audioPath) { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf<TrimHandle?>(null) }
    val currentStart by rememberUpdatedState(startMs)
    val currentEnd by rememberUpdatedState(endMs)
    val trimCallback by rememberUpdatedState(onTrimChanged)
    val seekCallback by rememberUpdatedState(onSeek)
    val span = durationMs.toDouble() / zoom
    val viewStart = viewport * (durationMs - span)
    LaunchedEffect(audioPath) {
        loading = true
        try {
            amplitudes = loadCachedWaveform(waveformCacheDir, audioPath) { partial, fraction ->
                withContext(Dispatchers.Main) {
                    amplitudes = partial
                    progress = fraction
                }
            }
        }
        finally { loading = false }
    }
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("WAVEFORM", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Text("Tap to seek · drag handles", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                TextButton(onClick = { zoom = (zoom / 2).coerceAtLeast(1f) }, enabled = zoom > 1) { Text("−") }
                Text("${zoom.toInt()}×", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                TextButton(onClick = { zoom = (zoom * 2).coerceAtMost(16f) }, enabled = zoom < 16) { Text("+") }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Canvas(Modifier.fillMaxSize()
                    .pointerInput(durationMs, zoom, viewport) {
                        detectTapGestures { offset ->
                            seekCallback((viewStart + offset.x / size.width.coerceAtLeast(1) * span).toLong().coerceIn(0, durationMs))
                        }
                    }
                    .pointerInput(durationMs, zoom, viewport) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val sx = ((currentStart - viewStart) / span * size.width).toFloat()
                                val ex = ((currentEnd - viewStart) / span * size.width).toFloat()
                                val hit = 28.dp.toPx()
                                dragging = when {
                                    abs(offset.x - sx) <= hit && abs(offset.x - sx) <= abs(offset.x - ex) -> TrimHandle.START
                                    abs(offset.x - ex) <= hit -> TrimHandle.END
                                    else -> null
                                }
                            },
                            onDragEnd = { dragging = null }, onDragCancel = { dragging = null },
                            onDrag = { change, _ ->
                                change.consume()
                                val time = (viewStart + change.position.x / size.width.coerceAtLeast(1) * span).toLong().coerceIn(0, durationMs)
                                when (dragging) {
                                    TrimHandle.START -> trimCallback(time.coerceAtMost((currentEnd - 1).coerceAtLeast(0)), currentEnd)
                                    TrimHandle.END -> trimCallback(currentStart, time.coerceAtLeast((currentStart + 1).coerceAtMost(durationMs)))
                                    null -> seekCallback(time)
                                }
                            }
                        )
                    }) {
                    val rulerHeight = 22.dp.toPx()
                    val bottom = size.height
                    val center = (bottom + rulerHeight) / 2
                    fun x(time: Long) = ((time - viewStart) / span * size.width).toFloat()
                    val sx = x(startMs); val ex = x(endMs)
                    drawRect(waveformColors.background)
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(184, 174, 199)
                        textSize = 10.sp.toPx(); isAntiAlias = true
                    }
                    for (tick in 0..4) {
                        val tx = size.width * tick / 4
                        drawLine(waveformColors.onSurface.copy(alpha = 0.08f), Offset(tx, rulerHeight), Offset(tx, bottom), 1f)
                        paint.textAlign = when(tick) { 0 -> android.graphics.Paint.Align.LEFT; 4 -> android.graphics.Paint.Align.RIGHT; else -> android.graphics.Paint.Align.CENTER }
                        drawContext.canvas.nativeCanvas.drawText(formatEditorDuration((viewStart + span * tick / 4).toLong()), tx, 12.sp.toPx(), paint)
                    }
                    drawLine(waveformColors.onSurface.copy(alpha = 0.2f), Offset(0f, center), Offset(size.width, center), 1f)
                    val left = sx.coerceIn(0f, size.width); val right = ex.coerceIn(0f, size.width)
                    drawRect(waveformColors.primary.copy(alpha = 0.12f), Offset(left, rulerHeight), Size((right-left).coerceAtLeast(0f), (bottom-rulerHeight).coerceAtLeast(0f)))
                    if (amplitudes.isNotEmpty()) {
                        val barCount = size.width.toInt().coerceAtLeast(1)
                        val envelope = FloatArray(barCount)
                        for (bar in 0 until barCount) {
                            val t0 = viewStart + span * bar / barCount
                            val t1 = viewStart + span * (bar + 1) / barCount
                            val first = (t0 / durationMs * amplitudes.size).toInt().coerceIn(0, amplitudes.lastIndex)
                            val last = (t1 / durationMs * amplitudes.size).toInt().coerceIn(first, amplitudes.lastIndex)
                            var peak = 0f
                            for (i in first..last) peak = max(peak, amplitudes[i])
                            val tx = (bar + 0.5f) * size.width / barCount
                            val half = peak * (bottom-rulerHeight) * 0.43f
                            envelope[bar] = half
                        }
                        val path = Path()
                        path.moveTo(0f, center)
                        for (bar in 0 until barCount) path.lineTo(bar.toFloat(), center - envelope[bar])
                        for (bar in barCount - 1 downTo 0) path.lineTo(bar.toFloat(), center + envelope[bar])
                        path.close()
                        drawPath(path, waveformColors.primary.copy(alpha = 0.35f))
                        drawContext.canvas.save()
                        drawContext.canvas.clipRect(left, rulerHeight, right, bottom)
                        drawPath(path, waveformColors.primary)
                        drawContext.canvas.restore()
                    }

                    for (handleX in listOf(sx, ex)) {
                        if (handleX in 0f..size.width) {
                            val hx = handleX.coerceIn(6.dp.toPx(), (size.width - 6.dp.toPx()).coerceAtLeast(6.dp.toPx()))
                            drawLine(waveformColors.primary, Offset(hx, rulerHeight), Offset(hx, bottom), 2.dp.toPx())
                            drawRoundRect(waveformColors.primary, Offset(hx-3.dp.toPx(), center-16.dp.toPx()),
                                Size(6.dp.toPx(), 32.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                        }
                    }
                    val px = x(positionMs)
                    if (px in 0f..size.width) {
                        drawLine(waveformColors.onSurface, Offset(px, rulerHeight), Offset(px, bottom), 2.dp.toPx())
                        drawCircle(waveformColors.onSurface, 4.dp.toPx(), Offset(px, rulerHeight))
                    }
                }
                if (loading) Text(
                    if (progress > 0f) "Building waveform ${(progress * 100).toInt()}%" else "Opening waveform…",
                    color = MaterialTheme.colorScheme.primary, fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.87f)).padding(6.dp)
                )
                else if (amplitudes.isEmpty()) Text("Waveform unavailable. You can still preview and adjust the selection.",
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center).padding(16.dp))
            }
            if (zoom > 1f) Slider(value = viewport, onValueChange = { viewport = it }, modifier = Modifier.height(30.dp))
        }
    }
}

enum class TrimHandle {
    START,
    END
}

data class EditorAudioInfo(
    val durationText: String,
    val durationMs: Long,
    val format: String,
    val bitrate: String
)
suspend fun loadWaveformAmplitudes(
    audioPath: String?, bars: Int = 16_384,
    onProgress: suspend (List<Float>, Float) -> Unit = { _, _ -> }
): List<Float> = withContext(Dispatchers.IO) {
    if (audioPath == null) return@withContext emptyList()
    require(bars > 0)
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    try {
        extractor.setDataSource(audioPath)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return@withContext emptyList()
        val format = extractor.getTrackFormat(track)
        extractor.selectTrack(track)
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION)
            else readEditorAudioInfo(audioPath).durationMs * 1000
        if (durationUs <= 0) return@withContext emptyList()
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        decoder = codec
        codec.configure(format, null, null, 0)
        codec.start()
        var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var floatPcm = false
        val peaks = FloatArray(bars)
        val energy = DoubleArray(bars)
        val counts = IntArray(bars)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var lastOutput = System.nanoTime()
        var lastPreview = lastOutput
        fun snapshot(): List<Float> {
            val levels = FloatArray(bars) { i ->
                if (counts[i] == 0) 0f else (kotlin.math.sqrt(energy[i] / counts[i]) * 0.85 + peaks[i] * 0.15).toFloat()
            }
            val scale = (levels.maxOrNull() ?: 0f).coerceAtLeast(0.001f)
            return levels.map { (it / scale).coerceIn(0f, 1f) }
        }
        while (!outputDone) {
            ensureActive()
            check(System.nanoTime() - lastOutput < 15_000_000_000L) { "Audio decoder timed out" }
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(0)
                if (index >= 0) {
                    val buffer = requireNotNull(codec.getInputBuffer(index))
                    buffer.clear()
                    val count = extractor.readSampleData(buffer, 0)
                    if (count < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(index, 0, count, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val index = codec.dequeueOutputBuffer(info, 1_000)
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val output = codec.outputFormat
                rate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) output.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                check(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT) { "Unsupported PCM encoding" }
                floatPcm = encoding == AudioFormat.ENCODING_PCM_FLOAT
                lastOutput = System.nanoTime()
            } else if (index >= 0) {
                lastOutput = System.nanoTime()
                try {
                    val buffer = codec.getOutputBuffer(index)
                    if (buffer != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        buffer.order(ByteOrder.nativeOrder())
                        val frameBytes = channels * if (floatPcm) 4 else 2
                        val frames = buffer.remaining() / frameBytes
                        val baseOffset = buffer.position()
                        // The waveform is an amplitude envelope, not an audio reconstruction.
                        // Limit analysis to ~8k frames/sec, retaining finer sampling for short clips.
                        val framesPerBucket = (durationUs.toDouble() * rate / 1_000_000 / bars).coerceAtLeast(1.0)
                        val stride = minOf((rate / 8_000).coerceAtLeast(1), framesPerBucket.toInt().coerceAtLeast(1))
                        val bytesPerSample = if (floatPcm) 4 else 2
                        val bucketPerFrame = bars.toDouble() * 1_000_000 / (durationUs.toDouble() * rate)
                        val firstBucket = info.presentationTimeUs.toDouble() * bars / durationUs
                        var frame = 0
                        while (frame < frames) {
                            var peak = 0f
                            val offset = baseOffset + frame * frameBytes
                            var channel = 0
                            while (channel < channels) {
                                val at = offset + channel * bytesPerSample
                                val sample = if (floatPcm) buffer.getFloat(at) else buffer.getShort(at) / 32768f
                                if (sample.isFinite()) peak = max(peak, abs(sample))
                                channel++
                            }
                            val bucket = (firstBucket + frame * bucketPerFrame).toInt()
                            if (bucket in 0 until bars) {
                                peaks[bucket] = max(peaks[bucket], peak)
                                energy[bucket] += peak.toDouble() * peak
                                counts[bucket]++
                            }
                            frame += stride
                        }
                    }
                } finally { codec.releaseOutputBuffer(index, false) }
                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                if (!outputDone && System.nanoTime() - lastPreview >= 350_000_000L) {
                    lastPreview = System.nanoTime()
                    onProgress(snapshot(), (info.presentationTimeUs.toDouble() / durationUs).toFloat().coerceIn(0f, 0.99f))
                }
            }
        }
        snapshot()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        android.util.Log.w("ToneForge", "Waveform decode failed", e)
        emptyList()
    } finally {
        try { decoder?.stop() } catch (_: Exception) { }
        decoder?.release()
        extractor.release()
    }
}
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
                ?.let { "${it / 1000} kbps" }
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

    return "$minutes:${seconds.toString().padStart(2, '0')}.${((ms % 1000) / 10).toString().padStart(2, '0')}"
}
