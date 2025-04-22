package org.kotlinlsp.analysis.services.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.common.warn

fun virtualFilesForPackage(project: Project, searchScope: GlobalSearchScope, packageFqName: FqName): Sequence<VirtualFile> = sequence {
    val files = VirtualFileEnumeration.extract(searchScope)?.filesIfCollection
    if(files == null) {
        warn("doesKotlinOnlyPackageExist: not a VirtualFileEnumeration!")
        return@sequence
    }

    for(file in files) {
        val ktFile = PsiManager.getInstance(project).findFile(file)!! as KtFile
        if(ktFile.packageDirective?.fqName == packageFqName) {
            yield(file)
        }
    }
}
