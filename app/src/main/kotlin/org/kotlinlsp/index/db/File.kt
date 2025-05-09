package org.kotlinlsp.index.db

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

fun Database.fileLastModifiedFromPath(path: String): Instant? {
    val dataJson = files.db.fetch(path) ?: return null
    val data: Map<String, Any> = Gson().fromJson(dataJson.toString(Charset.defaultCharset()), object : TypeToken<Map<String, Any>>() {}.type)
    return Instant.ofEpochMilli((data["lastModified"] as Double).toLong())
}

fun Database.setFile(file: File) {
    val fileMetadata = Gson().toJson(mapOf("lastModified" to file.lastModified.toEpochMilli())).toByteArray()

    files.db.put(file.path.toByteArray(), fileMetadata)
    appendPackage(file.packageFqName, file.path)
}

private fun Database.appendPackage(packageFqName: String, filePath: String) {
    val files = packages.db.fetch(packageFqName)?.asStringList()?.toMutableList() ?: mutableListOf()
    files.add(filePath)
    packages.db.put(packageFqName.toByteArray(), files.joinToString(",").toByteArray())
}
