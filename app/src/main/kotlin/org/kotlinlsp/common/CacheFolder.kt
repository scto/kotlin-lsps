package org.kotlinlsp.common

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

fun getCachePath(rootPath: String): Path {
    val cachePath = Paths.get(rootPath).resolve(".kotlin-lsp")

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
