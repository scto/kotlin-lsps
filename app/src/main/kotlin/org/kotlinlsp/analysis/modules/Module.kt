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

fun Module.getModuleList() = getModuleListInternal(this)

private fun getModuleListInternal(module: Module, processedModules: MutableSet<String> = mutableSetOf()): Sequence<Module> = sequence {
    if(processedModules.contains(module.id)) return@sequence

    yield(module)

    module.dependencies.forEach {
        yieldAll(getModuleListInternal(it, processedModules))
    }

    processedModules.add(module.id)
}
