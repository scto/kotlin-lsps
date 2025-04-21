package org.kotlinlsp.setup

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.WorkspaceFolder
import org.eclipse.lsp4j.services.LanguageClient
import org.kotlinlsp.lsp.MyLanguageServer
import org.kotlinlsp.utils.removeCacheFolder
import org.mockito.Mockito.mock
import java.io.File
import java.nio.file.Paths

fun scenario(projectName: String, testCase: (server: MyLanguageServer, client: LanguageClient, projectUrl: String) -> Unit) {
    // Setup
    val cwd = Paths.get("").toAbsolutePath().toString()
    val jdkHome = System.getProperty("java.home")
    val moduleContents = """
            [
              {
                "id": "main",
                "dependencies": [
                    "JDK 21"
                ],
                "javaVersion": "21",
                "sourcePath": "$cwd/test-projects/$projectName",
                "kotlinVersion": "2.1"
              },
              {
                "id": "JDK 21",
                "dependencies": [],
                "javaVersion": "21",
                "isJdk": true,
                "libraryRoots": [
                  "$jdkHome"
                ]
              }
            ]
        """.trimIndent()
    val moduleFile = File("$cwd/test-projects/$projectName/.kotlinlsp-modules.json")
    moduleFile.delete()
    moduleFile.writeText(moduleContents)

    val server = MyLanguageServer(exitProcess = { /* no op */ })
    val initParams = InitializeParams().apply {
        workspaceFolders = listOf(
            WorkspaceFolder().apply {
                uri = "file://$cwd/test-projects/$projectName"
            }
        )
    }
    val client = mock(LanguageClient::class.java)
    server.initialize(initParams).join()
    server.initialized(InitializedParams())
    server.connect(client)

    // Run test case
    try {
        testCase(server, client, "file://$cwd/test-projects/$projectName")
    } finally {
        // Cleanup
        server.shutdown().join()
        moduleFile.delete()
        File("$cwd/test-projects/$projectName/log.txt").delete()
        removeCacheFolder("$cwd/test-projects/$projectName")
    }
}
