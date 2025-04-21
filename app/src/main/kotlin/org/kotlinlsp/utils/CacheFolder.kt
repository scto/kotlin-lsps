package org.kotlinlsp.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun getCachePath(rootPath: String): Path {
    val home = System.getProperty("user.home")
    val baseCacheDir = Paths.get(home, ".cache", "kotlin-lsp")

    // This sanitize should work for the vast majority of cases
    val sanitizedPath = rootPath.replace(Regex("[^a-zA-Z0-9._/-]"), "_")
        .replace("/", "_")
        .replace("\\", "_")

    val cachePath = baseCacheDir.resolve(sanitizedPath)

    if (!Files.exists(cachePath)) Files.createDirectories(cachePath)

    return cachePath
}