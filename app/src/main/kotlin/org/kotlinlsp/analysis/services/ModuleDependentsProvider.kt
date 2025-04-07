package org.kotlinlsp.analysis.services

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.kotlinlsp.trace

class ModuleDependentsProvider: KotlinModuleDependentsProvider {
    override fun getDirectDependents(module: KaModule): Set<KaModule> {
        trace("[X] getDirectDependents: $module")
        return emptySet()   // TODO
    }

    override fun getRefinementDependents(module: KaModule): Set<KaModule> {
        trace("[X] getRefinementDependents: $module")
        return emptySet()   // TODO
    }

    override fun getTransitiveDependents(module: KaModule): Set<KaModule> {
        trace("[X] getTransitiveDependents: $module")
        return emptySet()   // TODO
    }
}
