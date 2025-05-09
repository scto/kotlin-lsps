package org.kotlinlsp.index.queries

import org.jetbrains.kotlin.name.FqName
import org.kotlinlsp.index.Index
import org.kotlinlsp.index.db.asStringList
import org.kotlinlsp.index.db.fetch

fun Index.filesForPackage(fqName: FqName): List<String> = query { db ->
    db.packages.db.fetch(fqName.asString())?.asStringList() ?: emptyList()
}
