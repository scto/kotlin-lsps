package org.kotlinlsp.index

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.kotlinlsp.common.debug
import org.kotlinlsp.common.info
import org.kotlinlsp.common.read
import java.util.concurrent.ArrayBlockingQueue

class WorkerThread(
    private val rootFolder: String,
    private val project: Project
): Runnable {
    private val workQueue = ArrayBlockingQueue<Command>(100)

    override fun run() {
        val connection = createDbConnection(rootFolder)

        var count = 0

        var isDebug = true

        while(true) {
            when(val command = workQueue.take()) {
                is Command.Stop -> break
                is Command.IndexFile -> {
                    // TODO Write to DB
                    project.read {
                        analyze(command.ktFile) {
                            if (isDebug && command.ktFile.virtualFilePath.contains("Main.kt")) {
                                val ktFile = command.ktFile
                                val packageFqName = ktFile.packageFqName.asString()
                                debug("PACKAGE: $packageFqName")
                                ktFile.accept(object : KtTreeVisitorVoid() {
                                    override fun visitDeclaration(dcl: KtDeclaration) {
                                        super.visitDeclaration(dcl)
                                        debug("${dcl.name} ${dcl.textOffset} ${dcl.textOffset + dcl.name!!.length} ${dcl::class.java.simpleName}")
                                        val parentDecl =
                                            PsiTreeUtil.getParentOfType(dcl, KtDeclaration::class.java, true)
                                        if (parentDecl != null) {
                                            debug("PARENT SYMBOL: ${parentDecl.name}")
                                        }
                                    }

                                    override fun visitReferenceExpression(e: KtReferenceExpression) {
                                        super.visitReferenceExpression(e)

                                        if (e is KtNameReferenceExpression) {
                                            val target = try {
                                                e.mainReference.resolve()
                                            } catch (_: Exception) {
                                                null
                                            }

                                            if (target != null) {
                                                debug("REF: ${e.text} ${e.textOffset} ${e.textOffset + e.text.length} $target ${target::class.java.simpleName}")
                                            }
                                        }
                                    }
                                })
                                isDebug = false
                            }
                            count++
                        }
                    }
                }
                is Command.IndexClassFile -> {
                    // TODO
                    count ++
                }
                is Command.IndexingFinished -> {
                    info("Background indexing finished!, $count files!")
                }
            }
        }

        connection.close()
    }

    fun submitCommand(command: Command) {
        workQueue.put(command)
    }
}
