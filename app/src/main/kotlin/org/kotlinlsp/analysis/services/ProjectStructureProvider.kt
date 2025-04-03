package org.kotlinlsp.analysis.services

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProviderBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.log

class ProjectStructureProvider: KotlinProjectStructureProviderBase() {
    private lateinit var mockProject: MockProject
    private lateinit var ktFiles: List<KtFile>
    private lateinit var virtualFiles: List<VirtualFile>

    fun setup(project: MockProject, ktFiles: List<KtFile>) {
        this.mockProject = project
        this.ktFiles = ktFiles
        this.virtualFiles = ktFiles.map { it.virtualFile }
    }

    fun getKtFile(path: String): KtFile? = ktFiles.find { "file://${it.virtualFilePath}" == path }

    @KaPlatformInterface
    private val mainModule = object : KaSourceModule {
        override val contentScope: GlobalSearchScope
            get() = GlobalSearchScope.filesScope(mockProject, virtualFiles)
        override val baseContentScope: GlobalSearchScope
            get() = GlobalSearchScope.filesScope(mockProject, virtualFiles)
        override val directDependsOnDependencies: List<KaModule>
            get() = emptyList()
        override val directFriendDependencies: List<KaModule>
            get() = emptyList()
        override val directRegularDependencies: List<KaModule>
            get() = emptyList() // This has libraries and SDKs
        override val languageVersionSettings: LanguageVersionSettings
            get() = LanguageVersionSettingsImpl.DEFAULT

        @KaExperimentalApi
        override val moduleDescription: String
            get() = "Main module desc"
        override val name: String
            get() = "Main module name"
        override val project: Project
            get() = mockProject
        override val targetPlatform: TargetPlatform
            get() = JvmPlatforms.defaultJvmPlatform
        override val transitiveDependsOnDependencies: List<KaModule>
            get() = emptyList()
    }

    override fun getImplementingModules(module: KaModule): List<KaModule> {
        log("getImplementingModules: $module")
        return emptyList()  // TODO
    }

    @OptIn(KaPlatformInterface::class)
    override fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule {
        log("getModule: $element")
        return mainModule
    }

    @OptIn(KaPlatformInterface::class)
    override fun getNotUnderContentRootModule(project: Project): KaNotUnderContentRootModule {
        TODO("Not yet implemented")
    }
}
