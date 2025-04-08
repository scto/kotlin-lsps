package org.kotlinlsp.buildsystem

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.kotlinlsp.analysis.services.modules.LibraryModule
import org.kotlinlsp.analysis.services.modules.SourceModule

fun getModuleList(project: MockProject): KaModule {
    // TODO Integrate with gradle, for now return a mock corresponding to the LSP project
    val javaVersion = JvmTarget.JVM_21
    val kotlinVersion = LanguageVersion.KOTLIN_2_1

    val kotlinStdlib = LibraryModule(
        mockProject = project,
        name = "org.jetbrains.kotlin:kotlin-stdlib:2.2.0-dev-7826",
        javaVersion = javaVersion,
        jarPath = "/home/amg/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.0-dev-7826/263622e6ba20bd16a41ed4602be30bce9145b0d7/kotlin-stdlib-2.2.0-dev-7826.jar",
        dependencies = listOf(
            LibraryModule(
                mockProject = project,
                name = "org.jetbrains:annotations:24.0.0",
                javaVersion = javaVersion,
                jarPath = "/home/amg/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/24.0.0/69b8b443c437fdeefa8d20c18d257b94836a92b9/annotations-24.0.0.jar",
            )
        )
    )

    val dependencies = listOf<KaModule>(
        kotlinStdlib
    )

    val rootPath = "/home/amg/Projects/kotlin-lsp"
    return SourceModule(
        moduleName = "main",
        mockProject = project,
        kotlinVersion = kotlinVersion,
        javaVersion = javaVersion,
        folderPath = "$rootPath/app/src/main",
        dependencies = dependencies
    )
}
