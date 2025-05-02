package org.kotlinlsp.index.db

data class ReferenceRecord(
    val id: Int,
    val symbolId: Int,  // A symbol has multiple references to it
    val startOffset: Int,
    val endOffset: Int
)
