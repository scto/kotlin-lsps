package org.kotlinlsp.index

import com.intellij.openapi.vfs.VirtualFile
import org.kotlinlsp.analysis.services.modules.Module
import org.kotlinlsp.common.info
import org.kotlinlsp.index.worker.WorkerThread
import java.util.concurrent.atomic.AtomicBoolean

class ScanFilesThread(
    private val worker: WorkerThread,
    private val rootModule: Module
): Runnable {
    private val shouldStop = AtomicBoolean(false)

    override fun run() {
        getModuleList(rootModule)
            .sortedByDescending { it.isSourceModule }   // Index source files first so initial analysis takes less time
            .map { it.computeFiles() }
            .flatten()
            .takeWhile { !shouldStop.get() }
            .forEach {
                val command = Command.IndexFile(it)
                worker.submitCommand(command)
            }

        worker.submitCommand(Command.IndexingFinished)
    }

    private fun getModuleList(module: Module, processedModules: MutableSet<String> = mutableSetOf()): Sequence<Module> = sequence {
        if(processedModules.contains(module.id)) return@sequence

        yield(module)

        module.dependencies.forEach {
            yieldAll(getModuleList(it, processedModules))
        }

        processedModules.add(module.id)
    }

    fun signalToStop() {
        shouldStop.set(true)
    }
}