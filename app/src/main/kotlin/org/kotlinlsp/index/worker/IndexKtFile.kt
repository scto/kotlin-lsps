package org.kotlinlsp.index.worker

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.kotlinlsp.common.debug
import org.kotlinlsp.common.info
import org.kotlinlsp.common.read
import org.kotlinlsp.common.warn
import org.kotlinlsp.index.db.*
import org.rocksdb.RocksDB
import java.sql.Connection
import java.time.Instant

fun indexKtFile(project: Project, ktFile: KtFile, db: Database) {
    val fileRecord = project.read {
        analyze(ktFile) {
            val packageFqName = ktFile.packageFqName.asString()
            val fileRecord = File(
                packageFqName = packageFqName,
                path = ktFile.virtualFile.url,
                lastModified = Instant.ofEpochMilli(ktFile.virtualFile.timeStamp),
            )
            return@read fileRecord
        }
    }

    // Check if the file record has been modified since last time
    val existingLastModified = db.fileLastModifiedFromPath(fileRecord.path)
    if (
        existingLastModified != null &&
        !existingLastModified.isBefore(fileRecord.lastModified)
    ) return

    // TODO Process the KtFile and get symbols and references
    /*ktFile.accept(object : KtTreeVisitorVoid() {
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
                parentSymbol = if (parentSymbol != null) {
                    1
                } else {
                    null
                }
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

            if (target == null) {
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
    })*/

    // Update the file timestamp and package
    db.setFile(fileRecord)
}
