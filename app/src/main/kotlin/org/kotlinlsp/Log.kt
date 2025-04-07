package org.kotlinlsp

import java.io.File
import java.io.FileWriter

private enum class LogLevel(level: Int) {
    Trace(0),
    Debug(1),
    Warning(2),
    Error(3),
    Off(4)
}
private val logLevel = LogLevel.Error

fun removeLogFile() {
    val logFile = File("/home/amg/Projects/kotlin-lsp/log.txt")
    if (logFile.exists()) {
        logFile.delete()
    }
}

private fun log(message: String) {
    if(logLevel >= LogLevel.Off) return
    FileWriter(File("/home/amg/Projects/kotlin-lsp/log.txt"), true).use { it.appendLine(message) }
}

fun debug(message: String) {
    if(logLevel > LogLevel.Debug) return
    log("[DEBUG]: $message")
}

fun error(message: String) {
    if(logLevel > LogLevel.Error) return
    log("[ERROR]: $message")
}

fun trace(message: String) {
    if(logLevel > LogLevel.Trace) return
    log("[TRACE]: $message")
}

fun warn(message: String) {
    if(logLevel > LogLevel.Warning) return
    log("[WARN]: $message")
}
