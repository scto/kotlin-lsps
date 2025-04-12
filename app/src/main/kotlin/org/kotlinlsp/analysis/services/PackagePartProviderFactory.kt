package org.kotlinlsp.analysis.services

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.kotlinlsp.trace

class PackagePartProviderFactory: KotlinPackagePartProviderFactory {
    override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
        trace("createPackagePartProvider $scope")
        val files = VirtualFileEnumeration.extract(scope)?.filesIfCollection
            ?: throw Exception("Scope is not a VirtualFileEnumeration!")

        val roots = files.map { JavaRoot(it, JavaRoot.RootType.BINARY) }
        return JvmPackagePartProvider(latestLanguageVersionSettings, scope).apply {
            addRoots(roots, MessageCollector.NONE)
        }
    }
}
