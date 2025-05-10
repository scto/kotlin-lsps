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
                modificationStamp = ktFile.modificationStamp
            )
            return@read fileRecord
        }
    }

    // Check if the file record has been modified since last time
    // I think the case of overflowing modificationStamp is not worth to be considered as it is 64bit int
    // (a trillion modifications on the same file in the same coding session)
    val existingFile = db.fileLastModifiedFromPath(fileRecord.path)
    if (
        existingFile != null &&
        !existingFile.first.isBefore(fileRecord.lastModified) &&
        existingFile.second >= fileRecord.modificationStamp &&
        (fileRecord.modificationStamp != 0L || existingFile.second == 0L)
    ) return

    // Update the file timestamp and package
    db.setFile(fileRecord)

    // TODO Remove declarations for this file first
    ktFile.accept(object : KtTreeVisitorVoid() {
        override fun visitDeclaration(dcl: KtDeclaration) {
            super.visitDeclaration(dcl)

            val name = dcl.name ?: return
            val startOffset = dcl.textOffset
            val endOffset = dcl.textOffset + name.length

            db.putDeclarationForFile(fileRecord.path, name, startOffset, endOffset)
        }

        // TODO Store references
        /*override fun visitReferenceExpression(e: KtReferenceExpression) {
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
        }*/
    })
}
