package org.kotlinlsp.index.queries

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.FqName
import org.kotlinlsp.index.Index

// TODO Use searchScope for correct behaviour
fun Index.packageExistsInSourceFiles(fqName: FqName, searchScope: GlobalSearchScope): Boolean = query { conn ->
    val query = "SELECT EXISTS (SELECT 1 FROM Files WHERE packageFqName = ?)"
    conn.prepareStatement(query).use {
        it.setString(1, fqName.asString())
        val resultSet = it.executeQuery()
        resultSet.next()
        resultSet.getBoolean(1)
    }
}
