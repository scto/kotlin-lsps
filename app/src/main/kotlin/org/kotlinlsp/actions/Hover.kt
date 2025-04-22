package org.kotlinlsp.actions

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.common.getElementRange
import org.kotlinlsp.common.toOffset

@OptIn(KaExperimentalApi::class)
fun hoverAction(ktFile: KtFile, position: Position): Pair<String, Range>? {
    val offset = position.toOffset(ktFile)
    val psi = ktFile.findElementAt(offset)?: return null
    val element = PsiTreeUtil.getParentOfType(
        psi,
        KtCallExpression::class.java,
    ) ?: return null
    val range = getElementRange(ktFile, element)

    val text = analyze(element) {
        when(element) {
            // TODO Implement the rest, this one is provided for guidance
            is KtCallExpression -> {
                val info = element.resolveToCall()?.successfulFunctionCallOrNull() ?: return null
                val symbol = info.partiallyAppliedSymbol.symbol
                val name = symbol.name ?: return null
                val printer = PrettyPrinter()
                printer.append("fun $name(")
                symbol.valueParameters.forEachIndexed { index, it ->
                    if(index > 0) printer.append(", ")
                    val name = it.name.toString()
                    printer.append(name)
                    printer.append(": ")
                    KaTypeRendererForSource.WITH_SHORT_NAMES.renderType(useSiteSession, it.returnType, printer)
                }
                printer.append("): ")
                KaTypeRendererForSource.WITH_SHORT_NAMES.renderType(useSiteSession, symbol.returnType, printer)
                return@analyze printer.toString()
            }
            else -> return null
        }
    }

    return Pair(text, range)
}