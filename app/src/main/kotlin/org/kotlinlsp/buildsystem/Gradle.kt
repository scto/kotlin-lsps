package org.kotlinlsp.buildsystem

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

class GradleBuildSystem: BuildSystem {
    override val markerFiles: List<String>
        get() = listOf()    // TODO

    override fun resolveRootModuleIfNeeded(cachedVersion: String?): Pair<KaModule, String>? {
        // TODO
        return null
    }
}
