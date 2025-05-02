package org.kotlinlsp.index.worker

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.common.info
import org.kotlinlsp.common.read
import org.kotlinlsp.index.Command
import org.kotlinlsp.index.db.createDbConnection
import java.util.concurrent.LinkedBlockingDeque

class WorkerThread(
    private val rootFolder: String,
    private val project: Project
): Runnable {
    private val workQueue = LinkedBlockingDeque<Command>(100)

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
        when(command) {
            // Incremental edits take priority over regular indexing operations
            // TODO Prevent blocking on IndexModifiedFile command if the queue gets full
            is Command.IndexModifiedFile -> workQueue.putFirst(command)
            else -> workQueue.putLast(command)
        }
    }
}
