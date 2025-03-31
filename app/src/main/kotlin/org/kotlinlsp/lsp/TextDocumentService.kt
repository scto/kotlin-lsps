package org.kotlinlsp.lsp

class MyTextDocumentService : TextDocumentService {
    private var client: LanguageClient? = null

    fun setClient(client: LanguageClient) {
        this.client = client
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        val diagnostic = Diagnostic(
            Range(Position(0, 0), Position(0, 5)),
            "Example error: Incorrect syntax",
            DiagnosticSeverity.Error,
            "MyLSP"
        )
        val diagnostics = listOf(diagnostic)
        client?.publishDiagnostics(PublishDiagnosticsParams(params.textDocument.uri, diagnostics))
    }

    override fun didChange(params: DidChangeTextDocumentParams) {}

    override fun didClose(params: DidCloseTextDocumentParams) {}

    override fun didSave(params: DidSaveTextDocumentParams) {}
}
