package com.example.dfusetoneforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dfusetoneforge.ui.theme.DfuseToneforgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DfuseToneforgeTheme {
                ToneForgeHome()
            }
        }
    }
}

@Composable
fun ToneForgeHome() {
    val pagerState = rememberPagerState(pageCount = { 2 })

    var linkText by remember { mutableStateOf("") }
    var startMs by remember { mutableLongStateOf(12_000L) }
    var endMs by remember { mutableLongStateOf(42_000L) }
    var isEditingWaveform by remember { mutableStateOf(false) }

    val durationMs = 60_000L
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }

    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var waveformFile by remember { mutableStateOf<File?>(null) }
    var forgedFile by remember { mutableStateOf<File?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "DFUSE",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Tone Forge",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (pagerState.currentPage == 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                        contentColor =
                            if (pagerState.currentPage == 0)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Downloads")
                }

                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (pagerState.currentPage == 1)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                        contentColor =
                            if (pagerState.currentPage == 1)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Forge")
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isEditingWaveform,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> DownloadsPage(
                        linkText = linkText,
                        onLinkChange = { linkText = it },
                        isWorking = isWorking,
                        statusText = statusText,
                        onDownloadClick = {
                            if (linkText.isBlank()) {
                                statusText = "Paste a YouTube link first"
                                return@DownloadsPage
                            }

                            scope.launch {
                                try {
                                    isWorking = true
                                    statusText = "Downloading video/audio..."

                                    val rawFile = downloadYoutubeAudio(context, linkText)
                                    downloadedFile = rawFile

                                    statusText = "Ripping audio..."

                                    val audioFile = extractAudioOnly(context, rawFile)
                                    saveAudioToDownloads(context, audioFile, audioFile.name)

                                    waveformFile = audioFile
                                    forgedFile = null
                                    startMs = 12_000L
                                    endMs = 42_000L

                                    statusText =
                                        "Audio ready.\nSaved to Ringtones/DFUSE Tone Forge\nLoaded: ${audioFile.name}"

                                    pagerState.animateScrollToPage(1)
                                } catch (e: Exception) {
                                    statusText = "Failed:\n${e.message}"
                                } finally {
                                    isWorking = false
                                }
                            }
                        }
                    )

                    1 -> ForgePage(
                        waveformFile = waveformFile,
                        forgedFile = forgedFile,
                        startMs = startMs,
                        endMs = endMs,
                        durationMs = durationMs,
                        statusText = statusText,
                        onTrimChanged = { s, e ->
                            startMs = s
                            endMs = e
                        },
                        onEditingChanged = { editing ->
                            isEditingWaveform = editing
                        },
                        onPreviewClick = {
                            statusText = "Preview coming next"
                        },
                        onForgeClick = {
                            if (waveformFile == null) {
                                statusText = "Download and rip audio first"
                                return@ForgePage
                            }

                            scope.launch {
                                try {
                                    isWorking = true
                                    statusText = "Forging trimmed ringtone..."

                                    val ringtone = forgeRingtone(
                                        context = context,
                                        inputFile = waveformFile!!,
                                        startMs = startMs,
                                        endMs = endMs
                                    )

                                    val finalName =
                                        waveformFile!!.nameWithoutExtension + "_ringtone.m4a"

                                    saveAudioToDownloads(context, ringtone, finalName)

                                    forgedFile = ringtone
                                    statusText =
                                        "Ringtone forged.\nSaved to Ringtones/DFUSE Tone Forge\nFile: $finalName"
                                } catch (e: Exception) {
                                    statusText = "Forge failed:\n${e.message}"
                                } finally {
                                    isWorking = false
                                }
                            }
                        },
                        onSetRingtoneClick = {
                            statusText = "Set ringtone coming next"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadsPage(
    linkText: String,
    onLinkChange: (String) -> Unit,
    isWorking: Boolean,
    statusText: String,
    onDownloadClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 18.dp, bottom = 120.dp)
    ) {
        Text(
            text = "Download + Rip Audio",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Paste a link. DFUSE downloads it, rips audio, saves it, and loads the wave.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = linkText,
            onValueChange = onLinkChange,
            placeholder = { Text("Paste audio/video link") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onDownloadClick,
            enabled = !isWorking,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(if (isWorking) "Working..." else "Download + Rip")
        }

        if (statusText.isNotBlank()) {
            Text(
                text = statusText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ForgePage(
    waveformFile: File?,
    forgedFile: File?,
    startMs: Long,
    endMs: Long,
    durationMs: Long,
    statusText: String,
    onTrimChanged: (Long, Long) -> Unit,
    onEditingChanged: (Boolean) -> Unit,
    onForgeClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onSetRingtoneClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 18.dp, bottom = 140.dp)
    ) {
        Text(
            text = "Forge Ringtone",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = waveformFile?.name ?: "No audio loaded yet",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        if (waveformFile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Audio Editing",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    WaveformTrimEditor(
                        file = waveformFile,
                        startMs = startMs,
                        endMs = endMs,
                        durationMs = durationMs,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.outline,
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        handleColor = MaterialTheme.colorScheme.primary,
                        handleLineColor = MaterialTheme.colorScheme.onSurface,
                        onTrimChanged = onTrimChanged,
                        onEditingChanged = onEditingChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Start: ${formatMs(startMs)}",
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "End: ${formatMs(endMs)}",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onPreviewClick,
                enabled = waveformFile != null,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Preview")
            }

            Button(
                onClick = onForgeClick,
                enabled = waveformFile != null,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Forge Trim")
            }
        }

        Button(
            onClick = onSetRingtoneClick,
            enabled = forgedFile != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Set as Ringtone")
        }

        if (forgedFile != null) {
            Text(
                text = "Forged file:\n${forgedFile.name}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }

        if (statusText.isNotBlank()) {
            Text(
                text = statusText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun WaveformTrimEditor(
    file: File,
    startMs: Long,
    endMs: Long,
    durationMs: Long,
    selectedColor: Color,
    unselectedColor: Color,
    backgroundColor: Color,
    handleColor: Color,
    handleLineColor: Color,
    onTrimChanged: (Long, Long) -> Unit,
    onEditingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var amplitudes by remember(file) { mutableStateOf<List<Float>>(emptyList()) }
    var draggingHandle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        amplitudes = withContext(Dispatchers.IO) {
            val bytes = file.readBytes()
            val sampleCount = 120
            val chunkSize = (bytes.size / sampleCount).coerceAtLeast(1)

            List(sampleCount) { index ->
                val start = index * chunkSize
                val end = minOf(start + chunkSize, bytes.size)

                if (start >= end) {
                    0.1f
                } else {
                    val avg = bytes.copyOfRange(start, end)
                        .map { abs(it.toInt()) }
                        .average()
                        .toFloat()

                    (avg / 128f).coerceIn(0.08f, 1f)
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .background(backgroundColor)
            .pointerInput(startMs, endMs, durationMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onEditingChanged(true)

                        val width = size.width.toFloat()
                        val startX = (startMs.toFloat() / durationMs.toFloat()) * width
                        val endX = (endMs.toFloat() / durationMs.toFloat()) * width

                        draggingHandle =
                            if (abs(offset.x - startX) < abs(offset.x - endX)) {
                                "start"
                            } else {
                                "end"
                            }
                    },
                    onDragEnd = {
                        draggingHandle = null
                        onEditingChanged(false)
                    },
                    onDragCancel = {
                        draggingHandle = null
                        onEditingChanged(false)
                    },
                    onDrag = { change, _ ->
                        change.consume()

                        val width = size.width.toFloat()
                        val tappedMs =
                            ((change.position.x / width).coerceIn(0f, 1f) * durationMs).toLong()

                        val minGap = 2_000L

                        if (draggingHandle == "start") {
                            val newStart = tappedMs.coerceIn(0L, endMs - minGap)
                            onTrimChanged(newStart, endMs)
                        } else {
                            val newEnd = tappedMs.coerceIn(startMs + minGap, durationMs)
                            onTrimChanged(startMs, newEnd)
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val startX = (startMs.toFloat() / durationMs.toFloat()) * width
        val endX = (endMs.toFloat() / durationMs.toFloat()) * width

        if (amplitudes.isNotEmpty()) {
            val barWidth = width / amplitudes.size

            amplitudes.forEachIndexed { index, amp ->
                val x = index * barWidth + barWidth / 2f
                val barHeight = amp * height * 0.55f
                val inSelected = x in startX..endX

                drawLine(
                    color = if (inSelected) selectedColor else unselectedColor,
                    start = Offset(x, centerY - barHeight / 2f),
                    end = Offset(x, centerY + barHeight / 2f),
                    strokeWidth = barWidth * 0.55f,
                    cap = StrokeCap.Round
                )
            }
        }

        drawRect(
            color = selectedColor.copy(alpha = 0.22f),
            topLeft = Offset(startX, 0f),
            size = androidx.compose.ui.geometry.Size(endX - startX, height)
        )

        drawLine(handleLineColor, Offset(startX, 0f), Offset(startX, height), strokeWidth = 5f)
        drawLine(handleLineColor, Offset(endX, 0f), Offset(endX, height), strokeWidth = 5f)

        drawRoundRect(
            color = handleColor,
            topLeft = Offset(startX - 12f, 0f),
            size = androidx.compose.ui.geometry.Size(24f, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
        )

        drawRoundRect(
            color = handleColor,
            topLeft = Offset(endX - 12f, 0f),
            size = androidx.compose.ui.geometry.Size(24f, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Preview(showBackground = true)
@Composable
fun ToneForgePreview() {
    DfuseToneforgeTheme {
        ToneForgeHome()
    }
}