package org.kotlinlsp.index.worker

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.kotlinlsp.common.debug
import org.kotlinlsp.common.read
import org.kotlinlsp.common.warn
import org.kotlinlsp.index.db.FileRecord
import org.kotlinlsp.index.db.ReferenceRecord
import org.kotlinlsp.index.db.SymbolRecord
import java.sql.Connection
import java.time.Instant

fun indexKtFile(project: Project, ktFile: KtFile, connection: Connection) = project.read {
    return@read // TODO For now until testing for a single file works
    analyze(ktFile) {
        val packageFqName = ktFile.packageFqName.asString()
        val fileRecord = FileRecord(
            id = -1,    // This means generate it
            packageFqName = packageFqName,
            path = ktFile.virtualFile.url,
            lastModified = Instant.ofEpochMilli(ktFile.modificationStamp)
        )
        debug("FILE: $fileRecord")
        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitDeclaration(dcl: KtDeclaration) {
                super.visitDeclaration(dcl)

                val parentSymbol = PsiTreeUtil.getParentOfType(dcl, KtDeclaration::class.java, true)
                val name = dcl.name ?: return
                val symbolRecord = SymbolRecord(
                    id = -1,
                    name = name,
                    startOffset = dcl.textOffset,
                    endOffset = dcl.textOffset + name.length,
                    file = -1,
                    kind = -1,
                    parentSymbol = if(parentSymbol != null) { 1 } else { null }
                )
                debug("SYMBOL: $symbolRecord")
            }

            override fun visitReferenceExpression(e: KtReferenceExpression) {
                super.visitReferenceExpression(e)
                if (e !is KtNameReferenceExpression) return

                val target = try {
                    e.mainReference.resolve()
                } catch (_: Exception) {
                    null
                }

                if(target == null) {
                    warn("Unresolved reference: ${e.text}")
                    return
                }

                val referenceRecord = ReferenceRecord(
                    id = -1,
                    symbolId = 1,
                    startOffset = e.textOffset,
                    endOffset = e.endOffset
                )

                debug("REFERENCE: $referenceRecord")
                debug("-> Name: ${e.text}")
                debug("-> Target: ${target?.containingFile?.virtualFile?.url}")
            }
        })
    }
}
