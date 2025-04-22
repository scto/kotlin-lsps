package org.kotlinlsp.analysis.services.modules

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.io.File

class SourceModule(
    val folderPath: String,
    val dependencies: MutableList<KaModule>,
    val javaVersion: JvmTarget,
    val kotlinVersion: LanguageVersion,
    val moduleName: String,
    private val mockProject: MockProject,
) : KaSourceModule, KaModuleBase() {
    private val scope: GlobalSearchScope by lazy {
        val files = computeFiles()
            .toList()

        return@lazy GlobalSearchScope.filesScope(mockProject, files)
    }

    fun computeFiles(): Sequence<VirtualFile> =
        File(folderPath)
            .walk()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .map { "file://${it.absolutePath}" }
            .mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it) }

    @KaPlatformInterface
    override val baseContentScope: GlobalSearchScope
        get() = scope
    override val directDependsOnDependencies: List<KaModule>
        get() = emptyList() // Not supporting KMP right now
    override val directFriendDependencies: List<KaModule>
        get() = emptyList() // No support for this right now
    override val directRegularDependencies: List<KaModule>
        get() = dependencies
    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl(kotlinVersion, ApiVersion.createByLanguageVersion(kotlinVersion))

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Source module: $name"
    override val name: String
        get() = moduleName
    override val project: Project
        get() = mockProject
    override val targetPlatform: TargetPlatform
        get() = JvmPlatforms.jvmPlatformByTargetVersion(javaVersion)
}
