package org.kotlinlsp

import java.io.File
import java.io.FileWriter

fun removeLogFile() {
    val logFile = File("/home/amg/Projects/kotlin-lsp/log.txt")
    if (logFile.exists()) {
        logFile.delete()
    }
}

fun log(message: String) {
    FileWriter(File("/home/amg/Projects/kotlin-lsp/log.txt"), true).use { it.appendLine(message) }
}
