package com.example.dfusetoneforge

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

fun saveAudioToDownloads(
    context: Context,
    sourceFile: File,
    displayName: String
): Uri {
    val resolver = context.contentResolver

    val cleanName = displayName
        .substringAfterLast("/")
        .removeSuffix(".mp4")
        .removeSuffix(".webm")
        .removeSuffix(".opus")
        .removeSuffix(".m4a") + ".m4a"

    val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, cleanName)
        put(MediaStore.Audio.Media.TITLE, cleanName.removeSuffix(".m4a"))
        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        put(MediaStore.Audio.Media.RELATIVE_PATH, "Ringtones/DFUSE Tone Forge/")
        put(MediaStore.Audio.Media.IS_MUSIC, true)
        put(MediaStore.Audio.Media.IS_PENDING, 1)
    }

    val uri = resolver.insert(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        values
    ) ?: throw Exception("Could not create audio file in Downloads")

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