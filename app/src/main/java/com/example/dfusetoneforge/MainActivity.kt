package com.example.dfusetoneforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dfusetoneforge.ui.theme.DfuseToneforgeTheme
import kotlinx.coroutines.launch
import java.io.File
import android.content.Intent

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
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            "dfuse_tone_forge_prefs",
            android.content.Context.MODE_PRIVATE
        )
    }

    var showDisclaimer by remember {
        mutableStateOf(!prefs.getBoolean("disclaimerAccepted", false))
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("⚒️ Welcome to the Forge") },
            text = {
                Text(
                    "Forge responsibly.\n\nOnly use audio from videos or content you own, created, or have permission to use. You are responsible for following copyright laws and platform terms."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit()
                            .putBoolean("disclaimerAccepted", true)
                            .apply()

                        showDisclaimer = false
                    }
                ) {
                    Text("Enter the Forge")
                }
            }
        )
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    var linkText by remember { mutableStateOf("") }
    var startMs by remember { mutableLongStateOf(12_000L) }
    var endMs by remember { mutableLongStateOf(42_000L) }
    val durationMs = 60_000L

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

                                    waveformFile = audioFile
                                    forgedFile = null
                                    startMs = 12_000L
                                    endMs = 42_000L

                                    statusText = "Audio ready.\nLoaded: ${audioFile.name}"

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
                                    android.content.Intent(
                                        context,
                                        AudioEditorActivity::class.java
                                    ).putExtra(
                                        "audioPath",
                                        file.absolutePath
                                    )
                                )
                            }
                        },
                        onForgeClick = {
                            if (waveformFile == null) {
                                statusText = "Download and rip audio first"
                                return@ForgePage
                            }

                            scope.launch {
                                try {
                                    isWorking = true
                                    statusText = "Forging ringtone..."

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
                        Text(
                            text = "Format",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "m4a",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Duration",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "3:47",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Bitrate",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "128 kbps",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }
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
                    text = "Forge \uD83D\uDD28",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ToneForgePreview() {
    DfuseToneforgeTheme {
        ToneForgeHome()
    }
}