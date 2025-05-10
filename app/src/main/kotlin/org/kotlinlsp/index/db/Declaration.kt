package org.kotlinlsp.index.db

import org.kotlinlsp.index.db.serializers.serializeIntForDb

fun Database.putDeclarationForFile(path: String, declName: String, startOffset: Int, endOffset: Int) {
    val key = "${declName}:${path}:${startOffset}:${endOffset}"
    declarationsDb.put(key, 0, ::serializeIntForDb)    // TODO Add metadata
}
