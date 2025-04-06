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
import org.kotlinlsp.log

class DeclarationProvider(private val scope: GlobalSearchScope): KotlinDeclarationProvider {
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false   // TODO 
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false   // TODO 

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        log("findFilesForFacade: $facadeFqName")
        return emptyList()  // TODO
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        log("findFilesForFacadeByPackage: $packageFqName")
        return emptyList()  // TODO
    }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> {
        log("findFilesForScript: $scriptFqName")
        return emptyList()  // TODO
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        log("findInternalFilesForFacade: $facadeFqName")
        return emptyList()  // TODO
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        log("getAllClassesByClassId: $classId")
        return emptyList()  // TODO
    }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        log("getAllTypeAliasesByClassId: $classId")
        return emptyList()  // TODO
    }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        log("getClassLikeDeclarationByClassId: $classId")
        return null    // TODO
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        log("getTopLevelCallableFiles: $callableId")
        return emptyList()  // TODO
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        log("getTopLevelFunctions: $callableId")
        return emptyList()  // TODO
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        log("getTopLevelKotlinClassLikeDeclarationNamesInPackage: $packageFqName")
        return emptySet()   // TODO
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        log("getTopLevelCallableNamesInPackage: $packageFqName")
        return emptySet()   // TODO
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        log("getTopLevelProperties: $callableId")
        return emptyList()  // TODO
    }
}

class DeclarationProviderFactory: KotlinDeclarationProviderFactory {
    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?
    ): KotlinDeclarationProvider {
        return DeclarationProvider(scope)
    }
}
