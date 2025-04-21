package org.kotlinlsp.index

import com.intellij.mock.MockProject
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.analysis.services.modules.LibraryModule
import org.kotlinlsp.analysis.services.modules.SourceModule
import org.kotlinlsp.analysis.services.modules.id
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ScanFilesThread(
    private val worker: WorkerThread,
    private val rootModule: KaModule,
    private val project: MockProject
): Runnable {
    private val shouldStop = AtomicBoolean(false)

    @OptIn(ExperimentalTime::class)
    override fun run() {
        processModule(rootModule)
            .takeWhile { !shouldStop.get() }
            .mapNotNull {
                VirtualFileManager.getInstance().findFileByUrl("file://$it")
            }
            .mapNotNull {
                PsiManager.getInstance(project).findFile(it) as? KtFile
            }
            .forEach {
                worker.submitCommand(Command.IndexFile(it, Clock.System.now()))
            }
        worker.submitCommand(Command.IndexingFinished)
    }

    private fun processModule(module: KaModule, processedModules: MutableSet<String> = mutableSetOf()): Sequence<String> = sequence {
        if(processedModules.contains(module.id())) return@sequence

        when(module) {
            is SourceModule -> {
                File(module.folderPath)
                    .walk()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                    .forEach {
                        yield(it.absolutePath)
                    }
                module.directRegularDependencies.forEach {
                    yieldAll(processModule(it, processedModules))
                }
            }
            is LibraryModule -> {
                // TODO
            }
            else -> throw Exception("Unknown KaModule!")
        }

        processedModules.add(module.id())
    }

    fun signalToStop() {
        shouldStop.set(true)
    }
}