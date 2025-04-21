package org.kotlinlsp

import org.eclipse.lsp4j.launch.LSPLauncher
import org.kotlinlsp.lsp.MyLanguageServer
import org.kotlinlsp.utils.getLspVersion
import org.kotlinlsp.utils.profileJvmStartup
import java.util.concurrent.Executors
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    profileJvmStartup()
    if ("-v" in args || "--version" in args) {
        println(getLspVersion())
        return
    }

    val server = MyLanguageServer(exitProcess = { exitProcess(0) })
    val threads = Executors.newSingleThreadExecutor {
        Thread(it, "client")
    }
    val launcher = LSPLauncher.createServerLauncher(server, System.`in`, System.out, threads) { it }

    server.connect(launcher.remoteProxy)
    launcher.startListening()
}
