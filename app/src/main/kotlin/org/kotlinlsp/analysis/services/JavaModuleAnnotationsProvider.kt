package org.kotlinlsp.analysis.services

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleJavaAnnotationsProvider
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.kotlinlsp.trace

@OptIn(KaNonPublicApi::class)
class JavaModuleAnnotationsProvider: KotlinJavaModuleJavaAnnotationsProvider {
    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
        trace("[X] getAnnotationsForModuleOwnerOfClass")
        return null // TODO
    }
}
