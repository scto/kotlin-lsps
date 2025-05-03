package org.kotlinlsp.index

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.kotlinlsp.analysis.services.modules.LibraryModule
import org.kotlinlsp.analysis.services.modules.Module
import org.kotlinlsp.analysis.services.modules.SourceModule
import org.kotlinlsp.index.worker.WorkerThread
import java.util.concurrent.atomic.AtomicBoolean

class ScanFilesThread(
    private val worker: WorkerThread,
    private val rootModule: Module
): Runnable {
    private val shouldStop = AtomicBoolean(false)

    override fun run() {
        processModule(rootModule)
            .takeWhile { !shouldStop.get() }
            .forEach {
                val command = Command.IndexFile(it)
                worker.submitCommand(command)
            }

        worker.submitCommand(Command.IndexingFinished)
    }

    private fun processModule(module: Module, processedModules: MutableSet<String> = mutableSetOf()): Sequence<VirtualFile> = sequence {
        if(processedModules.contains(module.id)) return@sequence

        yieldAll(module.computeFiles())

        module.dependencies.forEach {
            yieldAll(processModule(it, processedModules))
        }

        processedModules.add(module.id)
    }

    fun signalToStop() {
        shouldStop.set(true)
    }
}