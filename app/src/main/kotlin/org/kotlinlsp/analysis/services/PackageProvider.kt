package org.kotlinlsp.analysis.services

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.kotlinlsp.log

class PackageProviderFactory: KotlinPackageProviderFactory {
    private lateinit var project: MockProject

    fun setup(project: MockProject) {
        this.project = project
    }

    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        return PackageProvider(project, searchScope)
    }
}

private class PackageProvider(project: Project, searchScope: GlobalSearchScope): KotlinPackageProviderBase(project, searchScope) {
    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        // TODO Check in the index if this package name corresponds to a kotlin file
        log("doesKotlinOnlyPackageExist: $packageFqName")
        return true
    }

    override fun getKotlinOnlySubpackageNames(packageFqName: FqName): Set<Name> {
        // TODO Get subpackage names in the index
        log("getKotlinOnlySubpackageNames: $packageFqName")
        return emptySet()
    }
}
