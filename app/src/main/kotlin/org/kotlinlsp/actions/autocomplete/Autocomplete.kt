package org.kotlinlsp.actions.autocomplete

import com.intellij.psi.util.leavesAroundOffset
import com.intellij.psi.util.parentOfType
import org.eclipse.lsp4j.CompletionItem
import org.jetbrains.kotlin.psi.*
import org.kotlinlsp.index.Index

fun autocompleteAction(ktFile: KtFile, offset: Int, index: Index): List<CompletionItem> {
    val leaf = ktFile.leavesAroundOffset(offset).asSequence()
        .toList().last().first

    val prefix = leaf.text.substring(0, offset - leaf.textRange.startOffset)
    val completingElement = leaf.parentOfType<KtElement>() ?: ktFile

    if (completingElement is KtNameReferenceExpression) {
        if (completingElement.parent is KtDotQualifiedExpression) {
            return autoCompletionDotExpression(ktFile, offset, index, completingElement.parent as KtDotQualifiedExpression, prefix)
        } else {
            return autoCompletionGeneric(ktFile, offset, index, completingElement, prefix)
        }
    }

    if (completingElement is KtValueArgumentList) {
        return emptyList() // TODO: function call arguments
    }

    return autoCompletionGeneric(ktFile, offset, index, completingElement, prefix)
}
