package org.kotlinlsp.index

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.utils.getCachePath
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.absolutePathString

class Index(private val rootModule: KaModule, private val project: MockProject, private val rootFolder: String) {
    private lateinit var connection: Connection

    fun syncIndexInBackground() {
        val cacheFolder = getCachePath(rootFolder)
        connection = DriverManager.getConnection("jdbc:h2:${cacheFolder.resolve("index").absolutePathString()}")
        // TODO
    }

    fun queueOnFileChanged(ktFile: KtFile) {
        // TODO Update index entry for this file
    }

    fun close() {
        connection.close()
    }
}