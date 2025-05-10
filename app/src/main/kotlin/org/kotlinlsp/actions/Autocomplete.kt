package org.kotlinlsp.actions

import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.InsertTextFormat
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.index.Index
import org.kotlinlsp.index.queries.getCompletions

fun autocompleteAction(ktFile: KtFile, offset: Int, index: Index): List<CompletionItem> {
    val searchPrefix = computeSearchPrefix(ktFile, offset)
    val completions = index.getCompletions(searchPrefix)
        .map {
            CompletionItem().apply {
                label = it
                kind = CompletionItemKind.Function
                insertText = it
                insertTextFormat = InsertTextFormat.PlainText
            }
        }
    return completions
}

private fun computeSearchPrefix(ktFile: KtFile, offset: Int): String {
    val text = ktFile.text
    if (offset <= 0 || offset > text.length) return ""

    var start = offset - 1
    while (start >= 0 && text[start].isJavaIdentifierPart()) {
        start--
    }

    return text.substring(start + 1, offset)
}
