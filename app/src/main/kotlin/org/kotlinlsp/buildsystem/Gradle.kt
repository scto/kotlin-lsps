package org.kotlinlsp.buildsystem

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.kotlinlsp.analysis.services.modules.LibraryModule
import org.kotlinlsp.analysis.services.modules.SourceModule
import org.kotlinlsp.utils.debug
import org.kotlinlsp.utils.printModule
import kotlin.io.path.Path
import java.io.File

private var cachedModule: KaModule? = null

@OptIn(KaImplementationDetail::class, KaPlatformInterface::class)
fun getModuleList(project: MockProject, appEnvironment: KotlinCoreApplicationEnvironment): KaModule {
    val constantCachedModule = cachedModule
    if(constantCachedModule != null) return constantCachedModule

    // TODO Integrate with gradle, for now return a mock corresponding to the LSP project
    // To get dependency tree, use ./gradlew :app:dependencies --configuration compileClasspath
    // To get jar locations, use ./gradlew printMainClasspathJars

    val homeFolder = System.getenv("HOME")
    val gradleFolder = "$homeFolder/.gradle"
    val javaVersion = JvmTarget.JVM_21
    val kotlinVersion = LanguageVersion.KOTLIN_2_1

    val jdk = LibraryModule(
        appEnvironment = appEnvironment,
        mockProject = project,
        javaVersion = javaVersion,
        name = "JDK 21",
        isJdk = true,
        roots = listOf(Path("/usr/lib/jvm/java-21-openjdk")),
    )
    val jetbrainsAnnotations = LibraryModule(
        appEnvironment = appEnvironment,
        mockProject = project,
        name = "org.jetbrains:annotations:24.0.0",
        javaVersion = javaVersion,
        roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains/annotations/24.0.0/69b8b443c437fdeefa8d20c18d257b94836a92b9/annotations-24.0.0.jar"))
    )
    val kotlinStdlib = LibraryModule(
        appEnvironment = appEnvironment,
        mockProject = project,
        name = "org.jetbrains.kotlin:kotlin-stdlib:2.2.0-dev-7826",
        javaVersion = javaVersion,
        roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.0-dev-7826/263622e6ba20bd16a41ed4602be30bce9145b0d7/kotlin-stdlib-2.2.0-dev-7826.jar")),
        dependencies = listOf(
            jetbrainsAnnotations
        )
    )

    val errorProneAnnotations = LibraryModule(
        appEnvironment = appEnvironment,
        mockProject = project,
        name = "com.google.errorprone:error_prone_annotations:2.36.0",
        javaVersion = javaVersion,
        roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.google.errorprone/error_prone_annotations/2.36.0/227d4d4957ccc3dc5761bd897e3a0ee587e750a7/error_prone_annotations-2.36.0.jar")),
    )

    val lsp4j = LibraryModule(
        appEnvironment = appEnvironment,
        mockProject = project,
        name = "org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0",
        javaVersion = javaVersion,
        roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.eclipse.lsp4j/org.eclipse.lsp4j/0.24.0/6a0653487ea58604fc0a4fe3cdad8324b4ee3f6/org.eclipse.lsp4j-0.24.0.jar")),
        dependencies = listOf(
            LibraryModule(
                appEnvironment = appEnvironment,
                mockProject = project,
                name = "org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.24.0",
                javaVersion = javaVersion,
                roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.eclipse.lsp4j/org.eclipse.lsp4j.jsonrpc/0.24.0/27512deb3601e0cbf71d61d1a5611fb99687ab8e/org.eclipse.lsp4j.jsonrpc-0.24.0.jar")),
                dependencies = listOf(
                    LibraryModule(
                        appEnvironment = appEnvironment,
                        mockProject = project,
                        name = "com.google.code.gson:gson:2.12.1",
                        javaVersion = javaVersion,
                        roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.google.code.gson/gson/2.12.1/4e773a317740b83b43cfc3d652962856041697cb/gson-2.12.1.jar")),
                        dependencies = listOf(errorProneAnnotations)
                    )
                )
            )
        )
    )

    val intellijPlatformUtil = LibraryModule(
        appEnvironment = appEnvironment,
        mockProject = project,
        javaVersion = javaVersion,
        name = "com.jetbrains.intellij.platform:util:241.19416.19",
                roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/util/241.19416.19/29585e92acae37fbf78115c39e10adf0e158e7ec/util-241.19416.19.jar")),
        dependencies = listOf(
            jetbrainsAnnotations,
            LibraryModule(
                appEnvironment = appEnvironment,
                mockProject = project,
                javaVersion = javaVersion,
                name = "com.jetbrains.intellij.platform:util-rt:241.19416.19",
                roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/util-rt/241.19416.19/e177a41e5bd51dbbca0d35308f1813866bdb4065/util-rt-241.19416.19.jar")),
            ),
            LibraryModule(
                appEnvironment = appEnvironment,
                mockProject = project,
                javaVersion = javaVersion,
                name = "com.jetbrains.intellij.platform:util-base:241.19416.19",
                roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/util-base/241.19416.19/4725251850d1a69245db204f50d1f49d33ae06b6/util-base-241.19416.19.jar"))
            )
        )
    )

    val coroutinesCore = LibraryModule(
        appEnvironment = appEnvironment,
        mockProject = project,
        javaVersion = javaVersion,
        name = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.0",
                roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.8.0/ac1dc37a30a93150b704022f8d895ee1bd3a36b3/kotlinx-coroutines-core-jvm-1.8.0.jar")),
        dependencies = listOf(
            jetbrainsAnnotations,
            kotlinStdlib
        )
    )

    val intellijPlatformCore = LibraryModule(
        appEnvironment = appEnvironment,
        mockProject = project,
        javaVersion = javaVersion,
        name = "com.jetbrains.intellij.platform:core:241.19416.19",
                roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/core/241.19416.19/3e52cb8848c74ca17b0a888b85eeed79135d4401/core-241.19416.19.jar")),
        dependencies = listOf(
            intellijPlatformUtil,
            LibraryModule(
                appEnvironment = appEnvironment,
                mockProject = project,
                javaVersion = javaVersion,
                name = "com.jetbrains.intellij.platform:extensions:241.19416.19",
                roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/extensions/241.19416.19/f0cd5be01b6449b908a32a44729223a57c2bf890/extensions-241.19416.19.jar")),
                dependencies = listOf(
                    coroutinesCore
                )
            ),
            LibraryModule(
                appEnvironment = appEnvironment,
                mockProject = project,
                javaVersion = javaVersion,
                name = "com.jetbrains.intellij.platform:util-progress:241.19416.19",
                roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/util-progress/241.19416.19/a95ff5e4d9de82c7810e9351ccbea0930efbd546/util-progress-241.19416.19.jar")),
            ),
            LibraryModule(
                appEnvironment = appEnvironment,
                mockProject = project,
                javaVersion = javaVersion,
                name = "org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.7",
                roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-collections-immutable-jvm/0.3.7/1cd4ff7059bcb437086b76311a9fc4775a887bcc/kotlinx-collections-immutable-jvm-0.3.7.jar")),
            )
        )
    )

    val dependencies = listOf<KaModule>(
        kotlinStdlib,
        intellijPlatformUtil,
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            javaVersion = javaVersion,
            name = "org.jetbrains.kotlin:kotlin-compiler:2.2.0-dev-7826",
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler/2.2.0-dev-7826/1170a3824432506d623c04d949ed0c1e82a33a68/kotlin-compiler-2.2.0-dev-7826.jar")),
            dependencies = listOf(
                LibraryModule(
                    appEnvironment = appEnvironment,
                    mockProject = project,
                    javaVersion = javaVersion,
                    name = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.0-dev-7826",
                    roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk8/2.2.0-dev-7826/5e90a127bfc5318798ea8d6e3a950f7d031d0a6f/kotlin-stdlib-jdk8-2.2.0-dev-7826.jar")),
                    dependencies = listOf(
                        kotlinStdlib,
                        LibraryModule(
                            appEnvironment = appEnvironment,
                            mockProject = project,
                            javaVersion = javaVersion,
                            name = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.0-dev-7826",
                            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk7/2.2.0-dev-7826/aa98b507662e47bf2436185de9d91e909149723c/kotlin-stdlib-jdk7-2.2.0-dev-7826.jar")),
                            dependencies = listOf(kotlinStdlib)
                        )
                    )
                ),
                LibraryModule(
                    appEnvironment = appEnvironment,
                    mockProject = project,
                    javaVersion = javaVersion,
                    name = "org.jetbrains.kotlin:kotlin-script-runtime:2.2.0-dev-7826",
                    roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-script-runtime/2.2.0-dev-7826/246253e15c97f602cc855baf2c32d95b314a9b5/kotlin-script-runtime-2.2.0-dev-7826.jar")),
                ),
                LibraryModule(
                    appEnvironment = appEnvironment,
                    mockProject = project,
                    javaVersion = javaVersion,
                    name = "org.jetbrains.kotlin:kotlin-reflect:1.6.10",
                    roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-reflect/1.6.10/1cbe9c92c12a94eea200d23c2bbaedaf3daf5132/kotlin-reflect-1.6.10.jar")),
                ),
                coroutinesCore
            )
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            javaVersion = javaVersion,
            name = "com.jetbrains.intellij.platform:core-impl:241.19416.19",
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/core-impl/241.19416.19/abe0e29443c689686c74753037456f93dd58bb9f/core-impl-241.19416.19.jar")),
            dependencies = listOf(
                intellijPlatformCore,
                kotlinStdlib
            )
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            javaVersion = javaVersion,
            name = "com.github.ben-manes.caffeine:caffeine:2.9.3",
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/com.github.ben-manes.caffeine/caffeine/2.9.3/b162491f768824d21487551873f9b3b374a7fe19/caffeine-2.9.3.jar")),
            dependencies = listOf(
                LibraryModule(
                    appEnvironment = appEnvironment,
                    mockProject = project,
                    javaVersion = javaVersion,
                    name = "org.checkerframework:checker-qual:3.19.0",
                    roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.checkerframework/checker-qual/3.19.0/838b42bb6f7f73315167b359d24649845cef1c48/checker-qual-3.19.0.jar")),
                ),
                errorProneAnnotations
            )
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            javaVersion = javaVersion,
            name = "org.jetbrains.kotlin:analysis-api-k2-for-ide:2.2.0-dev-7826",
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/analysis-api-k2-for-ide/2.2.0-dev-7826/fad2bbd0dc89b12d3ca500d14b3eab2c5499739e/analysis-api-k2-for-ide-2.2.0-dev-7826.jar"))
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            javaVersion = javaVersion,
            name = "org.jetbrains.kotlin:analysis-api-for-ide:2.2.0-dev-7826",
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/analysis-api-for-ide/2.2.0-dev-7826/7c4c43263cf019f32d15a744c73087d3c01ccc55/analysis-api-for-ide-2.2.0-dev-7826.jar"))
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            javaVersion = javaVersion,
            name = "org.jetbrains.kotlin:low-level-api-fir-for-ide:2.2.0-dev-7826",
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/low-level-api-fir-for-ide/2.2.0-dev-7826/5e348b559cb8afe2ef74ca80bc3c27d244655c9/low-level-api-fir-for-ide-2.2.0-dev-7826.jar"))
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            name = "org.jetbrains.kotlin:analysis-api-platform-interface-for-ide:2.2.0-dev-7826",
            javaVersion = javaVersion,
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/analysis-api-platform-interface-for-ide/2.2.0-dev-7826/23637be781f5dc62c0d3b4c3e871849dc0b2e405/analysis-api-platform-interface-for-ide-2.2.0-dev-7826.jar"))
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            name = "org.jetbrains.kotlin:symbol-light-classes-for-ide:2.2.0-dev-7826",
            javaVersion = javaVersion,
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/symbol-light-classes-for-ide/2.2.0-dev-7826/15710019956d374df2c864f729af8587583c14ed/symbol-light-classes-for-ide-2.2.0-dev-7826.jar"))
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            name = "org.jetbrains.kotlin:analysis-api-impl-base-for-ide:2.2.0-dev-7826",
            javaVersion = javaVersion,
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/analysis-api-impl-base-for-ide/2.2.0-dev-7826/bb02f9977512c04f25607ef4a8a6b1416c543547/analysis-api-impl-base-for-ide-2.2.0-dev-7826.jar"))
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            name = "org.jetbrains.kotlin:kotlin-compiler-common-for-ide:2.2.0-dev-7826",
            javaVersion = javaVersion,
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler-common-for-ide/2.2.0-dev-7826/5b0b8e59ff39f4246948a367a1f3d65b90b6a072/kotlin-compiler-common-for-ide-2.2.0-dev-7826.jar"))
        ),
        LibraryModule(
            appEnvironment = appEnvironment,
            mockProject = project,
            name = "org.jetbrains.kotlin:kotlin-compiler-fir-for-ide:2.2.0-dev-7826",
            javaVersion = javaVersion,
            roots = listOf(Path("$gradleFolder/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler-fir-for-ide/2.2.0-dev-7826/4daec39a1f78c27c5a402d41a7e233c2041805b9/kotlin-compiler-fir-for-ide-2.2.0-dev-7826.jar"))
        ),
        lsp4j,
        jdk
    )

    val rootPath = File("").absolutePath 
    val rootModule = SourceModule(
        moduleName = "main",
        mockProject = project,
        kotlinVersion = kotlinVersion,
        javaVersion = javaVersion,
        folderPath = "$rootPath/app/src/main",
        dependencies = dependencies
    )
    printModule(rootModule)
    cachedModule = rootModule
    return rootModule
}
