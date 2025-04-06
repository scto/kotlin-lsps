package org.kotlinlsp

import java.io.File
import java.io.FileWriter

private enum class LogLevel(level: Int) {
    Trace(0),
    Warning(1),
    Off(2)
}
private val logLevel = LogLevel.Trace

fun removeLogFile() {
    val logFile = File("/home/amg/Projects/kotlin-lsp/log.txt")
    if (logFile.exists()) {
        logFile.delete()
    }
}

fun log(message: String) {
    if(logLevel >= LogLevel.Off) return
    FileWriter(File("/home/amg/Projects/kotlin-lsp/log.txt"), true).use { it.appendLine(message) }
}

fun trace(message: String) {
    if(logLevel > LogLevel.Trace) return
    log("[TRACE]: $message")
}

fun warn(message: String) {
    if(logLevel > LogLevel.Warning) return
    log("[WARN]: $message")
}
