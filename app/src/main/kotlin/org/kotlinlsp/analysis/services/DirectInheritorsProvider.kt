package org.kotlinlsp.analysis.services

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDirectInheritorsProvider
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.kotlinlsp.common.profile

class DirectInheritorsProvider: KotlinDirectInheritorsProvider {
    override fun getDirectKotlinInheritors(
        ktClass: KtClass,
        scope: GlobalSearchScope,
        includeLocalInheritors: Boolean
    ): Iterable<KtClassOrObject> = profile("[X] getDirectKotlinInheritors", "") {
        emptyList() // TODO
    }
}
