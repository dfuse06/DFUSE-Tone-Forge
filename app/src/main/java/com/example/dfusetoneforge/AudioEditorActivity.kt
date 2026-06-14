package com.example.dfusetoneforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dfusetoneforge.ui.theme.DfuseToneforgeTheme

class AudioEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation =
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller =
            androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)

        controller.hide(
            androidx.core.view.WindowInsetsCompat.Type.statusBars() or
                    androidx.core.view.WindowInsetsCompat.Type.navigationBars()
        )

        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE


        setContent {
            DfuseToneforgeTheme {
                AudioEditorScreen()
            }
        }
    }
}

@Composable
fun AudioEditorScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF090511)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TopEditorBar()

            TrackInfoRow()

            WaveformCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            BottomControls()
        }
    }
}

@Composable
fun TopEditorBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹ Back to Forge",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Audio Editor",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Drag handles to select start and end",
                color = Color(0xFFC8A7FF),
                fontSize = 15.sp
            )
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            OutlinedButton(onClick = {}) {
                Text("▷ Play Full Track")
            }
        }
    }
}

@Composable
fun TrackInfoRow() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF17121F)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(Color(0xFF5B21B6)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔨", fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "That supernova camo 😍 #bo7",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "...web_audio.m4a",
                    color = Color(0xFFB8AEC7),
                    fontSize = 15.sp
                )
            }

            StatBox("Duration", "3:47")
            StatBox("Format", "m4a")
            StatBox("Bitrate", "128 kbps")
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(
        modifier = Modifier.padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFFB8AEC7), fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WaveformCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF15101D)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            FakeWaveform(
                modifier = Modifier.fillMaxSize()
            )

            // selection handles placeholders
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(10.dp)
                    .offset(x = 170.dp)
                    .background(Color(0xFFC8A7FF))
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(10.dp)
                    .offset(x = 650.dp)
                    .background(Color(0xFFC8A7FF))
            )
        }
    }
}

@Composable
fun FakeWaveform(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerY = size.height / 2
        val bars = 160
        val barWidth = size.width / bars

        for (i in 0 until bars) {
            val heightPercent = when {
                i % 7 == 0 -> 0.85f
                i % 5 == 0 -> 0.65f
                i % 3 == 0 -> 0.45f
                else -> 0.30f
            }

            val barHeight = size.height * heightPercent
            val x = i * barWidth

            val color = if (i in 35..125) {
                Color(0xFFB178FF)
            } else {
                Color(0xFF5F5A6D)
            }

            drawLine(
                color = color,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun BottomControls() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = {}) {
            Text("Reset Selection")
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(onClick = {}) {
            Text("⏪")
        }

        Spacer(modifier = Modifier.width(18.dp))

        Button(
            onClick = {},
            modifier = Modifier.size(72.dp)
        ) {
            Text("▶")
        }

        Spacer(modifier = Modifier.width(18.dp))

        OutlinedButton(onClick = {}) {
            Text("⏩")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = {}) {
            Text("✓ Done")
        }
    }
}