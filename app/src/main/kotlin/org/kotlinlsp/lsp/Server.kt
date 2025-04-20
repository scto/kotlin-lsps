package org.kotlinlsp.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import org.kotlinlsp.analysis.AnalysisSession
import org.kotlinlsp.utils.getLspVersion
import org.kotlinlsp.utils.info
import org.kotlinlsp.utils.setupLogger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import kotlin.system.exitProcess

class MyLanguageServer: LanguageServer, TextDocumentService, WorkspaceService, LanguageClientAware {
    private lateinit var client: LanguageClient
    private lateinit var analysisSession: AnalysisSession
    private lateinit var rootPath: String

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        val capabilities = ServerCapabilities().apply {
            textDocumentSync = Either.forLeft(TextDocumentSyncKind.Incremental)
            hoverProvider = Either.forLeft(true)
            definitionProvider = Either.forLeft(true)
        }
        val serverInfo = ServerInfo().apply {
            version = getLspVersion()
        }

        rootPath = params.workspaceFolders.first().uri.removePrefix("file://")

        return completedFuture(InitializeResult(capabilities, serverInfo))
    }

    override fun initialized(params: InitializedParams) {
        setupLogger(rootPath)
        info(rootPath)

        analysisSession = AnalysisSession(
            onDiagnostics = {
                client.publishDiagnostics(it)
            },
            rootPath = rootPath
        )
    }

    override fun shutdown(): CompletableFuture<Any> {
        exit()  // TODO Nvim does not call exit so the server is kept alive and reparented to the init process (?)
        return completedFuture(null)
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

    override fun didSave(params: DidSaveTextDocumentParams) {

    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {

    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {

    }

    override fun connect(params: LanguageClient) {
        client = params
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover?> {
        // TODO Add javadoc
        val hoverResult = analysisSession.hover(params.textDocument.uri, params.position) ?: return completedFuture(null)
        val content = MarkupContent().apply {
            kind = "markdown"
            value = "```kotlin\n${hoverResult!!.first}\n"
        }

        val hover = Hover().apply {
            contents = Either.forRight(content)
            range = hoverResult.second
        }

        return completedFuture(hover)
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<MutableList<out Location>, MutableList<out LocationLink>>?> {
        val location = analysisSession.goToDefinition(params.textDocument.uri, params.position) ?: return completedFuture(null)
        return completedFuture(Either.forLeft(mutableListOf(location)))
    }
}
