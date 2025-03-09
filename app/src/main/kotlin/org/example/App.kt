package org.example

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockApplication
import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.util.Disposer
import org.example.services.KotlinLSPProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinReadActionConfinementLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.JvmType
import org.jetbrains.kotlin.psi.KtPsiFactory

@OptIn(KaExperimentalApi::class)
fun main() {
    setupIdeaStandaloneExecution()
    val projectDisposable = Disposer.newDisposable("LSPAnalysisAPISession.project")
    val compilerConfiguration = CompilerConfiguration()
    val appEnvironment = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(
        projectDisposable,
        compilerConfiguration,
        KotlinCoreApplicationEnvironmentMode.Production
    )
    val coreEnvironment = KotlinCoreProjectEnvironment(projectDisposable, appEnvironment)
    val project = coreEnvironment.project
    val app = appEnvironment.application

    registerFIRServices(project, app)

    project.apply {
        registerService(KotlinProjectStructureProvider::class.java, KotlinLSPProjectStructureProvider::class.java)
        registerService(KotlinLifetimeTokenFactory::class.java, KotlinReadActionConfinementLifetimeTokenFactory::class.java)
    }

    CoreApplicationEnvironment.registerExtensionPoint(project.extensionArea, KaResolveExtensionProvider.EP_NAME, KaResolveExtensionProvider::class.java)

    val psiFactory = KtPsiFactory(project)
    val ktFile = psiFactory.createFile("fn main() { println(\"aaa\")}")
    val virtualFile = ktFile.virtualFile

    KotlinLSPProjectStructureProvider.project = project
    KotlinLSPProjectStructureProvider.virtualFiles = listOf(virtualFile)
    
    analyze(ktFile) {
        val diagnostics = ktFile.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        println("Diagnostics: ${diagnostics.size}")
        diagnostics.forEach {
            println("${it.severity}: ${it.defaultMessage} | range: ${it.textRanges}")
        }
    }
}

fun registerFIRServices(project: MockProject, app: MockApplication) {
    val pluginDescriptor = DefaultPluginDescriptor("analysis-api-lsp-base-loader")

    // These services come from analysis-api-fhir.xml, so we need to keep synced with this file in case of analysis API libraries updates
    // TODO Add the rest of the FHIR services as needed
    registerFIRService(
        project,
        "org.jetbrains.kotlin.analysis.api.session.KaSessionProvider",
        "org.jetbrains.kotlin.analysis.api.fir.KaFirSessionProvider",
        pluginDescriptor
    )

    // LL FIR services, these come from low-level-api-fhir.xml
    registerFIRServiceClass(
        project,
        "org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirBuiltinsSessionFactory",
        pluginDescriptor
    )
    registerFIRServiceClass(
        project,
        "org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService",
        pluginDescriptor
    )
    registerFIRServiceClass(
        project,
        "org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache",
        pluginDescriptor
    )

    // analysis-api-impl-base.xml
    registerFIRService(
        app, "org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry",
        "org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionRegistry", pluginDescriptor
    )
}

fun registerFIRService(componentManager: MockComponentManager, interfaceName: String, implClassName: String, pluginDescriptor: DefaultPluginDescriptor) {
    val iface = componentManager.loadClass<JvmType.Object>(interfaceName, pluginDescriptor)
    val implClass = componentManager.loadClass<JvmType.Object>(implClassName, pluginDescriptor)
    componentManager.registerService(iface, implClass)
}

fun registerFIRServiceClass(componentManager: MockComponentManager, implClassName: String, pluginDescriptor: DefaultPluginDescriptor) {
    val implClass = componentManager.loadClass<JvmType.Object>(implClassName, pluginDescriptor)
    componentManager.registerService(implClass)
}
