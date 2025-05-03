package org.kotlinlsp.index.db

import java.sql.Connection
import java.sql.Statement
import java.time.Instant

data class FileRecord(
    val id: Int?,
    val path: String,
    val packageFqName: String,
    val lastModified: Instant,
    val modificationStamp: Long,
)

fun Statement.createFilesTable() {
    execute(
        """
        CREATE TABLE IF NOT EXISTS Files (
            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            path VARCHAR NOT NULL,
            packageFqName VARCHAR NOT NULL,
            lastModified TIMESTAMP NOT NULL,
            modificationStamp BIGINT NOT NULL
        );
        """
    )
}

fun Connection.queryFileRecord(path: String): FileRecord? {
    val query = "SELECT * FROM Files WHERE path = ?"
    prepareStatement(query).use {
        it.setString(1, path)
        it.executeQuery().use { rs ->
            if (!rs.next()) return null
            return FileRecord(
                id = rs.getInt("id"),
                path = rs.getString("path"),
                packageFqName = rs.getString("packageFqName"),
                lastModified = rs.getTimestamp("lastModified").toInstant(),
                modificationStamp = rs.getLong("modificationStamp")
            )
        }
    }
}

fun Connection.insertFileRecord(record: FileRecord) {
    val query = "INSERT INTO Files (path, packageFqName, lastModified, modificationStamp) VALUES (?, ?, ?, ?)"
    prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS).use { stmt ->
        stmt.setString(1, record.path)
        stmt.setString(2, record.packageFqName)
        stmt.setTimestamp(3, java.sql.Timestamp.from(record.lastModified))
        stmt.setLong(4, record.modificationStamp)
        stmt.executeUpdate()
    }
}

fun Connection.updateFileRecord(record: FileRecord) {
    val query = "UPDATE Files SET path = ?, packageFqName = ?, lastModified = ?, modificationStamp = ? WHERE id = ?"
    prepareStatement(query).use { stmt ->
        stmt.setString(1, record.path)
        stmt.setString(2, record.packageFqName)
        stmt.setTimestamp(3, java.sql.Timestamp.from(record.lastModified))
        stmt.setLong(4, record.modificationStamp)
        stmt.setInt(5, record.id!!)
        stmt.executeUpdate()
    }
}
