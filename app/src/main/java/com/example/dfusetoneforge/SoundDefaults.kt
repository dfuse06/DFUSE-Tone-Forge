package com.example.dfusetoneforge

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings

private fun soundType(type: SaveAudioType): Int = when (type) {
    SaveAudioType.RINGTONE -> RingtoneManager.TYPE_RINGTONE
    SaveAudioType.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
    SaveAudioType.ALARM -> RingtoneManager.TYPE_ALARM
}

fun requestDefaultSound(context: Context, uri: Uri, type: SaveAudioType): String {
    val prefs = context.getSharedPreferences("pending_sound", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    if (Settings.System.canWrite(context)) {
        RingtoneManager.setActualDefaultRingtoneUri(context, soundType(type), uri)
        return "Default ${type.name.lowercase()} sound applied."
    }
    // Persist before launching so activity recreation does not lose the user's choice.
    prefs.edit().putString("uri", uri.toString()).putString("type", type.name).commit()
    try {
        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")))
    } catch (e: android.content.ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
        } catch (fallback: Exception) {
            prefs.edit().clear().commit()
            throw fallback
        }
    }
    return "Allow modify system settings, then return to Tone Forge to apply the sound."
}

fun resumePendingSound(context: Context): String? {
    val prefs = context.getSharedPreferences("pending_sound", Context.MODE_PRIVATE)
    val value = prefs.getString("uri", null) ?: return null
    val type = prefs.getString("type", null)
    // Consume only this explicitly requested action; do not apply later after a denial.
    prefs.edit().clear().commit()
    if (!Settings.System.canWrite(context)) return "Sound saved. Default unchanged because permission was not granted."
    return try {
        val choice = SaveAudioType.valueOf(requireNotNull(type))
        RingtoneManager.setActualDefaultRingtoneUri(context, soundType(choice), Uri.parse(value))
        "Default ${choice.name.lowercase()} sound applied."
    } catch (e: Exception) {
        "Sound saved. Could not apply default: ${e.message}"
    }
}
