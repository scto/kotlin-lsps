package org.kotlinlsp.index.worker

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.common.read
import org.kotlinlsp.index.db.Database
import org.kotlinlsp.index.db.File
import org.kotlinlsp.index.db.file
import org.kotlinlsp.index.db.setFile
import java.time.Instant

fun scanKtFile(project: Project, ktFile: KtFile, db: Database) {
    val fileRecord = project.read {
        analyze(ktFile) {
            val packageFqName = ktFile.packageFqName.asString()
            val fileRecord = File(
                packageFqName = packageFqName,
                path = ktFile.virtualFile.url,
                lastModified = Instant.ofEpochMilli(ktFile.virtualFile.timeStamp),
                modificationStamp = ktFile.modificationStamp,
                indexed = false,
            )
            return@read fileRecord
        }
    }

    // Check if the file record has been modified since last time
    // I think the case of overflowing modificationStamp is not worth to be considered as it is 64bit int
    // (a trillion modifications on the same file in the same coding session)
    val existingFile = db.file(fileRecord.path)
    if (
        existingFile != null &&
        !existingFile.lastModified.isBefore(fileRecord.lastModified) &&
        existingFile.modificationStamp >= fileRecord.modificationStamp &&
        (fileRecord.modificationStamp != 0L || existingFile.modificationStamp == 0L)
    ) return

    // Update the file timestamp and package
    db.setFile(fileRecord)
}
