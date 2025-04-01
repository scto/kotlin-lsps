package org.kotlinlsp.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import org.kotlinlsp.analysis.AnalysisSession
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

class MyLanguageServer: LanguageServer, TextDocumentService, WorkspaceService, LanguageClientAware {
    private lateinit var client: LanguageClient
    private lateinit var analysisSession: AnalysisSession

    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
        val capabilities = ServerCapabilities().apply {
            textDocumentSync = Either.forLeft(TextDocumentSyncKind.Incremental)
        }

        return CompletableFuture.completedFuture(InitializeResult(capabilities))
    }

    override fun shutdown(): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(Unit)
    }

    override fun exit() {
        exitProcess(0)
    }

    override fun getTextDocumentService(): TextDocumentService = this
    override fun getWorkspaceService(): WorkspaceService = this

    override fun didOpen(params: DidOpenTextDocumentParams) {
        log("Opened file: ${params.textDocument.uri}")
        analysisSession.onOpenFile(params.textDocument.uri)
    }

    override fun didChange(p0: DidChangeTextDocumentParams) {

    }

    override fun didClose(p0: DidCloseTextDocumentParams) {

    }

    override fun didSave(p0: DidSaveTextDocumentParams) {

    }

    override fun didChangeConfiguration(p0: DidChangeConfigurationParams) {

    }

    override fun didChangeWatchedFiles(p0: DidChangeWatchedFilesParams) {

    }

    override fun connect(p0: LanguageClient) {
        client = p0

        analysisSession = AnalysisSession {
            client.publishDiagnostics(it)
        }

        log("Started successfully!")
    }

    private fun log(message: String) {
        client.logMessage(
            MessageParams(
                MessageType.Info,
                "KLSP: $message"
            )
        )
    }
}
