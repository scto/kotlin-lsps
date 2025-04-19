package org.kotlinlsp.analysis.services.modules

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

private data class SerializedModule(
    val id: String,
    val dependencies: List<String>,
    val javaVersion: String,
    // SourceModule
    val sourcePath: String? = null,
    val kotlinVersion: String? = null,
    // LibraryModule
    val isJdk: Boolean? = null,
    val libraryRoots: List<String>? = null
)

@OptIn(KaPlatformInterface::class)
fun serializeRootModule(rootModule: KaModule): String {
    val visited = LinkedHashMap<String, SerializedModule>()
    val stack = ArrayDeque<KaModule>()
    stack.add(rootModule)

    while (stack.isNotEmpty()) {
        val current = stack.removeLast()
        val id = current.id()
        if(visited.containsKey(id)) continue

        visited[id] = when(current) {
            is SourceModule -> SerializedModule(
                id = id,
                sourcePath = current.folderPath,
                kotlinVersion = current.kotlinVersion.versionString,
                javaVersion = current.javaVersion.toString(),
                dependencies = current.directRegularDependencies.map { it.id() }
            )
            is LibraryModule -> SerializedModule(
                id = id,
                libraryRoots = current.binaryRoots.map { it.absolutePathString() },
                isJdk = current.isSdk,
                javaVersion = current.javaVersion.toString(),
                dependencies = current.directRegularDependencies.map { it.id() }
            )
            else -> throw Exception("Unsupported KaModule!")
        }
        stack.addAll(current.directRegularDependencies)
    }

    return GsonBuilder().setPrettyPrinting().create().toJson(visited.values)
}

fun deserializeRootModule(
    data: String,
    appEnvironment: KotlinCoreApplicationEnvironment,
    mockProject: MockProject
): KaModule {
    val gson = Gson()
    val parsed: List<SerializedModule> = gson.fromJson(data, Array<SerializedModule>::class.java).toList()
    val nodeMap = parsed.associate {
        val module = if(it.sourcePath != null) {
            SourceModule(
                kotlinVersion = LanguageVersion.fromVersionString(it.kotlinVersion!!)!!,
                javaVersion = JvmTarget.fromString(it.javaVersion)!!,
                moduleName = it.id,
                folderPath = it.sourcePath,
                dependencies = mutableListOf(),
                mockProject = mockProject
            )
        } else {
            LibraryModule(
                javaVersion = JvmTarget.fromString(it.javaVersion)!!,
                isJdk = it.isJdk!!,
                name = it.id,
                roots = it.libraryRoots!!.map { Path(it) },
                dependencies = mutableListOf(),
                mockProject = mockProject,
                appEnvironment = appEnvironment,
            )
        }
        it.id to module
    }

    for (serial in parsed) {
        val deps = when(val node = nodeMap[serial.id]!!) {
            is SourceModule -> node.dependencies
            is LibraryModule -> node.dependencies
            else -> throw Exception("Unsupported KaModule!")
        }
        deps += serial.dependencies.map { nodeMap[it]!! }
    }

    return nodeMap[parsed.first().id]!!
}

private fun KaModule.id(): String = when(this) {
    is SourceModule -> moduleName
    is LibraryModule -> libraryName
    else -> throw Exception("Unsupported KaModule!")
}
