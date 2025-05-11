package org.kotlinlsp.actions

import com.intellij.psi.util.elementsAtOffsetUp
import com.intellij.psi.util.parentOfType
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.CompletionItemLabelDetails
import org.eclipse.lsp4j.InsertTextFormat
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.types.Variance
import org.kotlinlsp.index.Index
import org.kotlinlsp.index.db.Declaration
import org.kotlinlsp.index.queries.getCompletions

@OptIn(KaExperimentalApi::class)
fun autocompleteAction(ktFile: KtFile, offset: Int, index: Index): List<CompletionItem> {
    val searchPrefix = computeSearchPrefix(ktFile, offset)

    val containingScope = ktFile.elementsAtOffsetUp(offset).asSequence()
        .map { it.first }.filterIsInstance<KtElement>().first()

    val localVariableCompletions: List<CompletionItem> = analyze(containingScope) {
        ktFile.scopeContext(containingScope).scopes.flatMap {
            if (it.kind !is KaScopeKind.LocalScope) return@flatMap emptyList()
            it.scope.declarations.mapNotNull { decl ->
                if (!decl.name.toString().startsWith(searchPrefix)) return@mapNotNull null
                val psi = decl.psi ?: return@mapNotNull null
                // TODO: This is a hack to get the correct offset for function literals, can analysis tell us if a declaration is accessible?
                val declOffset = if (psi is KtFunctionLiteral) psi.textRange.startOffset else psi.textRange.endOffset
                if (declOffset >= offset) return@mapNotNull null

                val detail = when (decl) {
                    is KaVariableSymbol -> decl.returnType.render(KaTypeRendererForSource.WITH_SHORT_NAMES, Variance.INVARIANT)
                    else -> "Missing ${decl.javaClass.simpleName}"
                }

                val preview = when (psi) {
                    is KtProperty -> psi.text
                    is KtParameter -> {
                        if (psi.isLoopParameter) {
                            val loop = psi.parentOfType<KtLoopExpression>()!!
                            loop.text.replace(loop.body!!.text, "")
                        } else psi.text
                    }
                    is KtFunctionLiteral -> decl.name // TODO: Show the function call containing the lambda?
                    else -> "TODO: Preview for ${psi.javaClass.simpleName}"
                }

                CompletionItem().apply {
                    label = decl.name.toString()
                    labelDetails = CompletionItemLabelDetails().apply {
                        this.detail = "  $detail"
                        description = ""
                    }
                    documentation = Either.forRight(
                        MarkupContent("markdown", "```kotlin\n${preview}\n```")
                    )
                    kind = CompletionItemKind.Variable
                    insertText = decl.name.toString()
                    insertTextFormat = InsertTextFormat.PlainText
                }
            }.toList()
        }
    }

    val completions = index.getCompletions(searchPrefix)
        .mapNotNull {
            if (it is Declaration.Field) {
                if (it.parentFqName.isNotEmpty()) return@mapNotNull null
            }

            val kind = when (it) {
                is Declaration.EnumEntry -> CompletionItemKind.EnumMember
                is Declaration.Class -> when (it.type) {
                    Declaration.Class.Type.CLASS -> CompletionItemKind.Class
                    Declaration.Class.Type.ABSTRACT_CLASS -> CompletionItemKind.Class
                    Declaration.Class.Type.INTERFACE -> CompletionItemKind.Interface
                    Declaration.Class.Type.ENUM_CLASS -> CompletionItemKind.Enum
                    Declaration.Class.Type.OBJECT -> CompletionItemKind.Module
                    Declaration.Class.Type.ANNOTATION_CLASS -> CompletionItemKind.Interface
                }
                is Declaration.Function -> CompletionItemKind.Function
                is Declaration.Field -> CompletionItemKind.Field
            }

            val detail = when (it) {
                is Declaration.Class -> CompletionItemLabelDetails().apply {
                    detail = " (${it.fqName})"
                }
                is Declaration.EnumEntry -> CompletionItemLabelDetails().apply {
                    detail = ": ${it.enumFqName}"
                }
                is Declaration.Function -> CompletionItemLabelDetails().apply {
                    detail = "(${it.parameters.joinToString(", ") { param -> "${param.name}: ${param.type}" }}): ${it.returnType} (${it.fqName})"
                }
                is Declaration.Field -> CompletionItemLabelDetails().apply {
                    detail = ": ${it.type} (${it.fqName})"
                }
            }

            val (inserted, insertionType) = when (it) {
                is Declaration.Class -> it.name to InsertTextFormat.PlainText
                is Declaration.EnumEntry -> "${it.enumFqName.substringAfterLast('.')}.${it.name}" to InsertTextFormat.PlainText
                is Declaration.Function -> "${it.name}(${
                    it.parameters.mapIndexed { index, param -> "\${${index+1}:${param.name}}" }.joinToString(", ")
                })" to InsertTextFormat.Snippet
                is Declaration.Field -> it.name to InsertTextFormat.PlainText
            }

            CompletionItem().apply {
                label = it.name
                labelDetails = detail
                this.kind = kind
                insertText = inserted
                insertTextFormat = insertionType
            }
        }
    return localVariableCompletions + completions
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
