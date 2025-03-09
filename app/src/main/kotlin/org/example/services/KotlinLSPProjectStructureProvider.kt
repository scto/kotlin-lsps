package org.example.services

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

class KotlinLSPProjectStructureProvider: KotlinProjectStructureProviderBase() {
    companion object {
        var project: MockProject? = null
        var virtualFiles: List<VirtualFile>? = null
    }

    private val mainModule = object : KaSourceModule {
        override val contentScope: GlobalSearchScope
            get() = GlobalSearchScope.filesScope(KotlinLSPProjectStructureProvider.project!!, virtualFiles!!)
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
            get() = KotlinLSPProjectStructureProvider.project!!
        override val targetPlatform: TargetPlatform
            get() = JvmPlatforms.defaultJvmPlatform
        override val transitiveDependsOnDependencies: List<KaModule>
            get() = emptyList()
    }

    override fun getImplementingModules(module: KaModule): List<KaModule> {
        TODO("Not yet implemented")
    }

    override fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule {
        return mainModule
    }

    @OptIn(KaPlatformInterface::class)
    override fun getNotUnderContentRootModule(project: Project): KaNotUnderContentRootModule {
        TODO("Not yet implemented")
    }
}
