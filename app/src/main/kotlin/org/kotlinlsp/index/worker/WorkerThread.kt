package org.kotlinlsp.index.worker

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.common.info
import org.kotlinlsp.common.read
import org.kotlinlsp.index.Command
import org.kotlinlsp.index.IndexNotifier
import org.kotlinlsp.index.db.Database

interface WorkerThreadNotifier: IndexNotifier {
    fun onSourceFileIndexingFinished()
}

class WorkerThread(
    private val db: Database,
    private val project: Project,
    private val notifier: WorkerThreadNotifier
): Runnable {
    private val workQueue = WorkQueue<Command>()

    override fun run() {
        var count = 0

        while(true) {
            when(val command = workQueue.take()) {
                is Command.Stop -> break
                is Command.IndexFile -> {
                    if(command.virtualFile.url.startsWith("file://")) {
                        val ktFile = project.read { PsiManager.getInstance(project).findFile(command.virtualFile) } as KtFile
                        indexKtFile(project, ktFile, db)
                    } else {
                        indexClassFile(project, command.virtualFile, db)
                    }
                    count ++
                }
                is Command.IndexModifiedFile -> {
                    indexKtFile(project, command.ktFile, db)
                    count ++
                }
                is Command.IndexingFinished -> {
                    // TODO Should remove in this point files which do not exist anymore
                    info("Background indexing finished!, $count files!")
                    notifier.onBackgroundIndexFinished()
                }
                Command.SourceIndexingFinished -> {
                    // TODO Should remove in this point files which do not exist anymore
                    info("Source file indexing finished!, $count files!")
                    notifier.onSourceFileIndexingFinished()
                }
            }
        }
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
