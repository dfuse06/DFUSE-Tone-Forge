package com.example.dfusetoneforge

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive

private const val CACHE_MAGIC = 0x54465732
private const val WAVEFORM_BARS = 16_384
private val waveformMemory = object : LinkedHashMap<String, List<Float>>(8, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Float>>?): Boolean = size > 8
}

// Source identity and analysis version invalidate stale results without hashing the entire song.
internal fun waveformCacheKey(file: File): String {
    val identity = "envelope-fast-v3|${file.canonicalPath}|${file.length()}|${file.lastModified()}|$WAVEFORM_BARS"
    return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal fun readWaveformCache(file: File): List<Float>? = try {
    DataInputStream(file.inputStream().buffered()).use { input ->
        require(input.readInt() == CACHE_MAGIC)
        require(input.readInt() == WAVEFORM_BARS)
        val levels = List(WAVEFORM_BARS) {
            input.readFloat().also { value -> require(value.isFinite() && value in 0f..1f) }
        }
        require(input.read() == -1)
        levels
    }
} catch (_: Exception) { null }

internal fun writeWaveformCache(file: File, levels: List<Float>) {
    require(levels.size == WAVEFORM_BARS)
    val temporary = File.createTempFile("wave-", ".tmp", file.parentFile)
    try {
        DataOutputStream(temporary.outputStream().buffered()).use { output ->
            output.writeInt(CACHE_MAGIC)
            output.writeInt(levels.size)
            levels.forEach { value ->
                require(value.isFinite() && value in 0f..1f)
                output.writeFloat(value)
            }
        }
        check(temporary.renameTo(file)) { "Could not commit waveform cache" }
    } finally { temporary.delete() }
}

suspend fun loadCachedWaveform(
    cacheDir: File, audioPath: String?,
    onProgress: suspend (List<Float>, Float) -> Unit
): List<Float> = withContext(Dispatchers.IO) {
    if (audioPath == null) return@withContext emptyList()
    val source = File(audioPath)
    if (!source.isFile) return@withContext emptyList()
    val key = waveformCacheKey(source)
    synchronized(waveformMemory) { waveformMemory[key] }?.let { return@withContext it }
    val directory = File(cacheDir, "waveforms")
    val cacheAvailable = directory.isDirectory || directory.mkdirs()
    val cachedFile = File(directory, "$key.bin")
    if (cacheAvailable) {
        readWaveformCache(cachedFile)?.let {
            cachedFile.setLastModified(System.currentTimeMillis())
            synchronized(waveformMemory) { waveformMemory[key] = it }
            return@withContext it
        }
    }
    val levels = loadWaveformAmplitudes(audioPath, WAVEFORM_BARS, onProgress)
    ensureActive()
    // Failed, cancelled or changed-source analyses must never become a completed cache.
    if (levels.isNotEmpty() && waveformCacheKey(source) == key) {
        synchronized(waveformMemory) { waveformMemory[key] = levels }
        if (cacheAvailable) try {
            writeWaveformCache(cachedFile, levels)
            directory.listFiles()?.filter { it.extension == "bin" }
                ?.sortedByDescending { it.lastModified() }?.drop(64)?.forEach { it.delete() }
        } catch (e: Exception) {
            android.util.Log.w("ToneForge", "Waveform cache unavailable; using decoded result", e)
        }
    }
    levels
}

internal fun clearWaveformCache(cacheDir: File): Int {
    synchronized(waveformMemory) { waveformMemory.clear() }
    var deleted = 0
    File(cacheDir, "waveforms").listFiles()?.forEach { file ->
        if (file.isFile && (file.extension == "bin" || file.extension == "tmp")) {
            check(file.delete()) { "Could not delete ${file.name}" }
            deleted++
        }
    }
    return deleted
}
