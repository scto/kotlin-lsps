package org.kotlinlsp.analysis.services.modules

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

interface Module: KaModule {
    val id: String
    val dependencies: MutableList<Module>

    fun computeFiles(): Sequence<VirtualFile>
}
