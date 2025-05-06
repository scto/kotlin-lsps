package org.kotlinlsp.analysis.modules

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
import org.kotlinlsp.common.read
import java.io.File

class SourceModule(
    val folderPath: String,
    override val dependencies: List<Module>,
    val javaVersion: JvmTarget,
    val kotlinVersion: LanguageVersion,
    val moduleName: String,
    private val mockProject: MockProject,
) : Module {
    override val id: String
        get() = moduleName
    override val isSourceModule: Boolean
        get() = true

    override fun computeFiles(): Sequence<VirtualFile> =
        File(folderPath)
            .walk()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .map { "file://${it.absolutePath}" }
            .mapNotNull { mockProject.read { VirtualFileManager.getInstance().findFileByUrl(it) } }

    override val kaModule: KaModule by lazy {
        object : KaSourceModule, KaModuleBase() {
            private val scope: GlobalSearchScope by lazy {
                val files = computeFiles()
                    .toList()

                GlobalSearchScope.filesScope(mockProject, files)
            }

            @KaPlatformInterface
            override val baseContentScope: GlobalSearchScope
                get() = scope
            override val directDependsOnDependencies: List<KaModule>
                get() = emptyList() // Not supporting KMP right now
            override val directFriendDependencies: List<KaModule>
                get() = emptyList() // No support for this right now
            override val directRegularDependencies: List<KaModule>
                get() = dependencies.map { it.kaModule }
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
    }
}
