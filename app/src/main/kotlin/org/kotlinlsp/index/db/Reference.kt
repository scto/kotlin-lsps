package org.kotlinlsp.index.db

import java.sql.Statement

data class ReferenceRecord(
    val id: Int,
    val symbolId: Int,  // A symbol has multiple references to it
    val startOffset: Int,
    val endOffset: Int
)

fun Statement.createReferencesTable() {
    execute(
        """
        CREATE TABLE IF NOT EXISTS References (
            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            symbolId INT NOT NULL,
            startOffset INT NOT NULL,
            endOffset INT NOT NULL
        );
        """
    )
}
