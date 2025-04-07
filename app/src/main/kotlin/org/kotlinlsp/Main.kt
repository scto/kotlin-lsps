package org.kotlinlsp

import org.eclipse.lsp4j.launch.LSPLauncher
import org.kotlinlsp.lsp.MyLanguageServer
import java.util.concurrent.Executors

fun main() {
    removeLogFile()
    val server = MyLanguageServer()
    val threads = Executors.newSingleThreadExecutor {
        Thread(it, "client")
    }
    val launcher = LSPLauncher.createServerLauncher(server, System.`in`, System.out, threads) { it }

    server.connect(launcher.remoteProxy)
    launcher.startListening()
}
