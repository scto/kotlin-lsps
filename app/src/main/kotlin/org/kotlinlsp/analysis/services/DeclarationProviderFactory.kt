package org.kotlinlsp.analysis.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.kotlinlsp.analysis.services.utils.virtualFilesForPackage
import org.kotlinlsp.debug
import org.kotlinlsp.trace

class DeclarationProvider(private val scope: GlobalSearchScope, private val project: Project): KotlinDeclarationProvider {
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false   // TODO 
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false   // TODO 

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        trace("[X] findFilesForFacade: $facadeFqName")
        return emptyList()  // TODO
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        trace("[X] findFilesForFacadeByPackage: $packageFqName")
        return emptyList()  // TODO
    }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> {
        trace("[X] findFilesForScript: $scriptFqName")
        return emptyList()  // TODO
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        trace("[X] findInternalFilesForFacade: $facadeFqName")
        return emptyList()  // TODO
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        trace("[X] getAllClassesByClassId: $classId")
        return emptyList()  // TODO
    }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        trace("[X] getAllTypeAliasesByClassId: $classId")
        return emptyList()  // TODO
    }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        trace("getClassLikeDeclarationByClassId: $classId")
        virtualFilesForPackage(project, scope, classId.packageFqName).forEach {
            val ktFile = PsiManager.getInstance(project).findFile(it)!!
            val declaration = ktFile.children
                .filterIsInstance<KtClassLikeDeclaration>()
                .find {
                    val itClassId = it.getClassId() ?: return@find false
                    return@find itClassId == classId
                }

            if(declaration != null) {
                return declaration
            }
        }
        return null
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        trace("getTopLevelCallableFiles: $callableId")
        val files = mutableListOf<KtFile>()
        virtualFilesForPackage(project, scope, callableId.packageName).forEach {
            val ktFile = PsiManager.getInstance(project).findFile(it)!! as KtFile
            files.add(ktFile)
        }
        return files
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        trace("[X] getTopLevelFunctions: $callableId")
        return emptyList()  // TODO
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        trace("getTopLevelKotlinClassLikeDeclarationNamesInPackage: $packageFqName")

        val names = mutableSetOf<Name>()

        virtualFilesForPackage(project, scope, packageFqName).forEach {
            val ktFile = PsiManager.getInstance(project).findFile(it)!!
            val declarations = ktFile.children.filterIsInstance<KtClassLikeDeclaration>().map { Name.identifier(it.name!!) }
            names.addAll(declarations)
        }

        return names
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        trace("getTopLevelCallableNamesInPackage: $packageFqName")

        val names = mutableSetOf<Name>()

        virtualFilesForPackage(project, scope, packageFqName).forEach { it ->
            val ktFile = PsiManager.getInstance(project).findFile(it)!!
            val declarations = ktFile.children.filterIsInstance<KtCallableDeclaration>().mapNotNull { it.name }.map { Name.identifier(it) }
            names.addAll(declarations)
        }

        return names
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        trace("[X] getTopLevelProperties: $callableId")
        return emptyList()  // TODO
    }
}

class DeclarationProviderFactory: KotlinDeclarationProviderFactory {
    private lateinit var project: Project

    fun setup(project: Project) {
        this.project = project
    }

    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?
    ): KotlinDeclarationProvider {
        return DeclarationProvider(scope, project)
    }
}
