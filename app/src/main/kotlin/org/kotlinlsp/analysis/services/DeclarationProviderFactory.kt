package org.kotlinlsp.analysis.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.kotlinlsp.common.profile
import org.kotlinlsp.common.read
import org.kotlinlsp.index.Index
import org.kotlinlsp.index.queries.filesForPackage

// TODO Use the scope for correct behaviour
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
        get() = false
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> =
        profile("[X] findFilesForFacade", "$facadeFqName") {
            emptyList()  // TODO
        }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> =
        profile("findInternalFilesForFacade", "$facadeFqName") {
            // We don't deserialize libraries from stubs so we can return empty here safely
            // We don't take the KaBuiltinsModule into account for simplicity,
            // that means we expect the kotlin stdlib to be included on the project
            emptyList()
        }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> =
        profile("findFilesForFacadeByPackage", "$packageFqName") {
            virtualFilesForPackage(packageFqName).map { getKtFile(it) }.toList()
        }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> =
        profile("findFilesForScript", "$scriptFqName") {
            virtualFilesForPackage(scriptFqName).map { getKtFile(it) }.mapNotNull { it.script }.toList()
        }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> =
        profile("getAllClassesByClassId", "$classId") {
            virtualFilesForPackage(classId.packageFqName)
                .map { getKtFile(it) }
                .map {
                    project.read {
                        PsiTreeUtil.collectElementsOfType(it, KtClassOrObject::class.java).asSequence()
                    }
                }
                .flatten()
                .filter { it.getClassId() == classId }
                .toList()
        }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> =
        profile("getAllTypeAliasesByClassId", "$classId") {
            virtualFilesForPackage(classId.packageFqName)
                .map { getKtFile(it) }
                .map {
                    project.read {
                        PsiTreeUtil.collectElementsOfType(it, KtTypeAlias::class.java).asSequence()
                    }
                }
                .flatten()
                .filter { it.getClassId() == classId }
                .toList()
        }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? =
        profile("getClassLikeDeclarationByClassId", "$classId") {
            getAllClassesByClassId(classId).firstOrNull()
                ?: getAllTypeAliasesByClassId(classId).firstOrNull()
        }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> =
        profile("getTopLevelCallableFiles", "$callableId") {
            buildSet {
                getTopLevelProperties(callableId).mapTo(this) { it.containingKtFile }
                getTopLevelFunctions(callableId).mapTo(this) { it.containingKtFile }
            }
        }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        profile("getTopLevelFunctions", "$callableId") {
            virtualFilesForPackage(callableId.packageName)
                .map { getKtFile(it) }
                .map {
                    project.read {
                        PsiTreeUtil.collectElementsOfType(it, KtNamedFunction::class.java).asSequence()
                    }
                }
                .flatten()
                .filter { it.isTopLevel }
                .filter { it.nameAsName == callableId.callableName }
                .toList()
        }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> =
        profile("getTopLevelKotlinClassLikeDeclarationNamesInPackage", "$packageFqName") {
            virtualFilesForPackage(packageFqName)
                .map { getKtFile(it) }
                .map {
                    project.read {
                        PsiTreeUtil.collectElementsOfType(it, KtClassLikeDeclaration::class.java).asSequence()
                    }
                }
                .flatten()
                .filter { it.isTopLevelKtOrJavaMember() }
                .mapNotNull { it.nameAsName }
                .toSet()
        }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
        profile("getTopLevelCallableNamesInPackage", "$packageFqName") {
            virtualFilesForPackage(packageFqName)
                .map { getKtFile(it) }
                .map {
                    project.read {
                        PsiTreeUtil.collectElementsOfType(it, KtCallableDeclaration::class.java).asSequence()
                    }
                }
                .flatten()
                .filter { it.isTopLevelKtOrJavaMember() }
                .mapNotNull { it.nameAsName }
                .toSet()
        }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        profile("getTopLevelProperties", "$callableId") {
            virtualFilesForPackage(callableId.packageName)
                .map { getKtFile(it) }
                .map {
                    project.read {
                        PsiTreeUtil.collectElementsOfType(it, KtProperty::class.java).asSequence()
                    }
                }
                .flatten()
                .filter { it.isTopLevel }
                .filter { it.nameAsName == callableId.callableName }
                .toList()
        }

    private fun virtualFilesForPackage(fqName: FqName): Sequence<VirtualFile> {
        return index.filesForPackage(fqName, scope).asSequence().map { VirtualFileManager.getInstance().findFileByUrl(it)!! }
    }

    // TODO Look in opened files first to take in memory modifications into account
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