package org.kotlinlsp.index

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.psi.KtFile

sealed class Command {
    data object Stop : Command()
    data object IndexingFinished: Command()
    data class IndexFile(val ktFile: KtFile) : Command()
    data class IndexClassFile(val virtualFile: VirtualFile) : Command()
}
