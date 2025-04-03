package org.kotlinlsp.analysis.services

import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolver
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotated
import com.intellij.psi.search.GlobalSearchScope

class AnnotationsResolverFactory : KotlinAnnotationsResolverFactory {
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver {
        return AnnotationsResolver()
    }
}

// TODO Implement this
class AnnotationsResolver : KotlinAnnotationsResolver {
    override fun declarationsByAnnotation(annotationClassId: ClassId): Set<KtAnnotated> {
        return emptySet()
    }

    override fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId> {
        return emptySet()
    }
}
