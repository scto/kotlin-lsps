package org.example.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProviderBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule

class KotlinLSPProjectStructureProvider: KotlinProjectStructureProviderBase() {
    override fun getModule(element: PsiElement, useSiteModule: KaModule?): KaModule {
        TODO("Not yet implemented")
    }

    @OptIn(KaPlatformInterface::class)
    override fun getNotUnderContentRootModule(project: Project): KaNotUnderContentRootModule {
        TODO("Not yet implemented")
    }
}
