package org.kotlinlsp.analysis.services

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDirectInheritorsProvider
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.kotlinlsp.utils.trace

class DirectInheritorsProvider: KotlinDirectInheritorsProvider {
    override fun getDirectKotlinInheritors(
        ktClass: KtClass,
        scope: GlobalSearchScope,
        includeLocalInheritors: Boolean
    ): Iterable<KtClassOrObject> {
        trace("[X] getDirectKotlinInheritors")
        return emptyList()
    }
}
