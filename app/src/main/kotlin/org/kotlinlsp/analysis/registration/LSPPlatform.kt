package org.kotlinlsp.analysis.registration

import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDirectInheritorsProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProviderCliImpl
import org.kotlinlsp.analysis.services.DirectInheritorsProvider

fun Registrar.lspPlatform() {
    analysisApiFir()

    // These are defined in standalone platform xml
    project.registerService(
        KotlinDirectInheritorsProvider::class.java,
        DirectInheritorsProvider::class.java
    )
    app.registerService(
        BuiltinsVirtualFileProvider::class.java,
        BuiltinsVirtualFileProviderCliImpl::class.java
    )
}