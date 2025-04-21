package org.kotlinlsp.index

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtFile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class Index(private val rootModule: KaModule, private val project: MockProject, rootFolder: String) {
    private val workerThreadRunner = WorkerThread(rootFolder)
    private val workerThread = Thread(workerThreadRunner)

    fun syncIndexInBackground() {
        workerThread.start()
    }

    @OptIn(ExperimentalTime::class)
    fun queueOnFileChanged(ktFile: KtFile) {
        workerThreadRunner.submitCommand(Command.IndexFile(ktFile, Clock.System.now()))
    }

    fun close() {
        workerThreadRunner.submitCommand(Command.Stop)
        workerThread.join()
    }
}