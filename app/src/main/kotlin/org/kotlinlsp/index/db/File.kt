package org.kotlinlsp.index.db

import kotlinx.serialization.Serializable
import org.kotlinlsp.index.db.adapters.get
import org.kotlinlsp.index.db.adapters.put
import java.time.Instant

data class File(
    val path: String,
    val packageFqName: String,
    val lastModified: Instant,
    val modificationStamp: Long,
    val indexed: Boolean,
)

fun File.toDto(): FileDto = FileDto(
    packageFqName = packageFqName,
    lastModified = lastModified.toEpochMilli(),
    modificationStamp = modificationStamp,
    indexed = indexed,
)

@Serializable
data class FileDto(
    val packageFqName: String,
    val lastModified: Long,
    val modificationStamp: Long,
    val indexed: Boolean,
)

fun Database.fileLastModifiedFromPath(path: String): Pair<Instant, Long>? {
    return filesDb.get<FileDto>(path)?.let { Pair(Instant.ofEpochMilli(it.lastModified), it.modificationStamp) }
}

fun Database.fileLastModifiedAndIndexedFromPath(path: String): Triple<Instant, Long, Boolean>? {
    return filesDb.get<FileDto>(path)?.let {
        Triple(Instant.ofEpochMilli(it.lastModified), it.modificationStamp, it.indexed)
    }
}

fun Database.setFile(file: File) {
    val dto = file.toDto()
    val previousPackageFqName = filesDb.get<FileDto>(file.path)?.packageFqName

    filesDb.put(file.path, dto)

    if(previousPackageFqName != file.packageFqName) {
        // Remove previous package name
        if(previousPackageFqName != null) {
            val files = packagesDb.get<List<String>>(previousPackageFqName)?.toMutableList() ?: mutableListOf()
            files.remove(file.path)
            if(files.size == 0) {
                packagesDb.remove(previousPackageFqName)
            } else {
                packagesDb.put(previousPackageFqName, files)
            }
        }

        // Add new one
        val files = packagesDb.get<List<String>>(file.packageFqName)?.toMutableList() ?: mutableListOf()
        files.add(file.path)
        packagesDb.put(file.packageFqName, files)
    }
}
