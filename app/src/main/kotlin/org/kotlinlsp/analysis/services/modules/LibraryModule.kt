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
import org.kotlinlsp.common.info
import org.kotlinlsp.common.read
import java.nio.file.Path

class LibraryModule(
    val appEnvironment: KotlinCoreApplicationEnvironment,
    val roots: List<Path>,
    val javaVersion: JvmTarget,
    override val dependencies: MutableList<Module> = mutableListOf(),
    val isJdk: Boolean = false,
    val name: String,
    private val mockProject: MockProject,
    private val sourceModule: KaLibrarySourceModule? = null,
): KaLibraryModule, Module, KaModuleBase() {
    @KaPlatformInterface
    override val baseContentScope: GlobalSearchScope by lazy {
        val virtualFileUrls = mutableSetOf<String>()
        computeFiles()
            .forEach { virtualFileUrls.add(it.url) }

        return@lazy object : GlobalSearchScope(project) {
            override fun contains(file: VirtualFile): Boolean = file.url in virtualFileUrls

            override fun isSearchInModuleContent(p0: com.intellij.openapi.module.Module): Boolean = false

            override fun isSearchInLibraries(): Boolean = true

            override fun toString(): String = virtualFileUrls.joinToString("\n") {
                it
            }
        }
    }

    @OptIn(KaImplementationDetail::class)
    override fun computeFiles(): Sequence<VirtualFile> {
        val roots = if (isJdk) {
            // This returns urls to the JMOD files in the jdk
            project.read { LibraryUtils.findClassesFromJdkHome(binaryRoots.first(), isJre = false) }
        } else {
            // These are JAR/class files
            binaryRoots
        }

        return roots.asSequence()
            .mapNotNull {
                getVirtualFileForLibraryRoot(it, appEnvironment, mockProject)
            }
            .map {
                project.read { LibraryUtils.getAllVirtualFilesFromRoot(it, includeRoot = true) }
            }
            .flatten()
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

    override val id: String
        get() = libraryName
    override val isSourceModule: Boolean
        get() = false
}

private const val JAR_SEPARATOR = "!/"

private fun getVirtualFileForLibraryRoot(
    root: Path,
    environment: CoreApplicationEnvironment,
    project: MockProject
): VirtualFile? {
    val pathString = FileUtil.toSystemIndependentName(root.toAbsolutePath().toString())

    if (pathString.endsWith(JAR_PROTOCOL) || pathString.endsWith(KLIB_FILE_EXTENSION)) {
        return project.read { environment.jarFileSystem.findFileByPath(pathString + JAR_SEPARATOR) }
    }

    if (pathString.contains(JAR_SEPARATOR)) {
        val (libHomePath, pathInImage) = CoreJrtFileSystem.splitPath(pathString)
        val adjustedPath = libHomePath + JAR_SEPARATOR + "modules/$pathInImage"
        return project.read { environment.jrtFileSystem?.findFileByPath(adjustedPath) }
    }

    return project.read { VirtualFileManager.getInstance().findFileByNioPath(root) }
}
