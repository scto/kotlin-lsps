package org.kotlinlsp.analysis.services

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDirectInheritorsProvider
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.kotlinlsp.common.profile
import org.kotlinlsp.index.Index

class DirectInheritorsProvider: KotlinDirectInheritorsProvider {
    private lateinit var index: Index

    fun setup(index: Index) {
        this.index = index
    }

    override fun getDirectKotlinInheritors(
        ktClass: KtClass,
        scope: GlobalSearchScope,
        includeLocalInheritors: Boolean
    ): Iterable<KtClassOrObject> = profile("getDirectKotlinInheritors", "$ktClass") {
        emptyList() // TODO
    }
}
