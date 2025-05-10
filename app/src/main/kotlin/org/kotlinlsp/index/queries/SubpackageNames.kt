package org.kotlinlsp.index.queries

import org.kotlinlsp.index.Index
import org.rocksdb.ReadOptions
import java.nio.charset.Charset

fun Index.subpackageNames(fqName: String): Set<String> = query { db ->
    val readOptions = ReadOptions().setPrefixSameAsStart(true)
    val it = db.packages.db.newIterator(readOptions)
    it.seek(fqName.toByteArray())

    val subpackages = mutableSetOf<String>()
    while (it.isValid) {
        val key = it.key()
        val keyString = key.toString(Charset.defaultCharset())
        if (!keyString.startsWith(fqName)) break


        if(keyString != fqName) {
            val name = keyString.removePrefix("$fqName.")
            val parts = name.split(".")
            if (parts.isNotEmpty()) {
                subpackages.add(parts.first())
            }
        }

        it.next()
    }

    subpackages
}
