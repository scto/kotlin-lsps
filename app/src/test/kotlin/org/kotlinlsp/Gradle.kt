package org.kotlinlsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.kotlinlsp.analysis.ProgressNotifier
import org.kotlinlsp.buildsystem.GradleBuildSystem
import org.mockito.Mockito.mock
import java.nio.file.Paths
import org.kotlinlsp.analysis.modules.Module

class Gradle {
    private val cwd = Paths.get("").toAbsolutePath().toString()

    private fun scenario(projectName: String, testCase: (GradleBuildSystem) -> Unit) {
        setupIdeaStandaloneExecution()
        val rootFolder = "$cwd/test-projects/$projectName"
        val project = mock(Project::class.java)
        val progressNotifier = mock(ProgressNotifier::class.java)
        val disposable = Disposer.newDisposable()
        val appEnvironment = KotlinCoreApplicationEnvironment.create(disposable, KotlinCoreApplicationEnvironmentMode.UnitTest)
        val buildSystem = GradleBuildSystem(
            project = project,
            progressNotifier = progressNotifier,
            rootFolder = rootFolder,
            appEnvironment = appEnvironment
        )

        try {
            testCase(buildSystem)
        } finally {
            disposable.dispose()
        }
    }

    private fun Module.firstContentRootFilename() = Paths.get(contentRoots.first()).fileName.toString()

    @Test
    fun `loads single module project successfully`() = scenario("single-module") { buildSystem ->
        // Act
        val (rootModule, _) = buildSystem.resolveRootModuleIfNeeded(cachedMetadata = null)!!

        // Assert
        assertEquals(rootModule.isSourceModule, true)
        assertEquals(rootModule.contentRoots.first(), "$cwd/test-projects/single-module/app")
        assertEquals(rootModule.dependencies.size, 3)
        rootModule.dependencies.forEach {
            assertEquals(it.dependencies.size, 0)
            assertEquals(it.contentRoots.size, 1)
        }
        val depNames = rootModule.dependencies.map { it.firstContentRootFilename() }.toSet()
        assertTrue("annotations-13.0.jar" in depNames)
        assertTrue("kotlin-stdlib-2.1.20.jar" in depNames)
    }

    @Test
    fun `loads android project successfully`() = scenario("android") { buildSystem ->
        // Act
        val (rootModule, _) = buildSystem.resolveRootModuleIfNeeded(cachedMetadata = null)!!
        // TODO
    }
}
