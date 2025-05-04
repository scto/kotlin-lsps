package org.kotlinlsp.index.queries

import org.jetbrains.kotlin.name.FqName
import org.kotlinlsp.index.Index

fun Index.filesForPackage(fqName: FqName): List<String> = query { conn ->
    val query = "SELECT path FROM Files WHERE packageFqName = ?"
    conn.prepareStatement(query).use {
        it.setString(1, fqName.asString())
        val resultSet = it.executeQuery()
        val paths = mutableListOf<String>()
        while (resultSet.next()) paths.add(resultSet.getString("path"))
        paths
    }
}
