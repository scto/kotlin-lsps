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
        var gotFirstLibraryFile = false

        rootModule.getModuleList()
            .sortedByDescending { it.isSourceModule }   // Scan source files first so initial analysis takes less time
            .map { it.computeFiles() }
            .flatten()
            .takeWhile { !shouldStop.get() }
            .forEach {
                if (!it.url.endsWith(".kt") && !gotFirstLibraryFile) {
                    worker.submitCommand(Command.SourceScanningFinished)
                    gotFirstLibraryFile = true
                }
                val command = Command.ScanFile(it)
                worker.submitCommand(command)
            }

        if (!gotFirstLibraryFile) worker.submitCommand(Command.SourceScanningFinished)

        worker.submitCommand(Command.ScanningFinished)

        // Once scanning is done and the analysis API is available, index all files
        rootModule.getModuleList()
            .sortedByDescending { it.isSourceModule }
            .map { it.computeFiles() }
            .flatten()
            .takeWhile { !shouldStop.get() }
            .forEach {
                worker.submitCommand(Command.IndexFile(it))
            }
    }

    fun signalToStop() {
        shouldStop.set(true)
    }
}
