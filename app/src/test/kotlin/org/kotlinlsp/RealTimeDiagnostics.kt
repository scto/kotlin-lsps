package org.kotlinlsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.kotlinlsp.lsp.MyLanguageServer
import org.kotlinlsp.setup.scenario
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

class RealTimeDiagnostics {
    @Test
    fun `analyzes basic codebase with no error diagnostics`() = scenario("basic") { server, client, projectUrl ->
        // Act
        server.didOpen(DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                uri = "$projectUrl/Main.kt"
            }
        })

        // Assert
        verify(client).publishDiagnostics(argThat { !it.diagnostics.any { it.severity == DiagnosticSeverity.Error } })
    }

    @Test
    fun `analyzes basic codebase and reports syntax error`() = scenario("basic") { server, client, projectUrl ->
        // Act
        server.didOpen(DidOpenTextDocumentParams().apply {
            textDocument = TextDocumentItem().apply {
                uri = "$projectUrl/Errors.kt"
            }
        })

        // Assert
        verify(client).publishDiagnostics(argThat {
            it.diagnostics.any {
                it.severity == DiagnosticSeverity.Error && it.message == "Expecting a top level declaration"
            }
        })
    }
}
