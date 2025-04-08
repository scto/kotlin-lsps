package org.kotlinlsp.analysis.services

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.kotlinlsp.trace
import org.kotlinlsp.warn

class PackagePartProviderFactory: KotlinPackagePartProviderFactory {
    override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
        trace("createPackagePartProvider $scope")
        val files = VirtualFileEnumeration.extract(scope)?.filesIfCollection
        if(files == null) {
            warn("Not JAR files!")
        }
        val roots = files?.map { JavaRoot(it, JavaRoot.RootType.BINARY) } ?: emptyList()
        return JvmPackagePartProvider(latestLanguageVersionSettings, scope).apply {
            addRoots(roots, MessageCollector.NONE)
        }
    }
}
