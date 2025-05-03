package org.kotlinlsp.index.db

import org.kotlinlsp.common.getCachePath
import org.kotlinlsp.common.info
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.absolutePathString

const val CURRENT_SCHEMA_VERSION = 1    // Increment on schema changes

fun createDbConnection(rootFolder: String): Connection =
    DriverManager.getConnection("jdbc:h2:${getDbPath(rootFolder)}")

private fun getDbPath(rootFolder: String): String =
    getCachePath(rootFolder).resolve("index").absolutePathString()

fun checkDbSchema(rootFolder: String) {
    val connection = createDbConnection(rootFolder)
    val schemaVersion = connection.createStatement().use {
        try {
            val resultSet = it.executeQuery("SELECT version FROM SchemaMetadata")
            return@use if (resultSet.next()) resultSet.getInt("version") else null
        } catch(_: Exception) {
            null
        }
    }

    if(schemaVersion != null && schemaVersion == CURRENT_SCHEMA_VERSION) return

    // Schema version mismatch, wipe the db and recreate the tables
    info("Index DB schema version mismatch, recreating!")
    connection.close()
    val dbPath = getDbPath(rootFolder)
    File("$dbPath.db").delete()
    File("$dbPath.trace.db").delete()
    File("$dbPath.mv.db").delete()

    createDb(rootFolder)
}

private fun createDb(rootFolder: String) {
    val connection = createDbConnection(rootFolder)
    connection.createStatement().use {
        it.execute(
            """
        CREATE TABLE IF NOT EXISTS SchemaMetadata (
            version INT NOT NULL
        );
        """
        )
        it.execute(
            """
        CREATE TABLE IF NOT EXISTS Files (
            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            path VARCHAR NOT NULL,
            packageFqName VARCHAR NOT NULL,
            lastModified TIMESTAMP NOT NULL
        );
        """
        )
        it.execute(
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
        it.execute(
            """
        CREATE TABLE IF NOT EXISTS References (
            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            symbolId INT NOT NULL,
            startOffset INT NOT NULL,
            endOffset INT NOT NULL
        );
        """
        )
        it.executeUpdate("INSERT INTO SchemaMetadata (version) VALUES ($CURRENT_SCHEMA_VERSION)")
    }
}