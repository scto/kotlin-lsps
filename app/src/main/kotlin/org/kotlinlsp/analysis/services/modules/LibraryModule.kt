package org.kotlinlsp.analysis.services.modules

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems.JAR_PROTOCOL
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.nio.file.Path

class LibraryModule(
    val appEnvironment: KotlinCoreApplicationEnvironment,
    private val mockProject: MockProject,
    private val roots: List<Path>,
    private val javaVersion: JvmTarget,
    private val sourceModule: KaLibrarySourceModule? = null,
    private val dependencies: List<KaModule> = emptyList(),
    private val isJdk: Boolean = false,
    private val name: String
): KaLibraryModule, KaModuleBase() {
    @OptIn(KaImplementationDetail::class)
    @KaPlatformInterface
    override val baseContentScope: GlobalSearchScope by lazy {
        val roots = if(isJdk) {
            LibraryUtils.findClassesFromJdkHome(binaryRoots.first(), isJre = false)
        } else {
            binaryRoots
        }

        return@lazy buildLibrarySearchScope(mockProject, roots, appEnvironment)
    }

    override val binaryRoots: Collection<Path>
        get() = roots

    @KaExperimentalApi
    override val binaryVirtualFiles: Collection<VirtualFile>
        get() = emptyList() // Not supporting in-memory libraries
    override val directDependsOnDependencies: List<KaModule>
        get() = emptyList() // Not supporting KMP right now
    override val directFriendDependencies: List<KaModule>
        get() = emptyList() // No support for this right now
    override val directRegularDependencies: List<KaModule>
        get() = dependencies

    @KaPlatformInterface
    override val isSdk: Boolean
        get() = isJdk
    override val libraryName: String
        get() = name
    override val librarySources: KaLibrarySourceModule?
        get() = sourceModule
    override val project: Project
        get() = mockProject
    override val targetPlatform: TargetPlatform
        get() = JvmPlatforms.jvmPlatformByTargetVersion(javaVersion)
}

private const val JAR_SEPARATOR = "!/"

@OptIn(KaImplementationDetail::class)
private fun buildLibrarySearchScope(project: MockProject, binaryRoots: Collection<Path>, appEnvironment: CoreApplicationEnvironment): GlobalSearchScope {
    val virtualFileUrls = buildSet {
        for (root in getVirtualFilesForLibraryRoots(binaryRoots, appEnvironment)) {
            LibraryUtils.getAllVirtualFilesFromRoot(root, includeRoot = true)
                .mapTo(this) { it.url }
        }
    }

    return object : GlobalSearchScope(project) {
        override fun contains(file: VirtualFile): Boolean = file.url in virtualFileUrls

        override fun isSearchInModuleContent(p0: com.intellij.openapi.module.Module): Boolean = false

        override fun isSearchInLibraries(): Boolean = true

        override fun toString(): String = virtualFileUrls.joinToString("\n") {
            it
        }
    }
}

private fun getVirtualFilesForLibraryRoots(
    roots: Collection<Path>,
    environment: CoreApplicationEnvironment,
): List<VirtualFile> {
    return roots.mapNotNull { path ->
        val pathString = FileUtil.toSystemIndependentName(path.toAbsolutePath().toString())
        when {
            pathString.endsWith(JAR_PROTOCOL) || pathString.endsWith(KLIB_FILE_EXTENSION) -> {
                environment.jarFileSystem.findFileByPath(pathString + JAR_SEPARATOR)
            }

            pathString.contains(JAR_SEPARATOR) -> {
                environment.jrtFileSystem?.findFileByPath(adjustModulePath(pathString))
            }

            else -> {
                VirtualFileManager.getInstance().findFileByNioPath(path)
            }
        }
    }.distinct()
}

private fun adjustModulePath(pathString: String): String {
    return if (pathString.contains(JAR_SEPARATOR)) {
        // URLs loaded from JDK point to module names in a JRT protocol format,
        // e.g., "jrt:///path/to/jdk/home!/java.base" (JRT protocol prefix + JDK home path + JAR separator + module name)
        // After protocol erasure, we will see "/path/to/jdk/home!/java.base" as a binary root.
        // CoreJrtFileSystem.CoreJrtHandler#findFile, which uses Path#resolve, finds a virtual file path to the file itself,
        // e.g., "/path/to/jdk/home!/modules/java.base". (JDK home path + JAR separator + actual file path)
        // To work with that JRT handler, a hacky workaround here is to add "modules" before the module name so that it can
        // find the actual file path.
        // See [LLFirJavaFacadeForBinaries#getBinaryPath] and [StandaloneProjectFactory#getBinaryPath] for a similar hack.
        val (libHomePath, pathInImage) = CoreJrtFileSystem.splitPath(pathString)
        libHomePath + JAR_SEPARATOR + "modules/$pathInImage"
    } else
        pathString
}