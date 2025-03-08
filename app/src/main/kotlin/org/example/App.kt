package org.example

import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.util.Disposer
import org.example.services.KotlinLSPProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.JvmType
import org.jetbrains.kotlin.psi.KtPsiFactory

fun main() {
    setupIdeaStandaloneExecution()
    val projectDisposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project")
    val compilerConfiguration = CompilerConfiguration()
    val appEnvironment = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(
        projectDisposable,
        compilerConfiguration,
        KotlinCoreApplicationEnvironmentMode.Production
    )
    val coreEnvironment = KotlinCoreProjectEnvironment(projectDisposable, appEnvironment)
    val project = coreEnvironment.project

    registerFIRServices(project)

    project.apply {
        registerService(KotlinProjectStructureProvider::class.java, KotlinLSPProjectStructureProvider::class.java)
    }

    val psiFactory = KtPsiFactory(project)
    val ktFile = psiFactory.createFile("fn main() { println(\"aaa\")}")
    
    analyze(ktFile) {
        val diagnostics = ktFile.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        println("Diagnostics: ${diagnostics.size}")
        diagnostics.forEach {
            println("${it.severity}: ${it.defaultMessage} | range: ${it.textRanges}")
        }
    }
}

fun registerFIRServices(project: MockProject) {
    val pluginDescriptor = DefaultPluginDescriptor("analysis-api-lsp-base-loader")

    // These services come from analysis-api-fhir.xml, so we need to keep synced with this file in case of analysis API libraries updates
    // TODO Add the rest of the FHIR services as needed
    registerFIRService(project, "org.jetbrains.kotlin.analysis.api.session.KaSessionProvider", "org.jetbrains.kotlin.analysis.api.fir.KaFirSessionProvider", pluginDescriptor)
}

fun registerFIRService(componentManager: MockComponentManager, interfaceName: String, implClassName: String, pluginDescriptor: DefaultPluginDescriptor) {
    val iface = componentManager.loadClass<JvmType.Object>(interfaceName, pluginDescriptor)
    val implClass = componentManager.loadClass<JvmType.Object>(implClassName, pluginDescriptor)
    componentManager.registerService(iface, implClass)
}
