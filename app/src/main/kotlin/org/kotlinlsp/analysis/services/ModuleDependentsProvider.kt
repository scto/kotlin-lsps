package org.kotlinlsp.analysis.services

import com.intellij.util.containers.ContainerUtil.createConcurrentSoftMap
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProviderBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.kotlinlsp.common.profile

class ModuleDependentsProvider: KotlinModuleDependentsProviderBase() {
    private lateinit var rootModule: KaModule

    fun setup(rootModule: KaModule) {
        this.rootModule = rootModule
    }

    private val directDependentsByKtModule: Map<KaModule, Set<KaModule>> by lazy {
        buildDependentsMap(rootModule) { it.allDirectDependencies() }
    }

    private val transitiveDependentsByKtModule = createConcurrentSoftMap<KaModule, Set<KaModule>>()

    private val refinementDependentsByKtModule: Map<KaModule, Set<KaModule>> by lazy {
        buildDependentsMap(rootModule) { it.transitiveDependsOnDependencies.asSequence() }
    }

    override fun getDirectDependents(module: KaModule): Set<KaModule> = profile("getDirectDependents", "$module") {
        directDependentsByKtModule[module].orEmpty()
    }

    override fun getRefinementDependents(module: KaModule): Set<KaModule> =
        profile("getRefinementDependents", "$module") {
            refinementDependentsByKtModule[module].orEmpty()
        }

    override fun getTransitiveDependents(module: KaModule): Set<KaModule> =
        profile("getTransitiveDependents", "$module") {
            transitiveDependentsByKtModule.computeIfAbsent(module) { computeTransitiveDependents(it) }
        }
}

private inline fun buildDependentsMap(
    module: KaModule,
    getDependencies: (KaModule) -> Sequence<KaModule>,
): Map<KaModule, MutableSet<KaModule>> = buildMap {
    for (dependency in getDependencies(module)) {
        if (dependency == module) continue

        val dependents = computeIfAbsent(dependency) { mutableSetOf() }
        dependents.add(module)
    }
}
