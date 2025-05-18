package org.kotlinlsp.actions

import com.intellij.openapi.project.DefaultProjectFactory
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClassFileDecompiler
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.kotlinlsp.common.info
import org.kotlinlsp.common.toLspRange
import org.kotlinlsp.common.toOffset
import org.kotlinlsp.common.warn
import java.io.File

fun goToDefinitionAction(ktFile: KtFile, position: Position): Location? = analyze(ktFile) {
    val offset = position.toOffset(ktFile)
    val ref = ktFile.findReferenceAt(offset) ?: return null
    val element = ref.resolve() ?: return tryResolveFromKotlinLibrary(ktFile, offset)
    val file = element.containingFile ?: return null
    if(file.viewProvider.document == null) {
        // This comes from a java .class file
        return tryDecompileJavaClass(file)
    }
    val range = element.textRange.toLspRange(file)
    val folder = file.containingDirectory.toString().removePrefix("PsiDirectory:")

    return Location().apply {
        uri = "file://${folder}/${file.containingFile.name}"
        setRange(range)
    }
}

private fun KaSession.tryResolveFromKotlinLibrary(ktFile: KtFile, offset: Int): Location? {
    val element = ktFile.findElementAt(offset) ?: return null
    val ref = element.parent as? KtReferenceExpression ?: return null
    val symbol = ref.mainReference.resolveToSymbol() as? KaCallableSymbol ?: return null
    val packageName = symbol.callableId?.packageName?.asString() ?: return null
    val callableName = symbol.callableId?.callableName?.asString() ?: return null
    val module = symbol.containingModule

    val provider =
        KotlinPackagePartProviderFactory.getInstance(ktFile.project).createPackagePartProvider(module.contentScope)
    val names = provider.findPackageParts(packageName).map { it.replace("/", ".") }
    val psiFacade = JavaPsiFacade.getInstance(ktFile.project)
    val psiClass = names.mapNotNull {
        psiFacade.findClass(it, module.contentScope)
    }.find {
        val fns = it.methods.mapNotNull { it.name }
        return@find fns.contains(callableName)
    } ?: return null

    // Decompile the kotlin .class file
    val decompiledView = KotlinClassFileDecompiler().createFileViewProvider(
        psiClass.containingFile.virtualFile,
        PsiManager.getInstance(ktFile.project),
        physical = true
    )
    val decompiledContent = decompiledView.content.get()
    val tmpFile = File.createTempFile("KtDecompiledFile", ".kt")
    tmpFile.writeText(decompiledContent)

    return Location().apply {
        uri = "file://${tmpFile.absolutePath}"
        range = Range().apply {
            start = Position(0, 0)  // TODO Set correct position
            end = Position(0, 1)
        }
    }
}

private fun tryDecompileJavaClass(file: PsiFile): Location? {
    // Just testing, does not work right now
    /*val decompiledContent = ClassFileDecompiler().decompile(file.virtualFile).toString()
    val tmpFile = File.createTempFile("JavaDecompiledFile", ".java")
    tmpFile.writeText(decompiledContent)

    return Location().apply {
        uri = "file://${tmpFile.absolutePath}"
        range = Range().apply {
            start = Position(0, 0)  // TODO Set correct position
            end = Position(0, 1)
        }
    }*/
    return null
}
