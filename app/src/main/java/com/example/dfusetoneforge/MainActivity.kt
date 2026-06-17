package com.example.dfusetoneforge

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dfusetoneforge.ui.theme.DfuseToneforgeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.HorizontalDivider

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
    var dfuseTapCount by remember { mutableIntStateOf(0) }
    var showDfuseMode by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    var linkText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }

    var startMs by remember { mutableLongStateOf(12_000L) }
    var endMs by remember { mutableLongStateOf(42_000L) }

    var waveformFile by remember { mutableStateOf<File?>(null) }
    var forgedFile by remember { mutableStateOf<File?>(null) }

  
    val messages = listOf(
        "Signal detected...",
        "Accessing Forge...",
        "Decrypting audio cores...",
        "Loading DFUSE protocol...",
        "Bypassing safeguards...",
        "System breach detected..."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .navigationBarsPadding()
                .blur(if (showDfuseMode) 20.dp else 0.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "DFUSE",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable {
                    dfuseTapCount++

                    if (dfuseTapCount < 7) {
                        Toast.makeText(
                            context,
                            messages[dfuseTapCount - 1],
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        dfuseTapCount = 0

                        Toast.makeText(
                            context,
                            "⚡ DFUSE MODE ACTIVATED ⚡",
                            Toast.LENGTH_LONG
                        ).show()

                        showDfuseMode = true
                    }
                }
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
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Downloads")
                }

                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Forge")
                }
            }

            HorizontalPager(
                state = pagerState,
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
                                statusText = "Paste a link first"
                                return@DownloadsPage
                            }

                            scope.launch {
                                try {
                                    isWorking = true
                                    statusText = "Downloading video/audio..."

                                    val rawFile = downloadYoutubeAudio(context, linkText)

                                    statusText = "Ripping audio..."

                                    val audioFile = extractAudioOnly(context, rawFile)

                                    waveformFile = audioFile
                                    forgedFile = null

                                    startMs = 12_000L
                                    endMs = 42_000L

                                    statusText = "Audio ready.\nLoaded: ${cleanDisplayName(audioFile)}"

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
                        statusText = statusText,
                        onEditAudioClick = {
                            waveformFile?.let { file ->
                                context.startActivity(
                                    Intent(
                                        context,
                                        AudioEditorActivity::class.java
                                    ).putExtra("audioPath", file.absolutePath)
                                )
                            }
                        },
                        onForgeClick = {
                            val audioFile = waveformFile

                            if (audioFile == null) {
                                statusText = "Download and rip audio first"
                                return@ForgePage
                            }

                            scope.launch {
                                try {
                                    isWorking = true
                                    statusText = "Forging ringtone..."

                                    val ringtone = forgeRingtone(
                                        context = context,
                                        inputFile = audioFile,
                                        startMs = startMs,
                                        endMs = endMs
                                    )

                                    val finalName =
                                        audioFile.nameWithoutExtension + "_ringtone.m4a"

                                    saveAudioToDownloads(
                                        context = context,
                                        sourceFile = ringtone,
                                        displayName = finalName
                                    )

                                    forgedFile = ringtone

                                    statusText =
                                        "Ringtone forged.\nSaved to Ringtones/DFUSE Tone Forge\nFile: $finalName"
                                } catch (e: Exception) {
                                    statusText = "Forge failed:\n${e.message}"
                                } finally {
                                    isWorking = false
                                }
                            }
                        }
                    )
                }
            }
        }

        if (showDfuseMode) {
            DfuseModeOverlay(
                onDismiss = { showDfuseMode = false }
            )
        }
    }
}
@Composable
fun DfuseModeOverlay(
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(3500)
        onDismiss()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "dfusePulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(420),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .scale(pulseScale)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color(0xFF9F6FFF),
                    spotColor = Color(0xFF9F6FFF)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xDD12091F)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DFUSE MODE ⚒️",
                    color = Color(0xFFC8A7FF),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SYSTEM ACCESS GRANTED",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(
                    color = Color(0x66C8A7FF)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "DKW | LKW | JKW",
                    color = Color(0xFFC8A7FF),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "DFUSE Tone Forge",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
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
            text = "Paste a link. DFUSE downloads it, rips audio, and loads it into the forge.",
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
                .height(56.dp)
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
    statusText: String,
    onEditAudioClick: () -> Unit,
    onForgeClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val audioInfo = remember(waveformFile) {
        readAudioInfo(waveformFile)
    }

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
            text = waveformFile?.let { cleanDisplayName(it) } ?: "No audio loaded yet",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        if (waveformFile != null) {
            AudioInfoCard(audioInfo = audioInfo)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onEditAudioClick,
                enabled = waveformFile != null,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "Edit Audio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Button(
                onClick = onForgeClick,
                enabled = waveformFile != null,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "Forge 🔨",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
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
fun AudioInfoCard(
    audioInfo: AudioInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.dfuse_cosmos),
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                AudioStat(
                    label = "Format",
                    value = audioInfo.format
                )

                Spacer(modifier = Modifier.height(12.dp))

                AudioStat(
                    label = "Duration",
                    value = audioInfo.duration
                )

                Spacer(modifier = Modifier.height(12.dp))

                AudioStat(
                    label = "Bitrate",
                    value = audioInfo.bitrate
                )
            }
        }
    }
}

@Composable
fun AudioStat(
    label: String,
    value: String
) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp
    )

    Text(
        text = value,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
}

data class AudioInfo(
    val format: String,
    val duration: String,
    val bitrate: String
)

fun readAudioInfo(file: File?): AudioInfo {
    if (file == null) {
        return AudioInfo(
            format = "--",
            duration = "0:00",
            bitrate = "--"
        )
    }

    val retriever = MediaMetadataRetriever()

    return try {
        retriever.setDataSource(file.absolutePath)

        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?: 0L

        val bitrateRaw = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            ?.toLongOrNull()

        val bitrateText =
            if (bitrateRaw != null && bitrateRaw > 0) {
                "${bitrateRaw / 1000} kbps"
            } else {
                "Unknown"
            }

        AudioInfo(
            format = file.extension.ifBlank { "audio" },
            duration = formatMs(durationMs),
            bitrate = bitrateText
        )
    } catch (e: Exception) {
        AudioInfo(
            format = file.extension.ifBlank { "audio" },
            duration = "0:00",
            bitrate = "Unknown"
        )
    } finally {
        retriever.release()
    }
}

private fun cleanDisplayName(file: File): String {
    return file.name
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