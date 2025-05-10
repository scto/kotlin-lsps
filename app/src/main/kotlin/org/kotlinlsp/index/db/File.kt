package org.kotlinlsp.index.db

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.kotlinlsp.common.info
import org.rocksdb.RocksDB
import java.nio.charset.Charset
import java.sql.Connection
import java.sql.Statement
import java.time.Instant

data class File(
    val path: String,
    val packageFqName: String,
    val lastModified: Instant
)

data class FileDto(
    val packageFqName: String,
    val lastModified: Long
)

fun Database.fileLastModifiedFromPath(path: String): Instant? {
    val dataJson = files.db.fetch(path) ?: return null
    val data = Gson().fromJson(dataJson.toString(Charset.defaultCharset()), FileDto::class.java)
    return Instant.ofEpochMilli(data.lastModified)
}

fun Database.setFile(file: File) {
    val dto = FileDto(packageFqName = file.packageFqName, lastModified = file.lastModified.toEpochMilli())
    val previousPackageFqName = files.db.fetch(file.path)?.let { Gson().fromJson(it.toString(Charset.defaultCharset()), FileDto::class.java).packageFqName }

    files.db.put(file.path.toByteArray(), Gson().toJson(dto).toByteArray())

    if(previousPackageFqName != file.packageFqName) {
        // Remove previous package name
        if(previousPackageFqName != null) {
            val files = packages.db.fetch(previousPackageFqName)?.asStringList()?.toMutableList() ?: mutableListOf()
            files.remove(file.path)
            if(files.size == 0) {
                packages.db.delete(previousPackageFqName.toByteArray())
            } else {
                packages.db.put(previousPackageFqName.toByteArray(), files.joinToString(",").toByteArray())
            }
        }

        // Add new one
        val files = packages.db.fetch(file.packageFqName)?.asStringList()?.toMutableList() ?: mutableListOf()
        files.add(file.path)
        packages.db.put(file.packageFqName.toByteArray(), files.joinToString(",").toByteArray())
    }
}
