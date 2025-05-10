package org.kotlinlsp.index.db

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.kotlinlsp.common.info
import org.kotlinlsp.index.db.serializers.deserializeStringListForDb
import org.kotlinlsp.index.db.serializers.serializeStringListForDb
import org.rocksdb.RocksDB
import java.nio.charset.Charset
import java.sql.Connection
import java.sql.Statement
import java.time.Instant

data class File(
    val path: String,
    val packageFqName: String,
    val lastModified: Instant,
    val modificationStamp: Long
)

fun File.toDto(): FileDto = FileDto(
    packageFqName = packageFqName,
    lastModified = lastModified.toEpochMilli(),
    modificationStamp = modificationStamp
)

data class FileDto(
    val packageFqName: String,
    val lastModified: Long,
    val modificationStamp: Long
)

fun serializeFileDtoForDb(dto: FileDto): ByteArray = Gson().toJson(dto).toByteArray()
fun deserializeFileDtoForDb(data: ByteArray): FileDto = Gson().fromJson(data.toString(Charset.defaultCharset()), FileDto::class.java)

fun Database.fileLastModifiedFromPath(path: String): Pair<Instant, Long>? {
    return filesDb.get(path, ::deserializeFileDtoForDb)?.let { Pair(Instant.ofEpochMilli(it.lastModified), it.modificationStamp) }
}

fun Database.setFile(file: File) {
    val dto = file.toDto()
    val previousPackageFqName = filesDb.get(file.path, ::deserializeFileDtoForDb)?.packageFqName

    filesDb.put(file.path, dto, ::serializeFileDtoForDb)

    if(previousPackageFqName != file.packageFqName) {
        // Remove previous package name
        if(previousPackageFqName != null) {
            val files = packagesDb.get(previousPackageFqName, ::deserializeStringListForDb)?.toMutableList() ?: mutableListOf()
            files.remove(file.path)
            if(files.size == 0) {
                packagesDb.remove(previousPackageFqName)
            } else {
                packagesDb.put(previousPackageFqName, files, ::serializeStringListForDb)
            }
        }

        // Add new one
        val files = packagesDb.get(file.packageFqName, ::deserializeStringListForDb)?.toMutableList() ?: mutableListOf()
        files.add(file.path)
        packagesDb.put(file.packageFqName, files, ::serializeStringListForDb)
    }
}
