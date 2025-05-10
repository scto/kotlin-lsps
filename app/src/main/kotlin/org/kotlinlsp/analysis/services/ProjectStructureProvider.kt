package org.kotlinlsp.analysis.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProviderBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.kotlinlsp.common.profile
import org.kotlinlsp.common.read
import org.kotlinlsp.common.trace

class ProjectStructureProvider: KotlinProjectStructureProviderBase() {
    private lateinit var rootModule: KaModule
    private lateinit var project: Project

    fun setup(rootModule: KaModule, project: Project) {
        this.rootModule = rootModule
        this.project = project
    }

    override fun getImplementingModules(module: KaModule): List<KaModule> = profile("getImplementingModules", "$module") {
        emptyList()  // TODO Implement for KMP support
    }

    override fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule = profile("getModule", "$element, useSiteModule: $useSiteModule") {
        val virtualFile = element.containingFile.virtualFile
        searchVirtualFileInModule(virtualFile, useSiteModule ?: rootModule)!!
    }

    private fun searchVirtualFileInModule(virtualFile: VirtualFile, module: KaModule, visited: MutableSet<KaModule> = mutableSetOf()): KaModule? {
        if(visited.contains(module)) return null
        if(module.contentScope.contains(virtualFile)) return module

        for(it in module.directRegularDependencies) {
            val submodule = searchVirtualFileInModule(virtualFile, it)
            if(submodule != null) return submodule
        }

        visited.add(module)
        return null
    }

    @OptIn(KaPlatformInterface::class)
    override fun getNotUnderContentRootModule(project: Project): KaNotUnderContentRootModule {
        trace("getNotUnderContentRootModule")
        throw Exception("unsupported")
    }
}
