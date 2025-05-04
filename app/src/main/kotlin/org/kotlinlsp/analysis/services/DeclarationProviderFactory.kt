package org.kotlinlsp.analysis.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.kotlinlsp.common.profile
import org.kotlinlsp.common.read
import org.kotlinlsp.index.Index
import org.kotlinlsp.index.queries.filesForPackage

class DeclarationProvider(
    val scope: GlobalSearchScope,
    private val project: Project,
    private val index: Index
): KotlinDeclarationProvider {
    // This cache prevents parsing KtFiles over and over
    private val ktFileCache = Caffeine.newBuilder()
        .maximumSize(100)
        .build<String, KtFile>()

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false   // TODO 
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false   // TODO 

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> =
        profile("[X] findFilesForFacade", "$facadeFqName") {
            emptyList()  // TODO
        }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> =
        profile("[X] findFilesForFacadeByPackage", "$packageFqName") {
            emptyList()  // TODO
        }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> =
        profile("[X] findFilesForScript", "$scriptFqName") {
            emptyList()  // TODO
        }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> =
        profile("[X] findInternalFilesForFacade", "$facadeFqName") {
            emptyList()  // TODO
        }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> =
        profile("[X] getAllClassesByClassId", "$classId") {
            emptyList()  // TODO
        }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> =
        profile("[X] getAllTypeAliasesByClassId", "$classId") {
            emptyList()  // TODO
        }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? =
        profile("getClassLikeDeclarationByClassId", "$classId") {
            virtualFilesForPackage(classId.packageFqName).forEach {
                val ktFile = getKtFile(it)
                val declaration = project.read {
                    ktFile.children
                        .filterIsInstance<KtClassLikeDeclaration>()
                        .find {
                            val itClassId = it.getClassId() ?: return@find false
                            return@find itClassId == classId
                        }
                }

                if (declaration != null) {
                    return@profile declaration
                }
            }
            null
        }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> =
        profile("getTopLevelCallableFiles", "$callableId") {
            virtualFilesForPackage(callableId.packageName).map { getKtFile(it) }.toList()
        }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        profile("[X] getTopLevelFunctions", "$callableId") {
            emptyList()  // TODO
        }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> =
        profile("getTopLevelKotlinClassLikeDeclarationNamesInPackage", "$packageFqName") {
            virtualFilesForPackage(packageFqName).map {
                val ktFile = getKtFile(it)
                project.read {
                    ktFile.children.filterIsInstance<KtClassLikeDeclaration>().map { Name.identifier(it.name!!) }
                }.asSequence()
            }
                .flatten()
                .toSet()
        }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
        profile("getTopLevelCallableNamesInPackage", "$packageFqName") {
            virtualFilesForPackage(packageFqName).map { it ->
                val ktFile = getKtFile(it)
                project.read {
                    ktFile.children.filterIsInstance<KtCallableDeclaration>().mapNotNull { it.name }
                        .map { Name.identifier(it) }.asSequence()
                }
            }
                .flatten()
                .toSet()
        }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        profile("[X] getTopLevelProperties", "$callableId") {
            emptyList()  // TODO
        }

    private fun virtualFilesForPackage(fqName: FqName): Sequence<VirtualFile> {
        return index.filesForPackage(fqName, scope).asSequence().map { VirtualFileManager.getInstance().findFileByUrl(it)!! }
    }

    private fun getKtFile(virtualFile: VirtualFile): KtFile {
        val cachedKtFile = ktFileCache.getIfPresent(virtualFile.url)
        if(cachedKtFile != null) return cachedKtFile

        val ktFile = project.read { PsiManager.getInstance(project).findFile(virtualFile)!! as KtFile }
        ktFileCache.put(virtualFile.url, ktFile)
        return ktFile
    }
}

class DeclarationProviderFactory: KotlinDeclarationProviderFactory {
    private lateinit var project: Project
    private lateinit var index: Index

    fun setup(project: Project, index: Index) {
        this.project = project
        this.index = index
    }

    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KaModule?
    ): KotlinDeclarationProvider {
        return DeclarationProvider(scope, project, index)
    }
}

class DeclarationProviderMerger(private val project: Project) : KotlinDeclarationProviderMerger {
    override fun merge(providers: List<KotlinDeclarationProvider>): KotlinDeclarationProvider =
        providers.mergeSpecificProviders<_, DeclarationProvider>(KotlinCompositeDeclarationProvider.factory) { targetProviders ->
            val combinedScope = GlobalSearchScope.union(targetProviders.map { it.scope })
            project.createDeclarationProvider(combinedScope, contextualModule = null).apply {
                check(this is DeclarationProvider) {
                    "`DeclarationProvider` can only be merged into a combined declaration provider of the same type."
                }
            }
        }
}