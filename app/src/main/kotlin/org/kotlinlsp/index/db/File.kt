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

fun Database.file(path: String): File? {
    return filesDb.get<FileDto>(path)?.let {
        File(
            path = path,
            packageFqName = it.packageFqName,
            lastModified = Instant.ofEpochMilli(it.lastModified),
            modificationStamp = it.modificationStamp,
            indexed = it.indexed,
        )
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
