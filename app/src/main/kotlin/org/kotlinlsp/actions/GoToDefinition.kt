package org.kotlinlsp.actions

import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.common.toLspRange
import org.kotlinlsp.common.toOffset
import org.kotlinlsp.common.warn

fun goToDefinitionAction(ktFile: KtFile, position: Position): Location? = analyze(ktFile) {
    val offset = position.toOffset(ktFile)
    val ref = ktFile.findReferenceAt(offset) ?: return null
    val element = ref.resolve() ?: return null
    val file = element.containingFile ?: return null
    if(file.viewProvider.document == null) {
        // This comes from a .class file
        // TODO We could decompile the file and show it!
        warn("Go to definition failed: ${file.containingDirectory}/${file.containingFile.name}")
        return null
    }
    val range = element.textRange.toLspRange(file)
    val folder = file.containingDirectory.toString().removePrefix("PsiDirectory:")

    return Location().apply {
        uri = "file://${folder}/${file.containingFile.name}"
        setRange(range)
    }
}
