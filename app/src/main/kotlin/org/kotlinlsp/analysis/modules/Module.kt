package org.kotlinlsp.analysis.modules

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

interface Module {
    val id: String
    val dependencies: List<Module>
    val isSourceModule: Boolean
    val kaModule: KaModule

    fun computeFiles(): Sequence<VirtualFile>
}
