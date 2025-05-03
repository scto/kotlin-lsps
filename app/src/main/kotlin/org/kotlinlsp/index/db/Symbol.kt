package org.kotlinlsp.index.db

import java.sql.Statement

// A symbol is a definition (e.g. variable, function...)
data class SymbolRecord(
    val id: Int,
    val file: Int,      // A symbol corresponds to a file
    val startOffset: Int,
    val endOffset: Int,
    val name: String,
    val kind: Int,  // TODO Define the possible values
    val parentSymbol: Int?
)

fun Statement.createSymbolsTable() {
    execute(
        """
        CREATE TABLE IF NOT EXISTS Symbols (
            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            file INT NOT NULL,
            startOffset INT NOT NULL,
            endOffset INT NOT NULL,
            name VARCHAR NOT NULL,
            kind INT NOT NULL,
            parentSymbol INT
        );
        """
    )
}
