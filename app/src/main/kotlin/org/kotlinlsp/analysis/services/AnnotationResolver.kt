package org.kotlinlsp.analysis.services

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolver
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceAnd
import org.kotlinlsp.common.profile

class AnnotationsResolverFactory : KotlinAnnotationsResolverFactory {
    private lateinit var project: Project

    fun setup(project: Project) {
        this.project = project
    }

    override fun createAnnotationResolver(searchScope: GlobalSearchScope): KotlinAnnotationsResolver {
        return AnnotationsResolver(project, searchScope)
    }
}

class AnnotationsResolver(
    project: Project,
    scope: GlobalSearchScope
) : KotlinAnnotationsResolver {
    private val declarationProvider: KotlinDeclarationProvider by lazy {
        project.createDeclarationProvider(scope, contextualModule = null)
    }

    override fun declarationsByAnnotation(annotationClassId: ClassId): Set<KtAnnotated> =
        profile("[X] declarationsByAnnotation", "$annotationClassId") {
            // TODO
            emptySet()
        }

    override fun annotationsOnDeclaration(declaration: KtAnnotated): Set<ClassId> =
        profile("annotationsOnDeclaration", "$declaration") {
            // This one is straight copied from standalone platform
            declaration.annotationEntries
                .asSequence()
                .flatMap { it.typeReference?.resolveAnnotationClassIds(declarationProvider).orEmpty() }
                .toSet()
        }
}

private fun KtTypeReference.resolveAnnotationClassIds(
    declarationProvider: KotlinDeclarationProvider,
    candidates: MutableSet<ClassId> = mutableSetOf()
): Set<ClassId> {
    val annotationTypeElement = typeElement as? KtUserType
    val referencedName = annotationTypeElement?.referencedFqName() ?: return emptySet()
    if (referencedName.isRoot) return emptySet()

    if (!referencedName.parent().isRoot) {
        // we assume here that the annotation is used by its fully-qualified name
        return buildSet { referencedName.resolveToClassIds(this, declarationProvider) }
    }

    val targetName = referencedName.shortName()
    for (import in containingKtFile.importDirectives) {
        val importedName = import.importedFqName ?: continue
        when {
            import.isAllUnder -> importedName.child(targetName).resolveToClassIds(candidates, declarationProvider)
            importedName.shortName() == targetName -> importedName.resolveToClassIds(candidates, declarationProvider)
        }
    }

    containingKtFile.packageFqName.child(targetName).resolveToClassIds(candidates, declarationProvider)
    return candidates
}

private fun KtUserType.referencedFqName(): FqName? {
    val allTypes = generateSequence(this) { it.qualifier }.toList().asReversed()
    val allQualifiers = allTypes.map { it.referencedName ?: return null }

    return FqName.fromSegments(allQualifiers)
}

private fun FqName.resolveToClassIds(to: MutableSet<ClassId>, declarationProvider: KotlinDeclarationProvider) {
    toClassIdSequence().mapNotNullTo(to) { classId ->
        val classes = declarationProvider.getAllClassesByClassId(classId)
        val typeAliases = declarationProvider.getAllTypeAliasesByClassId(classId)
        typeAliases.singleOrNull()?.getTypeReference()?.resolveAnnotationClassIds(declarationProvider, to)

        val annotations = classes.filterIsInstanceAnd<KtClass> { it.isAnnotation() }
        annotations.singleOrNull()?.let {
            classId
        }
    }
}

private fun FqName.toClassIdSequence(): Sequence<ClassId> {
    var currentName = shortNameOrSpecial()
    if (currentName.isSpecial) return emptySequence()
    var currentParent = parentOrNull() ?: return emptySequence()
    var currentRelativeName = currentName.asString()

    return sequence {
        while (true) {
            yield(ClassId(currentParent, FqName(currentRelativeName), isLocal = false))
            currentName = currentParent.shortNameOrSpecial()
            if (currentName.isSpecial) break
            currentParent = currentParent.parentOrNull() ?: break
            currentRelativeName = "${currentName.asString()}.$currentRelativeName"
        }
    }
}
