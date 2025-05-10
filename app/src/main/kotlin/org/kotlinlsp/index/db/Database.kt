package org.kotlinlsp.index.db

import org.kotlinlsp.common.getCachePath
import org.kotlinlsp.common.info
import org.kotlinlsp.index.db.adapters.DatabaseAdapter
import org.kotlinlsp.index.db.adapters.RocksDBAdapter
import org.kotlinlsp.index.db.serializers.deserializeIntForDb
import org.kotlinlsp.index.db.serializers.serializeIntForDb
import org.rocksdb.InfoLogLevel
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

class Database(rootFolder: String) {
    private val cachePath = getCachePath(rootFolder)
    val filesDb: DatabaseAdapter
    val packagesDb: DatabaseAdapter

    init {
        var projectDb = RocksDBAdapter(cachePath.resolve("project"))
        val schemaVersion = projectDb.get(VERSION_KEY, ::deserializeIntForDb)

        if(schemaVersion == null || schemaVersion != CURRENT_SCHEMA_VERSION) {

            // Schema version mismatch, wipe the db
            info("Index DB schema version mismatch, recreating!")
            projectDb.close()
            deleteAll()

            projectDb = RocksDBAdapter(cachePath.resolve("project"))
            projectDb.put(VERSION_KEY, CURRENT_SCHEMA_VERSION, ::serializeIntForDb)
        }

        filesDb = RocksDBAdapter(cachePath.resolve("files"))
        packagesDb = RocksDBAdapter(cachePath.resolve("packages"))
        projectDb.close()
    }

    fun close() {
        filesDb.close()
        packagesDb.close()
    }

    private fun deleteAll() {
        File(cachePath.resolve("files").absolutePathString()).delete()
        File(cachePath.resolve("packages").absolutePathString()).delete()
    }
}
