package org.kotlinlsp.analysis.services

import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolver
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotated
import com.intellij.psi.search.GlobalSearchScope
import org.kotlinlsp.log

class AnnotationsResolverFactory : KotlinAnnotationsResolverFactory {
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver {
        return AnnotationsResolver()
    }
}

// TODO Implement this
class AnnotationsResolver : KotlinAnnotationsResolver {
    override fun declarationsByAnnotation(annotationClassId: ClassId): Set<KtAnnotated> {
        log("declarationsByAnnotation: $annotationClassId")
        return emptySet()
    }

    override fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId> {
        log("annotationsOnDeclaration: $declaration")
        return emptySet()
    }
}
