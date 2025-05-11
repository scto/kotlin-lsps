package org.kotlinlsp.index

import org.kotlinlsp.analysis.modules.Module
import org.kotlinlsp.analysis.modules.getModuleList
import org.kotlinlsp.index.worker.WorkerThread
import java.util.concurrent.atomic.AtomicBoolean

class ScanFilesThread(
    private val worker: WorkerThread,
    private val rootModule: Module
) : Runnable {
    private val shouldStop = AtomicBoolean(false)

    override fun run() {

        // Scan phase
        rootModule.getModuleList()
            .filter { it.isSourceModule }
            .map { it.computeFiles() }
            .flatten()
            .takeWhile { !shouldStop.get() }
            .forEach {
                val command = Command.ScanSourceFile(it)
                worker.submitCommand(command)
            }

        worker.submitCommand(Command.SourceScanningFinished)

        // Once scanning is done and the analysis API is available, index all files (Index phase)
        rootModule.getModuleList()
            // Scan source files first as they will be more frequently accessed by the user
            .sortedByDescending { it.isSourceModule }
            .map { it.computeFiles() }
            .flatten()
            .takeWhile { !shouldStop.get() }
            .forEach {
                worker.submitCommand(Command.IndexFile(it))
            }

        worker.submitCommand(Command.IndexingFinished)
    }

    fun signalToStop() {
        shouldStop.set(true)
    }
}
