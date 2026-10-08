package com.example.dfusetoneforge.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

@Composable
fun DfuseToneforgeTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("dfuse_prefs", Context.MODE_PRIVATE) }
    var theme by remember(prefs) { mutableStateOf(prefs.getString("appTheme", "purple") ?: "purple") }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            if (key == "appTheme") theme = preferences.getString("appTheme", "purple") ?: "purple"
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val accent = when (theme) {
        "teal" -> Color(0xFF4FE1CE)
        "ember" -> Color(0xFFFFAD75)
        "blue" -> Color(0xFF9CCAFF)
        else -> Color(0xFFC8A7FF)
    }
    val background = when (theme) {
        "teal" -> Color(0xFF061310)
        "ember" -> Color(0xFF180D08)
        "blue" -> Color(0xFF080F1B)
        else -> Color(0xFF0E0916)
    }
    MaterialTheme(
        colorScheme = if (theme == "white") lightColorScheme(
            primary = Color(0xFF6440A4),
            secondary = Color(0xFF6440A4),
            tertiary = Color(0xFF006C61),
            onPrimary = Color.White,
            background = Color(0xFFFAFAFC),
            surface = Color.White,
            surfaceVariant = Color(0xFFECE8F2),
            onBackground = Color(0xFF1C1920),
            onSurface = Color(0xFF1C1920),
            onSurfaceVariant = Color(0xFF514B59)
        ) else darkColorScheme(
            primary = accent, secondary = accent, tertiary = accent,
            onPrimary = Color(0xFF151019), onSecondary = Color(0xFF151019),
            background = background, surface = background,
            surfaceVariant = accent.copy(red = accent.red * 0.18f, green = accent.green * 0.18f, blue = accent.blue * 0.18f),
            onBackground = Color.White, onSurface = Color.White,
            onSurfaceVariant = Color(0xFFCDC8D2)
        ),
        typography = Typography, content = content
    )
}
