package org.kotlinlsp.index.worker

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.common.info
import org.kotlinlsp.common.read
import org.kotlinlsp.index.Command
import org.kotlinlsp.index.db.createDbConnection
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.LinkedBlockingDeque

class WorkerThread(
    private val rootFolder: String,
    private val project: Project
): Runnable {
    companion object {
        const val INDEX_QUEUE_SIZE = 100
        const val EDITS_QUEUE_SIZE = 20
    }
    private val indexQueue = ArrayBlockingQueue<Command>(INDEX_QUEUE_SIZE)
    private val editsQueue = LinkedBlockingDeque<Command>(EDITS_QUEUE_SIZE)
    private val workQueue = LinkedBlockingDeque<Command>(INDEX_QUEUE_SIZE + EDITS_QUEUE_SIZE)

    override fun run() {
        val connection = createDbConnection(rootFolder)

        var count = 0

        while(true) {
            when(val command = workQueue.take()) {
                is Command.Stop -> break
                is Command.IndexFile -> {
                    if(command.virtualFile.url.startsWith("file://")) {
                        val ktFile = project.read { PsiManager.getInstance(project).findFile(command.virtualFile) } as KtFile
                        indexKtFile(project, ktFile, connection)
                    } else {
                        indexClassFile(project, command.virtualFile, connection)
                    }
                    count ++
                }
                is Command.IndexModifiedFile -> {
                    indexKtFile(project, command.ktFile, connection)
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
        // Edits queue has more priority than index queue
        when(command) {
            is Command.IndexModifiedFile -> {
                editsQueue.putFirst(command)
                workQueue.putFirst(editsQueue.take())
            }
            else -> {
                indexQueue.put(command)
                workQueue.putLast(indexQueue.take())
            }
        }
    }
}
