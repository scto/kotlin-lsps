package org.kotlinlsp.actions.autocomplete

import com.intellij.openapi.util.text.StringUtil
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextEdit
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.kotlinlsp.index.Index
import org.kotlinlsp.index.queries.getCompletions

fun autoCompletionDotExpression(
    ktFile: KtFile,
    offset: Int,
    index: Index,
    completingElement: KtDotQualifiedExpression,
    prefix: String
): List<CompletionItem> {
    val receiverType = analyze(completingElement) {
        completingElement.receiverExpression.expressionType.toString()
    }
    val existingImports = ktFile.importList?.children?.filterIsInstance<KtImportDirective>() ?: emptyList()
    val (importInsertionOffset, newlineCount) = if (existingImports.isEmpty()) {
        ktFile.packageDirective?.textRange?.let { it.endOffset to 2 } ?: (ktFile.textRange.startOffset to 0)
    } else {
        existingImports.last().textRange.endOffset to 1
    }
    val importInsertionPosition =
        StringUtil.offsetToLineColumn(ktFile.text, importInsertionOffset).let { Position(it.line, it.column) }

    return index
        .getCompletions(prefix, "", receiverType) // TODO: ThisRef
        .map { decl ->
            val (inserted, insertionType) = decl.insertInfo()

            CompletionItem().apply {
                label = decl.name
                labelDetails = decl.details()
                kind = decl.completionKind()
                insertText = inserted
                insertTextFormat = insertionType
                additionalTextEdits = emptyList()
            }
        }
        .toList()
}
