package org.kotlinlsp.analysis.services

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolver
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotated
import org.kotlinlsp.utils.profile
import org.kotlinlsp.utils.trace

class AnnotationsResolverFactory : KotlinAnnotationsResolverFactory {
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver {
        return AnnotationsResolver()
    }
}

// TODO Implement this
class AnnotationsResolver : KotlinAnnotationsResolver {
    override fun declarationsByAnnotation(annotationClassId: ClassId): Set<KtAnnotated> =
        profile("[X] declarationsByAnnotation", "$annotationClassId") {
            emptySet()
        }

    override fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId> =
        profile("[X] annotationsOnDeclaration", "$declaration") {
            emptySet()
        }
}
