package org.example.services

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

class LSPModuleDependentsProvider: KotlinModuleDependentsProvider {
    override fun getDirectDependents(module: KaModule): Set<KaModule> {
        return emptySet()   // TODO
    }

    override fun getRefinementDependents(module: KaModule): Set<KaModule> {
        return emptySet()   // TODO
    }

    override fun getTransitiveDependents(module: KaModule): Set<KaModule> {
        return emptySet()   // TODO
    }
}