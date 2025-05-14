package org.kotlinlsp.buildsystem

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.kotlinlsp.analysis.modules.Module
import org.kotlinlsp.analysis.modules.deserializeRootModule
import java.io.File

// This build system is used to integrate projects are not supported by the LSP
// Also used for testing purposes
class FileBasedBuildSystem(
    private val project: Project,
    private val appEnvironment: KotlinCoreApplicationEnvironment,
    private val rootFolder: String
): BuildSystem {
    override val markerFiles: List<String>
        get() = listOf("$rootFolder/.kotlinlsp-modules.json")

    override fun resolveRootModuleIfNeeded(cachedMetadata: String?): Pair<Module, String>? {
        val file = File("$rootFolder/.kotlinlsp-modules.json")
        val currentVersion = file.lastModified()
        if(cachedMetadata != null) {
            val cachedVersionLong = cachedMetadata.toLong()
            if(currentVersion <= cachedVersionLong) return null
        }

        val contents = file.readText()
        val rootModule = deserializeRootModule(
            contents,
            project = project,
            appEnvironment = appEnvironment
        )
        return Pair(rootModule, currentVersion.toString())
    }
}
