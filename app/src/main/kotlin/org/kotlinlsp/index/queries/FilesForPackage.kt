package org.kotlinlsp.index.queries

import org.jetbrains.kotlin.name.FqName
import org.kotlinlsp.index.Index
import org.kotlinlsp.index.db.serializers.deserializeStringListForDb

fun Index.filesForPackage(fqName: FqName): List<String> = query { db ->
    db.packagesDb.get(fqName.asString(), ::deserializeStringListForDb) ?: emptyList()
}
