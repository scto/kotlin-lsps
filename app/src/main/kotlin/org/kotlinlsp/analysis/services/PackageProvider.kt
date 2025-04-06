package org.kotlinlsp.analysis.services

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.impl.VirtualFileEnumeration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.log
import org.kotlinlsp.warn

class PackageProviderFactory: KotlinPackageProviderFactory {
    private lateinit var project: MockProject

    fun setup(project: MockProject) {
        this.project = project
    }

    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider = PackageProvider(project, searchScope)
}

private class PackageProvider(project: Project, searchScope: GlobalSearchScope): KotlinPackageProviderBase(project, searchScope) {
    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        // TODO Cache this
        val files = VirtualFileEnumeration.extract(searchScope)?.filesIfCollection
        if(files == null) {
            warn("doesKotlinOnlyPackageExist: not a VirtualFileEnumeration!")
            return false
        }

        for(file in files) {
            val ktFile = PsiManager.getInstance(project).findFile(file)!! as KtFile
            if(ktFile.packageDirective?.fqName == packageFqName) {
                return true
            }
        }
        return false
    }

    override fun getKotlinOnlySubpackageNames(packageFqName: FqName): Set<Name> {
        // TODO Get subpackage names
        log("getKotlinOnlySubpackageNames: $packageFqName")
        return emptySet()
    }
}
