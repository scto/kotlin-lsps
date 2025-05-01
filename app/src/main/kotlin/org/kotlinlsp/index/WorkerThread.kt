package org.kotlinlsp.index

import org.kotlinlsp.common.info
import java.util.concurrent.ArrayBlockingQueue

class WorkerThread(private val rootFolder: String): Runnable {
    private val workQueue = ArrayBlockingQueue<Command>(100)

    override fun run() {
        val connection = createDbConnection(rootFolder)

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
