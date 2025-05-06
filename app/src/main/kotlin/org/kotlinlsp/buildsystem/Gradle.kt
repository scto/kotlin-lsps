package org.kotlinlsp.buildsystem

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.WorkDoneProgressKind
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.kotlinlsp.analysis.ProgressNotifier
import org.kotlinlsp.analysis.modules.LibraryModule
import org.kotlinlsp.analysis.modules.Module
import org.kotlinlsp.analysis.modules.SourceModule
import java.io.File

class GradleBuildSystem(
    private val project: Project,
    private val appEnvironment: KotlinCoreApplicationEnvironment,
    private val rootFolder: String,
    private val progressNotifier: ProgressNotifier
) : BuildSystem {
    companion object {
        const val PROGRESS_TOKEN = "GradleBuildSystem"
    }

    override val markerFiles: List<String> = listOf(
        "$rootFolder/build.gradle", "$rootFolder/build.gradle.kts",
        "$rootFolder/settings.gradle", "$rootFolder/settings.gradle.kts",
    )

    override fun resolveRootModuleIfNeeded(cachedVersion: String?): Pair<Module, String?> {
        // TODO Implement caching checks
        progressNotifier.onReportProgress(WorkDoneProgressKind.begin, PROGRESS_TOKEN, "[GRADLE] Resolving project...")
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(rootFolder))
            .connect()

        val model = connection.model(IdeaProject::class.java)

        model.addProgressListener({
            progressNotifier.onReportProgress(WorkDoneProgressKind.report, PROGRESS_TOKEN, "[GRADLE] ${it.displayName}")
        }, OperationType.PROJECT_CONFIGURATION)

        val ideaProject = model.get()

        val jvmTarget = checkNotNull(JvmTarget.fromString(ideaProject.jdkName)) { "Unknown jdk target" }
        val jdkModule = ideaProject.javaLanguageSettings?.jdk?.let { jdk ->
            LibraryModule(
                id = "JDK ${jvmTarget.description}",
                appEnvironment = appEnvironment,
                project = project,
                roots = listOf(jdk.javaHome.toPath()),
                javaVersion = jvmTarget,
                isJdk = true,
            )
        }

        val modules = ideaProject.modules.map { module ->
            val dependencies = module
                .dependencies
                .filterIsInstance<IdeaSingleEntryLibraryDependency>()
                .map { dependency ->
                    LibraryModule(
                        id = dependency.file.name,
                        appEnvironment = appEnvironment,
                        project = project,
                        javaVersion = jvmTarget,
                        roots = listOf(dependency.file.toPath()),
                    )
                }

            val allDependencies: MutableList<Module> = dependencies.toMutableList()
            if (jdkModule != null) {
                allDependencies.add(jdkModule)
            }

            SourceModule(
                id = module.name,
                project = project,
                folderPath = module.contentRoots.first().rootDirectory.path,
                dependencies = allDependencies,
                javaVersion = jvmTarget,
                kotlinVersion = LanguageVersion.KOTLIN_2_1,
            )
        }

        // TODO Support multiple modules, for now take the last one
        val rootModule = modules.last()
        progressNotifier.onReportProgress(WorkDoneProgressKind.end, PROGRESS_TOKEN, "[GRADLE] Done")
        return Pair(rootModule, null)
    }
}
