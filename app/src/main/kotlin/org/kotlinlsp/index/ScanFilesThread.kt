package org.kotlinlsp.index

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.kotlinlsp.analysis.services.modules.LibraryModule
import org.kotlinlsp.analysis.services.modules.SourceModule
import org.kotlinlsp.analysis.services.modules.id
import org.kotlinlsp.index.worker.WorkerThread
import java.util.concurrent.atomic.AtomicBoolean

class ScanFilesThread(
    private val worker: WorkerThread,
    private val rootModule: KaModule
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

    private fun processModule(module: KaModule, processedModules: MutableSet<String> = mutableSetOf()): Sequence<VirtualFile> = sequence {
        if(processedModules.contains(module.id())) return@sequence

        when(module) {
            is SourceModule -> {
                yieldAll(module.computeFiles())

                module.directRegularDependencies.forEach {
                    yieldAll(processModule(it, processedModules))
                }
            }
            is LibraryModule -> {
                yieldAll(module.computeFiles())

                module.directRegularDependencies.forEach {
                    yieldAll(processModule(it, processedModules))
                }
            }
            else -> throw Exception("Unknown KaModule!")
        }

        processedModules.add(module.id())
    }

    fun signalToStop() {
        shouldStop.set(true)
    }
}