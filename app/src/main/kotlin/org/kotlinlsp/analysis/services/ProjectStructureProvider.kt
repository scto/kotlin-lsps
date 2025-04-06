package org.kotlinlsp.analysis.services

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProviderBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.kotlinlsp.buildsystem.getModuleList
import org.kotlinlsp.log
import org.kotlinlsp.trace
import java.io.File

class ProjectStructureProvider: KotlinProjectStructureProviderBase() {
    private lateinit var mockProject: MockProject

    fun setup(project: MockProject) {
        this.mockProject = project
    }

    private val rootModule: KaModule by lazy {
        getModuleList(mockProject)
    }

    override fun getImplementingModules(module: KaModule): List<KaModule> {
        trace("getImplementingModules: $module")
        return emptyList()  // TODO
    }

    override fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule {
        trace("getModule: $element, useSiteModule: $useSiteModule")
        val virtualFile = element.containingFile.virtualFile
        return searchVirtualFileInModule(virtualFile, useSiteModule ?: rootModule)!!
    }

    private fun searchVirtualFileInModule(virtualFile: VirtualFile, module: KaModule): KaModule? {
        if(module.contentScope.contains(virtualFile)) return module

        for(it in module.directRegularDependencies) {
            val submodule = searchVirtualFileInModule(virtualFile, it)
            if(submodule != null) return submodule
        }
        return null
    }

    @OptIn(KaPlatformInterface::class)
    override fun getNotUnderContentRootModule(project: Project): KaNotUnderContentRootModule {
        trace("getNotUnderContentRootModule")
        throw Exception("unsupported")
    }
}

class SourceModule(
    private val mockProject: MockProject,
    private val folderPath: String,
    private val dependencies: List<KaModule>,
    private val javaVersion: JvmTarget,
    private val kotlinVersion: LanguageVersion,
    private val moduleName: String
) : KaSourceModule {
    private val scope: GlobalSearchScope by lazy {
        val files = File(folderPath)
            .walk()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .mapNotNull { VirtualFileManager.getInstance().findFileByUrl("file://${it.absolutePath}") }
            .toList()

        return@lazy GlobalSearchScope.filesScope(mockProject, files)
    }

    override val contentScope: GlobalSearchScope
        get() = scope

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
    override val transitiveDependsOnDependencies: List<KaModule>
        get() = emptyList() // Not supporting KMP right now
}
