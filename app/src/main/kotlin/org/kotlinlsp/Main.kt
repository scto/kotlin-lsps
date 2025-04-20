package org.kotlinlsp

import org.eclipse.lsp4j.launch.LSPLauncher
import org.kotlinlsp.lsp.MyLanguageServer
import org.kotlinlsp.utils.getLspVersion
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    if ("-v" in args || "--version" in args) {
        println(getLspVersion())
        return
    }

    val server = MyLanguageServer()
    val threads = Executors.newSingleThreadExecutor {
        Thread(it, "client")
    }
    val launcher = LSPLauncher.createServerLauncher(server, System.`in`, System.out, threads) { it }

    server.connect(launcher.remoteProxy)
    launcher.startListening()
}

