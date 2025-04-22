package org.kotlinlsp.common

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

fun getCachePath(rootPath: String): Path {
    val home = System.getProperty("user.home")
    val baseCacheDir = Paths.get(home, ".cache", "kotlin-lsp")

    // This sanitize should work for the vast majority of cases
    val cachePath = baseCacheDir.resolve(sanitizePath(rootPath))

    if (!Files.exists(cachePath)) Files.createDirectories(cachePath)

    return cachePath
}

fun removeCacheFolder(rootPath: String) {
    val cachePath = getCachePath(rootPath).absolutePathString()
    val folder = Paths.get(cachePath)
    if (Files.exists(folder)) {
        Files.walk(folder)
            .sorted(Comparator.reverseOrder())
            .forEach { path -> Files.delete(path) }
    }
}

private fun sanitizePath(path: String): String {
    return path.replace(Regex("[^a-zA-Z0-9._/-]"), "_")
        .replace("/", "_")
        .replace("\\", "_")
}