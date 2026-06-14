package com.example.dfusetoneforge

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun downloadYoutubeAudio(
    context: Context,
    url: String
): File = withContext(Dispatchers.IO) {
    YoutubeDL.getInstance().init(context)

    val tempDir = File(context.cacheDir, "downloads")
    if (!tempDir.exists()) tempDir.mkdirs()

    val clients = listOf("web", "web_embedded", "tv", "ios", "android")
    var downloadedFile: File? = null
    var lastError: Exception? = null

    for (client in clients) {
        try {
            val request = YoutubeDLRequest(url)

            request.addOption("--no-update")
            request.addOption("--extractor-args", "youtube:player_client=$client")
            request.addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
            request.addOption("-o", "${tempDir.absolutePath}/%(title)s-$client.%(ext)s")

            YoutubeDL.getInstance().execute(request)

            downloadedFile = tempDir.listFiles()
                ?.maxByOrNull { it.lastModified() }

            if (downloadedFile != null) break
        } catch (e: Exception) {
            lastError = e
        }
    }

    downloadedFile ?: throw Exception(lastError?.message ?: "Download failed")
}