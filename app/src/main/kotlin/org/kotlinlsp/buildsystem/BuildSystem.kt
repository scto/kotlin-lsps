package org.kotlinlsp.buildsystem

import org.kotlinlsp.analysis.services.modules.Module

interface BuildSystem {
    // List of files used to trigger this build system
    val markerFiles: List<String>

    // Resolves the module DAG if the cached version is not up to date
    // Returns null if cached version is up to date, otherwise
    // it returns the module DAG along with the current new version
    // cachedVersion = null if no cached module DAG is available
    // If the returned version is null it means caching is disabled
    fun resolveRootModuleIfNeeded(cachedVersion: String?): Pair<Module, String?>?
}