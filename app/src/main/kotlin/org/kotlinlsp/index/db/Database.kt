package org.kotlinlsp.index.db

import org.kotlinlsp.common.getCachePath
import org.kotlinlsp.common.info
import org.rocksdb.Options
import org.rocksdb.RocksDB
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.absolutePathString

const val CURRENT_SCHEMA_VERSION = 1    // Increment on schema changes
const val VERSION_KEY = "__version"

class DbHandle(basePath: Path, name: String) {
    companion object {
        init {
            RocksDB.loadLibrary()
        }

        val options = Options().apply {
            setCreateIfMissing(true)
        }
    }
    val path = basePath.resolve(name).absolutePathString()
    val db = RocksDB.open(options, path)
}

class Database(rootFolder: String) {
    private val cachePath = getCachePath(rootFolder)
    val files: DbHandle
    val packages: DbHandle

    init {
        var project = DbHandle(cachePath, "project")
        val schemaVersion = project.db.getInt(VERSION_KEY)

        if(schemaVersion == null || schemaVersion != CURRENT_SCHEMA_VERSION) {

            // Schema version mismatch, wipe the db
            info("Index DB schema version mismatch, recreating!")
            project.db.close()
            deleteAll()

            project = DbHandle(cachePath, "project")
            project.db.putInt(VERSION_KEY, CURRENT_SCHEMA_VERSION)
        }

        files = DbHandle(cachePath, "files")
        packages = DbHandle(cachePath, "packages")
        project.db.close()
    }

    fun close() {
        files.db.close()
        packages.db.close()
    }

    private fun deleteAll() {
        File(cachePath.resolve("files").absolutePathString()).delete()
        File(cachePath.resolve("packages").absolutePathString()).delete()
    }
}

fun RocksDB.getInt(key: String): Int? = get(key.toByteArray())?.let { ByteBuffer.wrap(it).getInt() }
fun RocksDB.putInt(key: String, value: Int) = put(key.toByteArray(), ByteBuffer.allocate(4).putInt(value).array())
fun RocksDB.fetch(key: String): ByteArray? = get(key.toByteArray())

fun ByteArray.asStringList() = toString(Charset.defaultCharset()).split(",")