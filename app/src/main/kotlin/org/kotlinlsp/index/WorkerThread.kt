package org.kotlinlsp.index

import org.kotlinlsp.common.getCachePath
import org.kotlinlsp.common.info
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.ArrayBlockingQueue
import kotlin.io.path.absolutePathString

class WorkerThread(private val rootFolder: String): Runnable {
    private val workQueue = ArrayBlockingQueue<Command>(100)

    override fun run() {
        val cacheFolder = getCachePath(rootFolder)
        val connection = DriverManager.getConnection("jdbc:h2:${cacheFolder.resolve("index").absolutePathString()}")
        createTablesIfNeeded(connection)

        var count = 0

        while(true) {
            when(val command = workQueue.take()) {
                is Command.Stop -> break
                is Command.IndexFile -> {
                    // TODO
                    count ++
                }
                is Command.IndexClassFile -> {
                    // TODO
                    count ++
                }
                is Command.IndexingFinished -> {
                    info("Background indexing finished!, $count files!")
                }
            }
        }

        connection.close()
    }

    fun submitCommand(command: Command) {
        workQueue.put(command)
    }
}

private fun createTablesIfNeeded(connection: Connection) {
    val createFileTable = """
        CREATE TABLE IF NOT EXISTS File (
            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
            path VARCHAR NOT NULL,
            packageFqName VARCHAR NOT NULL,
            timestamp TIMESTAMP NOT NULL
        );
        """.trimIndent()

    val createSymbolTable = """
        CREATE TABLE IF NOT EXISTS Symbol (
            file INT NOT NULL,
            startOffset INT NOT NULL,
            endOffset INT NOT NULL,
            origin_file INT,
            origin_startOffset INT,
            origin_endOffset INT,
            PRIMARY KEY (file, startOffset, endOffset),
            FOREIGN KEY (file) REFERENCES File(id),
            FOREIGN KEY (origin_file, origin_startOffset, origin_endOffset)
                REFERENCES Symbol(file, startOffset, endOffset)
        );
        """.trimIndent()

    connection.createStatement().use { stmt ->
        stmt.execute(createFileTable)
        stmt.execute(createSymbolTable)
    }
}