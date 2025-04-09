package org.kotlinlsp.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import org.kotlinlsp.analysis.AnalysisSession
import org.kotlinlsp.info
import org.kotlinlsp.setupLogger
import org.kotlinlsp.trace
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

class MyLanguageServer: LanguageServer, TextDocumentService, WorkspaceService, LanguageClientAware {
    private lateinit var client: LanguageClient
    private lateinit var analysisSession: AnalysisSession

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        val capabilities = ServerCapabilities().apply {
            textDocumentSync = Either.forLeft(TextDocumentSyncKind.Incremental)
        }

        val rootPath = params.workspaceFolders.first().uri.removePrefix("file://")
        setupLogger(rootPath)
        info(rootPath)

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
        analysisSession.onOpenFile(params.textDocument.uri)
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        analysisSession.onChangeFile(params.textDocument.uri, params.textDocument.version, params.contentChanges)
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        analysisSession.onCloseFile(params.textDocument.uri)
    }

    override fun didSave(p0: DidSaveTextDocumentParams) {

    }

    override fun didChangeConfiguration(p0: DidChangeConfigurationParams) {

    }

    override fun didChangeWatchedFiles(p0: DidChangeWatchedFilesParams) {

    }

    override fun connect(p0: LanguageClient) {
        client = p0

        analysisSession = AnalysisSession(
            onDiagnostics = {
                client.publishDiagnostics(it)
            }
        )
    }
}
