package org.kotlinlsp.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

class MyLanguageServer: LanguageServer, TextDocumentService, WorkspaceService, LanguageClientAware {
    private lateinit var client: LanguageClient

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
        val diagnostic = Diagnostic(
            Range(Position(0, 0), Position(0, 5)),
            "Example error: Incorrect syntax",
            DiagnosticSeverity.Error,
            "MyLSP"
        )
        val diagnostics = listOf(diagnostic)
        client.publishDiagnostics(PublishDiagnosticsParams(params.textDocument.uri, diagnostics))
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

        client.logMessage(
            MessageParams(
                MessageType.Info,
                "KLSP: started successfully!"
            )
        )
    }
}
