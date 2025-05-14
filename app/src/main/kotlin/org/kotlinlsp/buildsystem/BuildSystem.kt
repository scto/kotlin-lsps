package org.kotlinlsp.buildsystem

import org.kotlinlsp.analysis.modules.Module

interface BuildSystem {
    // List of files used to trigger this build system
    val markerFiles: List<String>

    // Resolves the module DAG if the cached metadata is not up to date
    // Returns null if cached metadata is up to date, otherwise
    // it returns the module DAG along with the current new version
    // cachedMetadata = null if no cached module DAG is available
    // If the returned metadata is null it means caching is disabled
    fun resolveRootModuleIfNeeded(cachedMetadata: String?): Pair<Module, String?>?
}