package org.kotlinlsp.common

import java.io.File

fun findSourceFiles(rootFolder: String): Sequence<String> =
    File(rootFolder)
        .walk()
        .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
        .map { it.absolutePath }
