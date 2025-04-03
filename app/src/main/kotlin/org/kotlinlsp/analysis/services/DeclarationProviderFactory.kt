package org.kotlinlsp.analysis.services

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

class DeclarationProvider: KotlinDeclarationProvider {
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false   // TODO 
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false   // TODO 

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return emptyList()  // TODO
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        return emptyList()  // TODO
    }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> {
        return emptyList()  // TODO
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return emptyList()  // TODO
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        return emptyList()  // TODO
    }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        return emptyList()  // TODO
    }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        return null    // TODO
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        return emptyList()  // TODO
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return emptySet()   // TODO
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        return emptyList()  // TODO
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return emptySet()   // TODO
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        return emptyList()  // TODO
    }

}

class DeclarationProviderFactory: KotlinDeclarationProviderFactory {
    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?
    ): KotlinDeclarationProvider {
        return DeclarationProvider()
    }
}
