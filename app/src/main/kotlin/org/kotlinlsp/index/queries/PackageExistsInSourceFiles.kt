package org.kotlinlsp.index.queries

import org.jetbrains.kotlin.name.FqName
import org.kotlinlsp.index.Index
import org.rocksdb.ReadOptions
import java.nio.charset.Charset

fun Index.packageExistsInSourceFiles(fqName: FqName): Boolean = query { db ->
    val prefix = fqName.asString()
    val readOptions = ReadOptions().setPrefixSameAsStart(true)
    val it = db.packages.db.newIterator(readOptions)
    it.seek(prefix.toByteArray())
    it.isValid && it.key().toString(Charset.defaultCharset()).startsWith(prefix)
}
