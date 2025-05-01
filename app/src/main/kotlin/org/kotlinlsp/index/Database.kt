package org.kotlinlsp.index

import org.kotlinlsp.common.getCachePath
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.absolutePathString

fun createDbConnection(rootFolder: String): Connection {
    val cacheFolder = getCachePath(rootFolder)
    val connection = DriverManager.getConnection("jdbc:h2:${cacheFolder.resolve("index").absolutePathString()}")
    createTablesIfNeeded(connection)

    return connection
}

private fun createTablesIfNeeded(connection: Connection) {
    val createFileTable = """
        CREATE TABLE IF NOT EXISTS Files (
            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            path VARCHAR NOT NULL,
            packageFqName VARCHAR NOT NULL,
            lastModified TIMESTAMP NOT NULL
        );
        """.trimIndent()

    val createSymbolTable = """
        CREATE TABLE IF NOT EXISTS Symbols (
            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            file INT NOT NULL,
            startOffset INT NOT NULL,
            endOffset INT NOT NULL,
            name VARCHAR NOT NULL,
            kind INT NOT NULL,
            visibility INT NOT NULL,
            isStatic BOOLEAN NOT NULL,
            extensionFunctionSymbol INT,
            parentSymbol INT
        );
        """.trimIndent()

    val createReferencesTable = """
        CREATE TABLE IF NOT EXISTS References (
            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            symbolId INT NOT NULL,
            startOffset INT NOT NULL,
            endOffset INT NOT NULL
        );
    """.trimIndent()

    connection.createStatement().use {
        it.execute(createFileTable)
        it.execute(createSymbolTable)
        it.execute(createReferencesTable)
    }
}