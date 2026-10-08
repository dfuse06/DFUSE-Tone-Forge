package com.example.dfusetoneforge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dfusetoneforge.ui.theme.DfuseToneforgeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val message = withContext(Dispatchers.IO) { resumePendingSound(this@MainActivity) }
            message?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() }
        }
    }
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

    val prefs = context.getSharedPreferences(
        "dfuse_prefs",
        Context.MODE_PRIVATE
    )

    var showDisclaimer by remember {
        mutableStateOf(!prefs.getBoolean("disclaimerAccepted", false))
    }

    var showSaveChoice by remember { mutableStateOf(false) }
    var applyNow by remember { mutableStateOf(false) }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showSaveChoice = true
        else Toast.makeText(context, "Storage permission is needed to save sounds on this Android version.", Toast.LENGTH_LONG).show()
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "⚒️ Welcome to the Forge",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            },
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

    var dfuseTapCount by remember { mutableIntStateOf(0) }
    var showDfuseMode by remember { mutableStateOf(false) }
    var settingsUnlocked by remember { mutableStateOf(prefs.getBoolean("settingsUnlocked", false)) }
    var showSettings by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    var linkText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }

    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(0L) }

    var waveformFile by remember { mutableStateOf<File?>(null) }
    var forgedFile by remember { mutableStateOf<File?>(null) }
    var hasTrim by remember { mutableStateOf(false) }

    val editAudioLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->

                    startMs = data.getLongExtra("startMs", startMs)
                    endMs = data.getLongExtra("endMs", endMs)

                    hasTrim = true

                    statusText =
                        "Trim saved.\nStart: ${startMs / 1000}s\nEnd: ${endMs / 1000}s"
                }
            }
        }

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

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

                        prefs.edit().putBoolean("settingsUnlocked", true).apply()
                        settingsUnlocked = true
                        showDfuseMode = true
                    }
                }
            )

            Spacer(Modifier.weight(1f))
            if (settingsUnlocked) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                }
            }
            }
            Text(
                text = "Tone Forge 0.7 Beta",
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

                                    startMs = 0L
                                    val trackDuration = withContext(Dispatchers.IO) { readEditorAudioInfo(audioFile.absolutePath).durationMs }
                                    val presetSeconds = prefs.getInt("trimPresetSeconds", 0)
                                    endMs = if (presetSeconds > 0) minOf(trackDuration, presetSeconds * 1000L) else trackDuration
                                    hasTrim = endMs < trackDuration

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
                        hasTrim = hasTrim,
                        startMs = startMs,
                        endMs = endMs,
                        onEditAudioClick = {
                            if (isWorking) return@ForgePage
                            waveformFile?.let { file ->
                                editAudioLauncher.launch(
                                    Intent(
                                        context,
                                        AudioEditorActivity::class.java
                                    ).putExtra("audioPath", file.absolutePath)
                                        .putExtra("startMs", startMs)
                                        .putExtra("endMs", endMs)
                                )
                            }
                        },
                        onForgeClick = {
                            if (isWorking) return@ForgePage
                            if (waveformFile == null) {
                                statusText = "Download and rip audio first"
                                return@ForgePage
                            }

                            if (Build.VERSION.SDK_INT < 29 && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                storageLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else showSaveChoice = true
                        }
                    )
                }
            }
        }

        if (showSaveChoice) {
            SaveChoiceDialog(
                applyNow = applyNow,
                onApplyNowChange = { applyNow = it },
                onDismiss = { showSaveChoice = false },
                onSaveAsRingtone = {
                    showSaveChoice = false
                    forgeAndSaveAudio(
                        context = context,
                        scope = scope,
                        audioFile = waveformFile,
                        startMs = startMs,
                        endMs = endMs,
                        saveType = SaveAudioType.RINGTONE,
                        onStatus = { statusText = it },
                        onWorking = { isWorking = it },
                        onForged = { forgedFile = it },
                        applyNow = applyNow
                    )
                },
                onSaveAsNotification = {
                    showSaveChoice = false
                    forgeAndSaveAudio(
                        context = context,
                        scope = scope,
                        audioFile = waveformFile,
                        startMs = startMs,
                        endMs = endMs,
                        saveType = SaveAudioType.NOTIFICATION,
                        onStatus = { statusText = it },
                        onWorking = { isWorking = it },
                        onForged = { forgedFile = it },
                        applyNow = applyNow
                    )
                },
                onSaveAsAlarm = {
                    showSaveChoice = false
                    forgeAndSaveAudio(
                        context = context,
                        scope = scope,
                        audioFile = waveformFile,
                        startMs = startMs,
                        endMs = endMs,
                        saveType = SaveAudioType.ALARM,
                        onStatus = { statusText = it },
                        onWorking = { isWorking = it },
                        onForged = { forgedFile = it },
                        applyNow = applyNow
                    )
                }
            )
        }

        if (showSettings && !showDfuseMode) {
            ThemeSettingsDialog(onDismiss = { showSettings = false })
        }
        if (showDfuseMode) {
            DfuseModeOverlay(
                onDismiss = { showDfuseMode = false; showSettings = true }
            )
        }
    }
}

@Composable
fun SaveChoiceDialog(
    applyNow: Boolean,
    onApplyNowChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSaveAsRingtone: () -> Unit,
    onSaveAsNotification: () -> Unit,
    onSaveAsAlarm: () -> Unit
) {
    val context = LocalContext.current
    val preferred = context.getSharedPreferences("dfuse_prefs", Context.MODE_PRIVATE).getString("defaultSaveType", "RINGTONE")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save or set sound",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = applyNow, onCheckedChange = onApplyNowChange)
                    Text("Set as default now")
                }
                Text("Default alarm affects alarms using the default sound. Existing custom alarms and app notification channels keep their own sounds.", fontSize = 12.sp)
                listOf(
                    Triple("RINGTONE", "Ringtone", onSaveAsRingtone),
                    Triple("NOTIFICATION", "Notification", onSaveAsNotification),
                    Triple("ALARM", "Alarm", onSaveAsAlarm)
                ).sortedBy { if (it.first == preferred) 0 else 1 }.forEach { (id, label, save) ->
                    Button(onClick = save, modifier = Modifier.fillMaxWidth()) {
                        Text(if (preferred == id) "$label · preferred" else label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun forgeAndSaveAudio(
    context: Context,
    scope: CoroutineScope,
    audioFile: File?,
    startMs: Long,
    endMs: Long,
    saveType: SaveAudioType,
    onStatus: (String) -> Unit,
    onWorking: (Boolean) -> Unit,
    onForged: (File) -> Unit,
    applyNow: Boolean = false
) {
    if (audioFile == null) {
        onStatus("Download and rip audio first")
        return
    }

    scope.launch {
        try {
            onWorking(true)

            val typeText = when (saveType) {
                SaveAudioType.RINGTONE -> "ringtone"
                SaveAudioType.NOTIFICATION -> "notification"
                SaveAudioType.ALARM -> "alarm"
            }

            onStatus("Forging $typeText...")

            val forged = forgeRingtone(
                context = context,
                inputFile = audioFile,
                startMs = startMs,
                endMs = endMs
            )

            val finalName = audioFile.nameWithoutExtension + "_$typeText.m4a"

            val savedUri = withContext(Dispatchers.IO) { saveAudioToDownloads(
                context = context,
                sourceFile = forged,
                displayName = finalName,
                type = saveType
            ) }

            onForged(forged)

            val folderText = when (saveType) {
                SaveAudioType.RINGTONE -> "Ringtones"
                SaveAudioType.NOTIFICATION -> "Notifications"
                SaveAudioType.ALARM -> "Alarms"
            }

            val samsungNote =
                if (DeviceSupport.isSamsung) {
                    "\n\nSamsung Galaxy detected ✅"
                } else {
                    ""
                }

            onStatus(
                "Forged.\nSaved to $folderText/DFUSE Tone Forge\nFile: $finalName$samsungNote"
            )

            if (applyNow) {
                try {
                    val message = requestDefaultSound(context, savedUri, saveType)
                    onStatus("Saved to $folderText/DFUSE Tone Forge.\n$message")
                } catch (e: Exception) {
                    onStatus("Audio saved. Could not set default: ${e.message}")
                }
            }
            Toast.makeText(
                context,
                "Saved to $folderText/DFUSE Tone Forge",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            onStatus("Forge failed:\n${e.message}")
        } finally {
            onWorking(false)
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
    hasTrim: Boolean,
    startMs: Long,
    endMs: Long,
    onEditAudioClick: () -> Unit,
    onForgeClick: () -> Unit
){
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
            text = "Forge Audio",
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
            val displayInfo =
                if (hasTrim) {
                    audioInfo.copy(
                        duration = formatMs(endMs - startMs)
                    )
                } else {
                    audioInfo
                }

            AudioInfoCard(audioInfo = displayInfo)
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
        .replace("_notification", "")
        .replace("_alarm", "")
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
@Composable
private fun ThemeSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("dfuse_prefs", Context.MODE_PRIVATE) }
    var selected by remember { mutableStateOf(prefs.getString("appTheme", "purple") ?: "purple") }
    var preferred by remember { mutableStateOf(prefs.getString("defaultSaveType", "RINGTONE") ?: "RINGTONE") }
    var loop by remember { mutableStateOf(prefs.getBoolean("loopPreview", false)) }
    var preset by remember { mutableIntStateOf(prefs.getInt("trimPresetSeconds", 0)) }
    var bitrate by remember { mutableIntStateOf(prefs.getInt("exportBitrate", 192000)) }
    var fadeIn by remember { mutableIntStateOf(prefs.getInt("fadeInMs", 0)) }
    var fadeOut by remember { mutableIntStateOf(prefs.getInt("fadeOutMs", 0)) }
    var cacheBusy by remember { mutableStateOf(false) }
    var cacheStatus by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                }
                HorizontalDivider()
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text("Appearance", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("App theme", style = MaterialTheme.typography.headlineSmall)
                    Text("Applies immediately to the app and audio editor. Your choice is saved.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
                    listOf("purple" to "Forge Purple", "teal" to "Neon Teal", "ember" to "Ember", "blue" to "Electric Blue", "white" to "White").forEach { (id, name) ->
                        Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(Modifier.fillMaxWidth().clickable {
                                selected = id
                                prefs.edit().putString("appTheme", id).apply()
                            }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected == id, onClick = {
                                    selected = id
                                    prefs.edit().putString("appTheme", id).apply()
                                })
                                Text(name, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

                    SettingsSection("Saving")
                    Text("Preferred sound type")
                    SettingsChoices(listOf("RINGTONE" to "Ringtone", "NOTIFICATION" to "Notification", "ALARM" to "Alarm"), preferred) {
                        preferred = it; prefs.edit().putString("defaultSaveType", it).apply()
                    }
                    Text("Export format: M4A (AAC)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Export quality")
                    SettingsChoices(listOf("96000" to "Small · 96 kbps", "192000" to "Balanced · 192 kbps", "256000" to "High · 256 kbps"), bitrate.toString()) {
                        bitrate = it.toInt(); prefs.edit().putInt("exportBitrate", bitrate).apply()
                    }
                    SettingsSection("Editing & playback")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Loop selection preview", modifier = Modifier.weight(1f))
                        Switch(checked = loop, onCheckedChange = { loop = it; prefs.edit().putBoolean("loopPreview", it).apply() })
                    }
                    Text("Initial trim length for new tracks")
                    SettingsChoices(listOf("0" to "Full track", "5" to "5 seconds", "15" to "15 seconds", "30" to "30 seconds"), preset.toString()) {
                        preset = it.toInt(); prefs.edit().putInt("trimPresetSeconds", preset).apply()
                    }
                    Text("Fade in on export")
                    SettingsChoices(listOf("0" to "Off", "500" to "0.5 seconds", "1000" to "1 second", "2000" to "2 seconds"), fadeIn.toString()) {
                        fadeIn = it.toInt(); prefs.edit().putInt("fadeInMs", fadeIn).apply()
                    }
                    Text("Fade out on export")
                    SettingsChoices(listOf("0" to "Off", "500" to "0.5 seconds", "1000" to "1 second", "2000" to "2 seconds"), fadeOut.toString()) {
                        fadeOut = it.toInt(); prefs.edit().putInt("fadeOutMs", fadeOut).apply()
                    }
                    Text("Fades affect saved audio. Editor preview plays the original selection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SettingsSection("Storage & permissions")
                    OutlinedButton(enabled = !cacheBusy, onClick = {
                        cacheBusy = true
                        scope.launch {
                            try {
                                val count = withContext(Dispatchers.IO) { clearWaveformCache(context.cacheDir) }
                                cacheStatus = "Cleared $count cached waveforms. Audio files kept."
                            } catch (e: Exception) { cacheStatus = "Could not clear cache: ${e.message}" }
                            finally { cacheBusy = false }
                        }
                    }) { Text(if (cacheBusy) "Clearing…" else "Clear waveform cache") }
                    if (cacheStatus.isNotEmpty()) Text(cacheStatus)
                    OutlinedButton(onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
                        } catch (_: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, "Open your phone settings to allow modifying system settings.", Toast.LENGTH_LONG).show()
                        }
                    }) { Text("Sound-setting permission") }
                    Text("Allows Tone Forge to set default sounds when you choose Set as default now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
private fun SettingsChoices(choices: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        choices.forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == value, onClick = { onSelect(value) })
                Text(label)
            }
        }
    }
}
