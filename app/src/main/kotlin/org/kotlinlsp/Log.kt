package org.kotlinlsp

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.kotlinlsp.analysis.services.modules.LibraryModule
import org.kotlinlsp.analysis.services.modules.SourceModule
import java.io.File
import java.io.FileWriter

private enum class LogLevel(level: Int) {
    Trace(0),
    Debug(1),
    Info(2),
    Warning(3),
    Error(4),
    Off(5)
}
private val logLevel = LogLevel.Trace
private lateinit var loggerPath: String

fun setupLogger(path: String) {
    loggerPath = "$path/log.txt"
    val logFile = File(loggerPath)
    if (logFile.exists()) {
        logFile.delete()
    }
}

private fun log(message: String) {
    if(logLevel >= LogLevel.Off) return
    FileWriter(File(loggerPath), true).use { it.appendLine(message) }
}

fun debug(message: String) {
    if(logLevel > LogLevel.Debug) return
    log("[DEBUG]: $message")
}

fun info(message: String) {
    if(logLevel > LogLevel.Info) return
    log("[INFO]: $message")
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

fun printModule(rootModule: KaModule, level: Int = 0) {
    val indent = "\t".repeat(level)
    when (rootModule) {
        is SourceModule -> {
            info("$indent- ${rootModule.name}")
        }

        is LibraryModule -> {
            info("$indent- ${rootModule.libraryName}")
            info("$indent* ${rootModule.jarPath.substringAfterLast("/")}")
        }

        else -> {
            throw Exception("Invalid KaModule!")
        }
    }
    rootModule.directRegularDependencies.forEach {
        printModule(it, level + 1)
    }
}
