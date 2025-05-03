package org.kotlinlsp.index.worker

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.common.info
import org.kotlinlsp.common.read
import org.kotlinlsp.index.Command
import org.kotlinlsp.index.IndexNotifier
import org.kotlinlsp.index.db.createDbConnection

class WorkerThread(
    private val rootFolder: String,
    private val project: Project,
    private val notifier: IndexNotifier
): Runnable {
    private val workQueue = WorkQueue<Command>()

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
                    notifier.onBackgroundIndexFinished()
                }
            }
        }

        connection.close()
    }

    fun submitCommand(command: Command) {
        // Edits queue has more priority than index queue
        when(command) {
            is Command.IndexModifiedFile -> {
                workQueue.putEditQueue(command)
            }
            else -> {
                workQueue.putIndexQueue(command)
            }
        }
    }
}
