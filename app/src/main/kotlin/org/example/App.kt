package org.example

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockApplication
import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProviderCliImpl
import org.example.services.KotlinLSPDeclarationProviderFactory
import org.example.services.KotlinLSPPlatformSettings
import org.example.services.KotlinLSPProjectStructureProvider
import org.example.services.LSPPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinReadActionConfinementLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMerger
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.JvmType
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinSimpleGlobalSearchScopeMerger
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.example.services.LSPAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionConfigurator
import org.example.services.LSPAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import com.intellij.psi.impl.file.impl.JavaFileManager
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.config.*
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import com.intellij.psi.PsiDocumentManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.openapi.editor.impl.DocumentWriteAccessGuard
import com.intellij.openapi.editor.Document
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiElement

val latestLanguageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)

class WriteAccessGuard: DocumentWriteAccessGuard() {
    override fun isWritable(p0: Document): Result {
        return success()
    }
}

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

    app.apply {
        // TODO Intellij uses VFSUtil propietary class
        registerService(BuiltinsVirtualFileProvider::class.java, BuiltinsVirtualFileProviderCliImpl::class.java)

        registerService(KotlinAnalysisPermissionOptions::class.java, LSPAnalysisPermissionOptions::class.java)
    }

    project.apply {
        registerService(KotlinProjectStructureProvider::class.java, KotlinLSPProjectStructureProvider::class.java)
        registerService(KotlinLifetimeTokenFactory::class.java, KotlinReadActionConfinementLifetimeTokenFactory::class.java)
        registerService(KotlinPlatformSettings::class.java, KotlinLSPPlatformSettings::class.java)
        registerService(KotlinDeclarationProviderFactory::class.java, KotlinLSPDeclarationProviderFactory::class.java)
        registerService(KotlinPackageProviderFactory::class.java, LSPPackageProviderFactory::class.java)
        // TODO Implement something like intellij plugin
        registerService(KotlinGlobalSearchScopeMerger::class.java, KotlinSimpleGlobalSearchScopeMerger::class.java)

        registerService(KotlinAnnotationsResolverFactory::class.java, LSPAnnotationsResolverFactory::class.java)
    }

    CoreApplicationEnvironment.registerExtensionPoint(project.extensionArea, KaResolveExtensionProvider.EP_NAME, KaResolveExtensionProvider::class.java)
    CoreApplicationEnvironment.registerExtensionPoint(project.extensionArea, "org.jetbrains.kotlin.llFirSessionConfigurator", LLFirSessionConfigurator::class.java)
    CoreApplicationEnvironment.registerExtensionPoint(project.extensionArea, "com.intellij.java.elementFinder", JavaElementFinder::class.java) 
CoreApplicationEnvironment.registerExtensionPoint(app.extensionArea, DocumentWriteAccessGuard.EP_NAME, WriteAccessGuard::class.java)

    val pluginDescriptor = DefaultPluginDescriptor("analysis-api-lsp-base-loader-2")
    val kcsrClass = loadClass(app, "org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaResolveExtensionToContentScopeRefinerBridge", pluginDescriptor) 
    CoreApplicationEnvironment.registerExtensionPoint(project.extensionArea, "org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner", kcsrClass::class.java)

    val javaFileManager = project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl

    // TODO This setup comes from standalone platform
    val packagePartsScope = ProjectScope.getLibrariesScope(project)
    val libraryRoots = emptyList<JavaRoot>()
    val packagePartProvider = JvmPackagePartProvider(latestLanguageVersionSettings, packagePartsScope).apply {
        addRoots(libraryRoots, MessageCollector.NONE)
    }
    javaFileManager.initialize(
        index = JvmDependenciesIndexImpl(emptyList(), shouldOnlyFindFirstClass = false),
        packagePartProviders = listOf(packagePartProvider),
        singleJavaFileRootsIndex = SingleJavaFileRootsIndex(emptyList()),
        usePsiClassFilesReading = true,
        perfManager = null, 
    )

    // Load an example kotlin file from disk 
    val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file:///home/amg/Projects/kotlin-incremental-analysis/test-project/Main.kt")!!
    val ktFile = PsiManager.getInstance(project).findFile(virtualFile)!! as KtFile

    KotlinLSPProjectStructureProvider.project = project
    KotlinLSPProjectStructureProvider.virtualFiles = listOf(virtualFile)
    
    // Get diagnostics
    analyze(ktFile) {
        val diagnostics = ktFile.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        println("Diagnostics: ${diagnostics.size}")
        diagnostics.forEach {
            println("${it.severity}: ${it.defaultMessage} | range: ${it.textRanges}")
        }
    }

    // Perform an in-memory modification
    // TODO
    val cmd = app.getService(CommandProcessor::class.java)
    val psiDocMgr = PsiDocumentManager.getInstance(project)
    val doc = psiDocMgr.getDocument(ktFile)!!
    printPsiTree(ktFile)
    println(doc.text)
    cmd.executeCommand(project, {
        doc.replaceString(0, 0, "fun a(){}\n")
        psiDocMgr.commitDocument(doc)
        println(doc.text)
        try {
            printPsiTree(ktFile)
        } catch (e: Exception) {
            println(e)
        }
    }, "sample", null)

    // Get diagnostics again
    analyze(ktFile) {
        val diagnostics = ktFile.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        println("Updated diagnostics: ${diagnostics.size}")
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
    registerFIRServiceClass(project, "org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsService", pluginDescriptor)
    registerFIRServiceClass(project, "org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirInBlockModificationTracker", pluginDescriptor)
    registerFIRServiceClass(project, "org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService", pluginDescriptor)
    registerFIRServiceClass(project, "org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationEventPublisher", pluginDescriptor)
    registerFIRServiceClass(project, "org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationService", pluginDescriptor)
    registerFIRService(project, "org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser", "org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLRealFirElementByPsiElementChooser", pluginDescriptor)
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
    registerFIRServiceClass(project, "org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents", pluginDescriptor)
    registerFIRService(app, "org.jetbrains.kotlin.analysis.api.platform.resolution.KaResolutionActivityTracker", "org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolutionActivityTracker", pluginDescriptor)

    // analysis-api-impl-base.xml
    registerFIRServiceClass(app, "org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.KotlinDeclarationInCompiledFileSearcher", pluginDescriptor)
    registerFIRService(project, "org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaContentScopeProvider", "org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBaseContentScopeProvider", pluginDescriptor)
    registerFIRService(project, "org.jetbrains.kotlin.analysis.api.platform.KotlinMessageBusProvider", "org.jetbrains.kotlin.analysis.api.platform.KotlinProjectMessageBusProvider", pluginDescriptor)
    registerFIRService(project, "org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider", "org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBaseModuleProvider", pluginDescriptor)
    registerFIRService(project, "org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade", "org.jetbrains.kotlin.analysis.api.impl.base.java.KaBaseKotlinJavaPsiFacade", pluginDescriptor)
    registerFIRService(project, "org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory", "org.jetbrains.kotlin.analysis.api.impl.base.java.source.JavaElementSourceWithSmartPointerFactory", pluginDescriptor)
    registerFIRService(project, "org.jetbrains.kotlin.psi.KotlinReferenceProvidersService", "org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService", pluginDescriptor)
    registerFIRService(
        app, "org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry",
        "org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionRegistry", pluginDescriptor
    )
    registerFIRService(
        project, "org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver",
        "org.jetbrains.kotlin.analysis.api.impl.base.java.KaBaseJavaModuleResolver", pluginDescriptor
    )
    registerFIRService(project, "org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider",
        "org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBaseResolutionScopeProvider", pluginDescriptor)
    registerFIRService(project, "org.jetbrains.kotlin.analysis.api.platform.permissions.KaAnalysisPermissionChecker", "org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionChecker", pluginDescriptor)
    registerFIRService(project, "org.jetbrains.kotlin.analysis.api.platform.lifetime.KaLifetimeTracker", "org.jetbrains.kotlin.analysis.api.impl.base.lifetime.KaBaseLifetimeTracker", pluginDescriptor)
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

fun loadClass(componentManager: MockComponentManager, className: String, pluginDescriptor: DefaultPluginDescriptor): Class<JvmType.Object> {
    return componentManager.loadClass<JvmType.Object>(className, pluginDescriptor)
}

fun printPsiTree(ktFile: KtFile) {
    val rootNode = ktFile.node.psi

    printPsiNode(rootNode, 0)
}

fun printPsiNode(node: PsiElement, depth: Int) {
    // Print the node with indentation based on its depth in the tree
    val indent = "  ".repeat(depth)
    println("$indent${node.javaClass.simpleName}: ${node.text}")

    // Recursively print child nodes
    for (child in node.children) {
        printPsiNode(child, depth + 1)
    }
}
