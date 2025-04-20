package org.kotlinlsp.buildsystem

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import java.io.File

class BuildSystemResolver(
    project: MockProject,
    appEnvironment: KotlinCoreApplicationEnvironment,
    rootFolder: String
) {
    private val BUILD_SYSTEMS: List<BuildSystem> = listOf(
        FileBasedBuildSystem(project, appEnvironment, rootFolder),
        GradleBuildSystem()
    )

    fun resolveModuleDAG(): KaModule {
        // TODO Implement build system result caching
        BUILD_SYSTEMS.forEach {
            if(it.markerFiles.any { File(it).exists() }) {
                return it.resolveRootModuleIfNeeded(null)!!.first
            }
        }
        throw Exception("Not suitable build system found!")
    }
}
