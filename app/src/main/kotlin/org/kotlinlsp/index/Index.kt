package org.kotlinlsp.index

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtFile
import java.time.Instant
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class Index(
    rootModule: KaModule,
    project: MockProject,
    rootFolder: String
) {
    private val workerThreadRunner = WorkerThread(rootFolder)
    private val workerThread = Thread(workerThreadRunner)
    private val scanFilesThreadRunner = ScanFilesThread(workerThreadRunner, rootModule, project)
    private val scanFilesThread = Thread(scanFilesThreadRunner)

    fun syncIndexInBackground() {
        // We have 2 threads here
        // Scan files -> It scans files to index, loads them as a KtFile and submits them to a work queue
        // Worker     -> Takes each KtFile, fetches its symbols and writes them to the index database
        // The scan files thread will stop when the background indexing is done, while the worker is kept alive
        // for the duration of the server. It will keep receiving work whenever a KtFile is edited by the user so the
        // index is always up to date
        workerThread.start()
        scanFilesThread.start()
    }

    fun queueOnFileChanged(ktFile: KtFile) {
        workerThreadRunner.submitCommand(Command.IndexFile(ktFile))
    }

    fun close() {
        scanFilesThreadRunner.signalToStop()
        workerThreadRunner.submitCommand(Command.Stop)
        scanFilesThread.join()
        workerThread.join()
    }
}