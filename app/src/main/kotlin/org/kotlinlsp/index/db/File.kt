package org.kotlinlsp.index.db

import java.time.Instant

data class FileRecord(
    val id: Int,
    val path: String,
    val packageFqName: String,
    val lastModified: Instant
)
