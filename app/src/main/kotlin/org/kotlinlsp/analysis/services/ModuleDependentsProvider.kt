package org.kotlinlsp.analysis.services

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.kotlinlsp.log

class ModuleDependentsProvider: KotlinModuleDependentsProvider {
    override fun getDirectDependents(module: KaModule): Set<KaModule> {
        log("getDirectDependents: $module")
        return emptySet()   // TODO
    }

    override fun getRefinementDependents(module: KaModule): Set<KaModule> {
        log("getRefinementDependents: $module")
        return emptySet()   // TODO
    }

    override fun getTransitiveDependents(module: KaModule): Set<KaModule> {
        log("getTransitiveDependents: $module")
        return emptySet()   // TODO
    }
}
