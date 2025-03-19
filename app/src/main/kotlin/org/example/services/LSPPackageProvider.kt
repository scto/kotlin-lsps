package org.example.services

import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderBase
import org.example.services.KotlinLSPProjectStructureProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class LSPPackageProviderFactory: KotlinPackageProviderFactory {
    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        val project = KotlinLSPProjectStructureProvider.project!!
        return LSPPackageProvider(project, searchScope)
    }
}

class LSPPackageProvider(project: Project, searchScope: GlobalSearchScope): KotlinPackageProviderBase(project, searchScope) {
    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        // TODO
        return true
    }

    override fun getKotlinOnlySubpackageNames(packageFqName: FqName): Set<Name> {
        // TODO
        return emptySet()
    }
}
