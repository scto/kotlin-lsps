package org.example

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.CompilerConfiguration

fun main() {
    setupIdeaStandaloneExecution()
    val projectDisposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project")
    val compilerConfiguration = CompilerConfiguration()
    val environment = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(
        projectDisposable,
        compilerConfiguration,
        KotlinCoreApplicationEnvironmentMode.Production,
    )
    println("Hello, world!")
}
