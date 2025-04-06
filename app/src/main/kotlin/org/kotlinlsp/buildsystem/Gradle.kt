package org.kotlinlsp.buildsystem

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.kotlinlsp.analysis.services.SourceModule

fun getModuleList(project: MockProject): KaModule {
    // TODO Integrate with gradle, for now return a mock corresponding to the LSP project
    val rootPath = "/home/amg/Projects/kotlin-lsp"
    return SourceModule(
        moduleName = "main",
        mockProject = project,
        kotlinVersion = LanguageVersion.KOTLIN_2_1,
        javaVersion = JvmTarget.JVM_21,
        folderPath = "$rootPath/app/src/main",
        dependencies = emptyList()
    )
}
