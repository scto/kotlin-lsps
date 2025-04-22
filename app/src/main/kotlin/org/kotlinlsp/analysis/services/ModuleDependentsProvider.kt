package org.kotlinlsp.analysis.services

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.kotlinlsp.common.profile

class ModuleDependentsProvider: KotlinModuleDependentsProvider {
    override fun getDirectDependents(module: KaModule): Set<KaModule> = profile("[X] getDirectDependents", "$module") {
        emptySet()   // TODO
    }

    override fun getRefinementDependents(module: KaModule): Set<KaModule> =
        profile("[X] getRefinementDependents", "$module") {
            emptySet()   // TODO
        }

    override fun getTransitiveDependents(module: KaModule): Set<KaModule> =
        profile("[X] getTransitiveDependents", "$module") {
            emptySet()   // TODO
        }
}
