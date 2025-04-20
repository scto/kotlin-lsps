package org.kotlinlsp.utils

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.analysis.services.modules.LibraryModule
import org.kotlinlsp.analysis.services.modules.SourceModule
import java.io.*
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.measureTime

private enum class LogLevel(level: Int) {
    Trace(0),
    Debug(1),
    Info(2),
    Warning(3),
    Error(4),
    Off(5)
}

// Configure as needed
private val logLevel = LogLevel.Trace
private const val profileEnabled = true

private lateinit var logFile: File
private val profileInfo = mutableMapOf<String, Duration>()

fun setupLogger(path: String) {
    val loggerPath = "$path/log.txt"
    logFile = File(loggerPath)
    if (logFile.exists()) {
        logFile.delete()
    }

    // This is to log the exceptions to log.txt file (JUL = java.util.log)
    Logger.getLogger("").addHandler(JULRedirector())

    // Also redirect stderr there (for analysis api logs)
    System.setErr(PrintStream(FileOutputStream(logFile)))
}

fun <T> profile(tag: String, message: String, fn: () -> T): T {
    trace("$tag $message")

    if(profileEnabled) {
        var result: T?
        val time = measureTime {
            result = fn()
        }
        if(!profileInfo.containsKey(tag)) {
            profileInfo[tag] = time
        } else {
            profileInfo[tag] = profileInfo[tag]!!.plus(time)
        }
        return result!!
    } else {
        return fn()
    }
}

fun logProfileInfo() {
    if (!profileEnabled) return

    log("------------")
    log("PROFILE INFO")
    profileInfo.entries.sortedByDescending { it.value }.forEach {
        log("${it.key}: ${it.value}")
    }
    log("------------")
    profileInfo.clear()
}

private fun log(message: String) {
    if(logLevel >= LogLevel.Off) return
    FileWriter(logFile, true).use {
        it.appendLine(message)
        it.flush()
    }
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
            info("$indent* ${rootModule.binaryRoots.first().absolutePathString().substringAfterLast("/")}")
        }

        else -> {
            throw Exception("Invalid KaModule!")
        }
    }
    rootModule.directRegularDependencies.forEach {
        printModule(it, level + 1)
    }
}

private class JULRedirector: Handler() {
    override fun publish(record: LogRecord) {
        when (record.level) {
            Level.SEVERE -> error(record.message)
            Level.WARNING -> warn(record.message)
            Level.INFO -> info(record.message)
            Level.CONFIG -> debug(record.message)
            Level.FINE -> trace(record.message)
            else -> trace(record.message)
        }

        val stackTrace = StringWriter().also { PrintWriter(it).use { pw -> record.thrown.printStackTrace(pw) } }.toString()
        error(stackTrace)
    }

    override fun flush() {}
    override fun close() {}
}

fun printPsiTree(ktFile: KtFile) {
    val rootNode = ktFile.node.psi

    printPsiNode(rootNode, 0)
}

fun printPsiNode(node: PsiElement, depth: Int = 0) {
    val indent = "  ".repeat(depth)
    debug("$indent${node.javaClass.simpleName}: ${node.text}")

    for (child in node.children) {
        printPsiNode(child, depth + 1)
    }
}