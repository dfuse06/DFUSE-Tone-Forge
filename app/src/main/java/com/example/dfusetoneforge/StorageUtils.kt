package com.example.dfusetoneforge

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import android.os.Environment
import android.os.Build

enum class SaveAudioType {
    RINGTONE,
    NOTIFICATION,
    ALARM
}

fun saveAudioToDownloads(
    context: Context,
    sourceFile: File,
    displayName: String,
    type: SaveAudioType
): Uri {
    val resolver = context.contentResolver

    val cleanName = displayName
        .substringAfterLast("/")
        .removeSuffix(".mp4")
        .removeSuffix(".webm")
        .removeSuffix(".opus")
        .removeSuffix(".m4a") + ".m4a"

    val folder = when (type) {
        SaveAudioType.RINGTONE ->
            "${Environment.DIRECTORY_RINGTONES}/DFUSE Tone Forge/"

        SaveAudioType.NOTIFICATION ->
            "${Environment.DIRECTORY_NOTIFICATIONS}/DFUSE Tone Forge/"

        SaveAudioType.ALARM ->
            "${Environment.DIRECTORY_ALARMS}/DFUSE Tone Forge/"
    }

    if (Build.VERSION.SDK_INT < 29) {
        val directory = File(Environment.getExternalStorageDirectory(), folder)
        check(directory.isDirectory || directory.mkdirs()) { "Could not create sound folder" }
        val target = File(directory, "${System.currentTimeMillis()}_$cleanName")
        try {
            sourceFile.copyTo(target, overwrite = false)
            val legacy = ContentValues().apply {
                put(MediaStore.Audio.Media.DATA, target.absolutePath)
                put(MediaStore.Audio.Media.DISPLAY_NAME, target.name)
                put(MediaStore.Audio.Media.TITLE, cleanName.removeSuffix(".m4a"))
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.IS_MUSIC, false)
                put(MediaStore.Audio.Media.IS_RINGTONE, type == SaveAudioType.RINGTONE)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, type == SaveAudioType.NOTIFICATION)
                put(MediaStore.Audio.Media.IS_ALARM, type == SaveAudioType.ALARM)
            }
            return resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, legacy)
                ?: error("Could not register sound")
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, cleanName)
        put(MediaStore.Audio.Media.TITLE, cleanName.removeSuffix(".m4a"))
        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        put(MediaStore.Audio.Media.RELATIVE_PATH, folder)

        put(MediaStore.Audio.Media.IS_MUSIC, false)
        put(MediaStore.Audio.Media.IS_RINGTONE, type == SaveAudioType.RINGTONE)
        put(MediaStore.Audio.Media.IS_NOTIFICATION, type == SaveAudioType.NOTIFICATION)
        put(MediaStore.Audio.Media.IS_ALARM, type == SaveAudioType.ALARM)

        put(MediaStore.Audio.Media.IS_PENDING, 1)
    }

    val uri = resolver.insert(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        values
    ) ?: throw Exception("Could not create audio file")

    try {
        resolver.openOutputStream(uri, "w")?.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw Exception("Could not open output stream")

        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        android.util.Log.d("DFUSE", "Saved public audio: $uri")
        return uri
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        throw e
    }
}