package org.kotlinlsp.index.db

import org.kotlinlsp.index.db.adapters.put

fun Database.putDeclarationForFile(path: String, declName: String, startOffset: Int, endOffset: Int) {
    val key = "${declName}:${path}:${startOffset}:${endOffset}"
    declarationsDb.put(key, 0)    // TODO Add metadata
}
