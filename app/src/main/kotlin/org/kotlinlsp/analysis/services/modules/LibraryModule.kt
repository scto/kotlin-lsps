package org.kotlinlsp.analysis.services.modules

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.nio.file.Path
import kotlin.io.path.Path

class LibraryModule(
    private val mockProject: MockProject,
    private val jarPath: String,
    private val javaVersion: JvmTarget,
    private val sourceModule: KaLibrarySourceModule? = null,
    private val dependencies: List<KaModule> = emptyList(),
    private val isJdk: Boolean = false,
    private val name: String
): KaLibraryModule {
    private val scope = GlobalSearchScope.fileScope(mockProject, VirtualFileManager.getInstance().findFileByUrl("file://${jarPath}"))

    @KaPlatformInterface
    override val baseContentScope: GlobalSearchScope
        get() = scope
    override val binaryRoots: Collection<Path>
        get() = listOf(Path(jarPath))

    @KaExperimentalApi
    override val binaryVirtualFiles: Collection<VirtualFile>
        get() = emptyList() // Not supporting in-memory libraries
    override val contentScope: GlobalSearchScope
        get() = scope
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
    override val transitiveDependsOnDependencies: List<KaModule>
        get() = emptyList() // Not supporting KMP right now
}
