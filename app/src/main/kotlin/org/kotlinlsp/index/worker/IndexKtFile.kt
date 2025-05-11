package org.kotlinlsp.index.worker

import com.intellij.openapi.project.Project
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.kotlinlsp.common.info
import org.kotlinlsp.common.read
import org.kotlinlsp.common.warn
import org.kotlinlsp.index.db.*
import java.time.Instant

fun indexKtFile(project: Project, ktFile: KtFile, db: Database) {
    val fileRecord = project.read {
        analyze(ktFile) {
            val packageFqName = ktFile.packageFqName.asString()
            val fileRecord = File(
                packageFqName = packageFqName,
                path = ktFile.virtualFile.url,
                lastModified = Instant.ofEpochMilli(ktFile.virtualFile.timeStamp),
                modificationStamp = ktFile.modificationStamp,
                indexed = true,
            )
            return@read fileRecord
        }
    }

    // Check if the file record has been modified since last time
    // I think the case of overflowing modificationStamp is not worth to be considered as it is 64bit int
    // (a trillion modifications on the same file in the same coding session)
    val existingFile = db.fileLastModifiedAndIndexedFromPath(fileRecord.path)
    if (
        existingFile != null &&
        !existingFile.first.isBefore(fileRecord.lastModified) &&
        existingFile.second >= fileRecord.modificationStamp &&
        (fileRecord.modificationStamp != 0L || existingFile.second == 0L) &&
        existingFile.third  // Already indexed
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

            val decl = project.read {
                analyze(dcl) {
                    when (dcl) {
                        is KtNamedFunction -> {
                            val parentFqName = if (dcl.parent is KtClassBody) {
                                (dcl.parent.parent as? KtClassOrObject)?.fqName?.asString() ?: ""
                            } else ""

                            Declaration.Function(
                                name,
                                dcl.fqName?.asString() ?: "",
                                fileRecord.path,
                                startOffset,
                                endOffset,
                                dcl.valueParameters.map {
                                    Declaration.Function.Parameter(
                                        it.nameAsSafeName.asString(),
                                        it.returnType.toString()
                                    )
                                },
                                dcl.returnType.toString(),
                                parentFqName,
                                dcl.receiverTypeReference?.type?.toString() ?: ""
                            )
                        }

                        is KtClass -> {
                            if (dcl is KtEnumEntry) {
                                return@analyze Declaration.EnumEntry(
                                    name,
                                    dcl.fqName?.asString() ?: "",
                                    fileRecord.path,
                                    startOffset,
                                    endOffset,
                                    dcl.parentOfType<KtClass>()?.fqName?.asString() ?: ""
                                )
                            }

                            val type = if (dcl.isEnum()) {
                                Declaration.Class.Type.ENUM_CLASS
                            } else if (dcl.isAnnotation()) {
                                Declaration.Class.Type.ANNOTATION_CLASS
                            } else if (dcl.isInterface()) {
                                Declaration.Class.Type.INTERFACE
                            } else if (dcl.isAbstract()) {
                                Declaration.Class.Type.ABSTRACT_CLASS
                            } else {
                                Declaration.Class.Type.CLASS
                            }

                            Declaration.Class(
                                name,
                                type,
                                dcl.fqName?.asString() ?: "",
                                fileRecord.path,
                                startOffset,
                                endOffset
                            )
                        }

                        is KtParameter -> {
                            if (!dcl.hasValOrVar()) return@analyze null
                            val constructor = dcl.parentOfType<KtPrimaryConstructor>() ?: return@analyze null
                            val clazz = constructor.parentOfType<KtClass>() ?: return@analyze null

                            Declaration.Field(
                                name,
                                dcl.fqName?.asString() ?: "",
                                fileRecord.path,
                                startOffset,
                                endOffset,
                                dcl.returnType.toString(),
                                clazz.fqName?.asString() ?: ""
                            )
                        }

                        is KtProperty -> {
                            if (dcl.isLocal) return@analyze null
                            val clazz = dcl.parentOfType<KtClass>() ?: return@analyze Declaration.Field(
                                name,
                                dcl.fqName?.asString() ?: "",
                                fileRecord.path,
                                startOffset,
                                endOffset,
                                dcl.returnType.toString(),
                                ""
                            )

                            Declaration.Field(
                                name,
                                dcl.fqName?.asString() ?: "",
                                fileRecord.path,
                                startOffset,
                                endOffset,
                                dcl.returnType.toString(),
                                clazz.fqName?.asString() ?: ""
                            )
                        }

                        else -> {
                            // TODO Handle other declarations
                            warn("Declaration type not handled: ${dcl::class.simpleName}")
                            null
                        }
                    }
                }
            } ?: return

            db.putDeclaration(decl)

//            db.putDeclarationForFile(fileRecord.path, name, startOffset, endOffset)
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
