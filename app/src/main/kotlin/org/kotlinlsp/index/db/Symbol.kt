package org.kotlinlsp.index.db

// A symbol is a definition (e.g. variable, function...)
data class SymbolRecord(
    val id: Int,
    val file: Int,      // A symbol corresponds to a file
    val startOffset: Int,
    val endOffset: Int,
    val name: String,
    val kind: Int,  // TODO Define the possible values
    val parentSymbol: Int?
)
