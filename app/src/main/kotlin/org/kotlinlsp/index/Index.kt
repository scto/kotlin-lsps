package org.kotlinlsp.index

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.analysis.modules.Module
import org.kotlinlsp.common.read
import org.kotlinlsp.index.db.checkDbSchema
import org.kotlinlsp.index.db.createDbConnection
import org.kotlinlsp.index.worker.WorkerThread
import org.kotlinlsp.index.worker.WorkerThreadNotifier
import java.sql.Connection
import java.util.concurrent.CountDownLatch

interface IndexNotifier {
    fun onBackgroundIndexFinished()
}

class Index(
    rootModule: Module,
    private val project: Project,
    rootFolder: String,
    notifier: IndexNotifier
) {
    private val sourceFileIndexingFinishedSignal = CountDownLatch(1)
    private val workerThreadNotifier = object : WorkerThreadNotifier {
        override fun onSourceFileIndexingFinished() {
            sourceFileIndexingFinishedSignal.countDown()
        }

        override fun onBackgroundIndexFinished() = notifier.onBackgroundIndexFinished()
    }
    private val workerThreadRunner = WorkerThread(rootFolder, project, workerThreadNotifier)
    private val workerThread = Thread(workerThreadRunner)
    private val scanFilesThreadRunner = ScanFilesThread(workerThreadRunner, rootModule)
    private val scanFilesThread = Thread(scanFilesThreadRunner)
    private val dbConnection: Connection

    // This cache prevents parsing KtFiles over and over
    private val ktFileCache = Caffeine.newBuilder()
        .maximumSize(100)
        .build<String, KtFile>()

    init {
        checkDbSchema(rootFolder)   // This needs to be called before any connection is made
        dbConnection = createDbConnection(rootFolder)
    }

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
        workerThreadRunner.submitCommand(Command.IndexModifiedFile(ktFile))
    }

    fun close() {
        scanFilesThreadRunner.signalToStop()
        workerThreadRunner.submitCommand(Command.Stop)
        scanFilesThread.join()
        workerThread.join()
        dbConnection.close()
    }

    fun <T> query(block: (connection: Connection) -> T): T {
        sourceFileIndexingFinishedSignal.await()
        return block(dbConnection)
    }

    fun getKtFile(virtualFile: VirtualFile): KtFile? {
        val cachedKtFile = ktFileCache.getIfPresent(virtualFile.url)
        if(cachedKtFile != null) return cachedKtFile

        val ktFile = project.read { PsiManager.getInstance(project).findFile(virtualFile) as? KtFile } ?: return null
        ktFileCache.put(virtualFile.url, ktFile)
        return ktFile
    }
}
