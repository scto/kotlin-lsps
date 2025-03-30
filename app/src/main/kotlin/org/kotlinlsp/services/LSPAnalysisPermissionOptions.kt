package org.kotlinlsp.services

import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions

class LSPAnalysisPermissionOptions : KotlinAnalysisPermissionOptions {
    override val defaultIsAnalysisAllowedOnEdt: Boolean get() = false
    override val defaultIsAnalysisAllowedInWriteAction: Boolean get() = true // TODO See if we can set it to false
}
