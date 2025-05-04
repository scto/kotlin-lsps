package org.kotlinlsp.index.queries

import org.jetbrains.kotlin.name.FqName
import org.kotlinlsp.index.Index

fun Index.packageExistsInSourceFiles(fqName: FqName): Boolean = query { conn ->
    val query = "SELECT EXISTS (SELECT 1 FROM Files WHERE packageFqName = ?)"
    conn.prepareStatement(query).use {
        it.setString(1, fqName.asString())
        val resultSet = it.executeQuery()
        resultSet.next()
        resultSet.getBoolean(1)
    }
}
