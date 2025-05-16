package org.kotlinlsp.buildsystem

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import org.kotlinlsp.common.getCachePath
import java.io.ByteArrayOutputStream
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

    override fun resolveRootModuleIfNeeded(cachedMetadata: String?): Pair<Module, String?>? {
        val androidVariant = "debug"    // TODO Make it a config parameter
        if(!shouldReloadGradleProject(cachedMetadata)) {
            return null
        }

        progressNotifier.onReportProgress(WorkDoneProgressKind.begin, PROGRESS_TOKEN, "[GRADLE] Resolving project...")
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(File(rootFolder))
            .connect()

        val output = ByteArrayOutputStream()
        val androidInitScript = getAndroidInitScriptFile(rootFolder)
        val ideaProject = connection
            .model(IdeaProject::class.java)
            .withArguments("--init-script", androidInitScript.absolutePath, "-DandroidVariant=${androidVariant}")
            .setStandardOutput(output)
            .addProgressListener({
            progressNotifier.onReportProgress(WorkDoneProgressKind.report, PROGRESS_TOKEN, "[GRADLE] ${it.displayName}")
        }, OperationType.PROJECT_CONFIGURATION)
            .get()

        println(output)

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

        val metadata = Gson().toJson(computeGradleMetadata(ideaProject))
        connection.close()
        androidInitScript.delete()
        return Pair(rootModule, metadata)
    }
}

private fun computeGradleMetadata(project: IdeaProject): Map<String, Map<String, Long>> {
    val result = mutableMapOf<String, Map<String, Long>>()
    project.modules.forEach {
        val folder = it.contentRoots.first().rootDirectory
        result[folder.absolutePath] = getGradleFileTimestamps(folder)
    }
    return result
}

private fun getGradleFileTimestamps(dir: File): Map<String, Long> {
    if (!dir.isDirectory) return emptyMap()

    val fileNames = listOf(
        "settings.gradle",
        "settings.gradle.kts",
        "build.gradle",
        "build.gradle.kts",
        "gradle.properties"
    )

    val result = mutableMapOf<String, Long>()

    for (name in fileNames) {
        val file = File(dir, name)
        if (file.exists()) {
            result[name] = file.lastModified()
        }
    }

    return result
}

private fun shouldReloadGradleProject(metadataString: String?): Boolean {
    if(metadataString == null) return true
    val type = object : TypeToken<Map<String, Map<String, Long>>>() {}.type
    val metadata: Map<String, Map<String, Long>> = try {
        Gson().fromJson(metadataString, type)
    } catch(_: Throwable) {
        return true
    }

    metadata.forEach { (folder, timestamps) ->
        timestamps.forEach { (file, cachedTimestamp) ->
            val currentTimestamp = File(folder).resolve(file).lastModified()
            if(currentTimestamp > cachedTimestamp) return true
        }
    }

    return false
}

private fun getAndroidInitScriptFile(rootFolder: String): File {
    val inputStream = object {}.javaClass.getResourceAsStream("/android.init.gradle")
    val scriptFile = getCachePath(rootFolder).resolve(".android.init.gradle").toFile()
    scriptFile.delete()

    scriptFile.outputStream().use { out ->
        inputStream.copyTo(out)
    }

    return scriptFile
}
