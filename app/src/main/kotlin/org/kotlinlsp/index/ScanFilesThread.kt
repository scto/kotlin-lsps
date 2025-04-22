package org.kotlinlsp.index

import com.intellij.mock.MockProject
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.analysis.services.modules.LibraryModule
import org.kotlinlsp.analysis.services.modules.SourceModule
import org.kotlinlsp.analysis.services.modules.id
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ScanFilesThread(
    private val worker: WorkerThread,
    private val rootModule: KaModule,
    private val project: MockProject
): Runnable {
    private val shouldStop = AtomicBoolean(false)

    override fun run() {
        processModule(rootModule)
            .takeWhile { !shouldStop.get() }
            .forEach {
                val command = if(it.url.startsWith("file://")) {
                    val ktFile = PsiManager.getInstance(project).findFile(it) as KtFile
                    Command.IndexFile(ktFile)
                } else {
                    Command.IndexClassFile(it)
                }
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