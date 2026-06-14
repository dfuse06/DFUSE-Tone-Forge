package com.example.dfusetoneforge

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@androidx.annotation.OptIn(UnstableApi::class)
suspend fun extractAudioOnly(
    context: Context,
    inputFile: File
): File {
    val outputDir = File(context.cacheDir, "audio")
    if (!outputDir.exists()) outputDir.mkdirs()

    val outputFile = File(
        outputDir,
        inputFile.nameWithoutExtension + "_audio.m4a"
    )

    if (outputFile.exists()) outputFile.delete()

    val mediaItem = MediaItem.fromUri(inputFile.toURI().toString())

    val editedMediaItem = EditedMediaItem.Builder(mediaItem)
        .setRemoveVideo(true)
        .build()

    return runTransformer(
        context = context,
        editedMediaItem = editedMediaItem,
        outputFile = outputFile
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
suspend fun forgeRingtone(
    context: Context,
    inputFile: File,
    startMs: Long,
    endMs: Long
): File {
    val outputDir = File(context.cacheDir, "forged")
    if (!outputDir.exists()) outputDir.mkdirs()

    val outputFile = File(
        outputDir,
        inputFile.nameWithoutExtension + "_ringtone.m4a"
    )

    if (outputFile.exists()) outputFile.delete()

    val mediaItem = MediaItem.Builder()
        .setUri(inputFile.toURI().toString())
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()
        )
        .build()

    val editedMediaItem = EditedMediaItem.Builder(mediaItem)
        .setRemoveVideo(true)
        .build()

    return runTransformer(
        context = context,
        editedMediaItem = editedMediaItem,
        outputFile = outputFile
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
private suspend fun runTransformer(
    context: Context,
    editedMediaItem: EditedMediaItem,
    outputFile: File
): File {
    return suspendCancellableCoroutine { continuation ->
        val transformer = Transformer.Builder(context)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(
                    composition: Composition,
                    exportResult: ExportResult
                ) {
                    android.util.Log.d("DFUSE", "Transformer complete: ${outputFile.absolutePath}")
                    continuation.resume(outputFile)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    android.util.Log.e("DFUSE", "Transformer failed", exportException)
                    continuation.resumeWithException(exportException)
                }
            })
            .build()

        transformer.start(editedMediaItem, outputFile.absolutePath)

        continuation.invokeOnCancellation {
            transformer.cancel()
        }
    }
}