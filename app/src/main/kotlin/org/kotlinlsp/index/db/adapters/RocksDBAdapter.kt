package org.kotlinlsp.index.db.adapters

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import org.rocksdb.InfoLogLevel
import org.rocksdb.Options
import org.rocksdb.ReadOptions
import org.rocksdb.RocksDB
import java.io.File
import java.io.Serial
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.reflect.KClass

class RocksDBAdapter(private val path: Path): DatabaseAdapter {
    companion object {
        init {
            RocksDB.loadLibrary()
        }

        val options = Options().apply {
            setCreateIfMissing(true)
            setKeepLogFileNum(1)
            setInfoLogLevel(InfoLogLevel.FATAL_LEVEL)
        }
    }

    private val db = RocksDB.open(options, path.absolutePathString())

    override fun putByteArray(key: String, value: ByteArray) {
        db.put(key.toByteArray(), value)
    }

    override fun getByteArray(key: String): ByteArray? {
        val data = db.get(key.toByteArray()) ?: return null
        return data
    }

    override fun prefixSearch(prefix: String): Sequence<Pair<String, ByteArray>> = sequence {
        val readOptions = ReadOptions().setPrefixSameAsStart(true)

        readOptions.use {
            val iterator = db.newIterator(readOptions)
            iterator.seek(prefix.toByteArray())

            while (iterator.isValid) {
                val key = iterator.key()
                val keyString = key.toString(Charset.defaultCharset())
                if (!keyString.startsWith(prefix)) break

                yield(Pair(keyString, iterator.value()))
                iterator.next()
            }
        }
    }

    override fun remove(key: String) {
        db.delete(key.toByteArray())
    }

    override fun close() {
        db.close()
    }

    override fun deleteDb() {
        if(!db.isClosed) db.close()
        File(path.absolutePathString()).deleteRecursively()
    }
}
