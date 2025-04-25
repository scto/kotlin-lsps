package org.kotlinlsp.buildsystem

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.kotlinlsp.analysis.services.modules.deserializeRootModule
import java.io.File

// This build system is used to integrate projects are not supported by the LSP
// Also used for testing purposes
class FileBasedBuildSystem(
    private val project: MockProject,
    private val appEnvironment: KotlinCoreApplicationEnvironment,
    private val rootFolder: String
): BuildSystem {
    override val markerFiles: List<String>
        get() = listOf("$rootFolder/.kotlinlsp-modules.json")

    override fun resolveRootModuleIfNeeded(cachedVersion: String?): Pair<KaModule, String>? {
        val file = File("$rootFolder/.kotlinlsp-modules.json")
        val currentVersion = file.lastModified()
        if(cachedVersion != null) {
            val cachedVersionLong = cachedVersion.toLong()
            if(currentVersion <= cachedVersionLong) return null
        }

        val contents = file.readText()
        val rootModule = deserializeRootModule(
            contents,
            mockProject = project,
            appEnvironment = appEnvironment
        )
        return Pair(rootModule, currentVersion.toString())
    }
}
