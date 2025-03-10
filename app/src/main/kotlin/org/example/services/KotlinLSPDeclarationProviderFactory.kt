package org.example.services

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

class KotlinLSPDeclarationProvider: KotlinDeclarationProvider {
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = TODO("Not yet implemented")
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = TODO("Not yet implemented")

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        TODO("Not yet implemented")
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        TODO("Not yet implemented")
    }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> {
        TODO("Not yet implemented")
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        TODO("Not yet implemented")
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        TODO("Not yet implemented")
    }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        TODO("Not yet implemented")
    }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        TODO("Not yet implemented")
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        TODO("Not yet implemented")
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        TODO("Not yet implemented")
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        TODO("Not yet implemented")
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        TODO("Not yet implemented")
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        TODO("Not yet implemented")
    }

}

class KotlinLSPDeclarationProviderFactory: KotlinDeclarationProviderFactory {
    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?
    ): KotlinDeclarationProvider {
        return KotlinLSPDeclarationProvider()
    }
}
