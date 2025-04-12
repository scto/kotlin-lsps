package org.kotlinlsp.analysis.services

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityChecker
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityError
import org.jetbrains.kotlin.name.FqName
import org.kotlinlsp.trace

class JavaModuleAccessibilityChecker: KotlinJavaModuleAccessibilityChecker {
    override fun checkAccessibility(
        useSiteFile: VirtualFile?,
        referencedFile: VirtualFile,
        referencedPackage: FqName?
    ): KotlinJavaModuleAccessibilityError? {
        trace("[X] KotlinJavaModuleAccessibilityChecker.checkAccessibility")
        return null     // TODO Implement
    }
}
