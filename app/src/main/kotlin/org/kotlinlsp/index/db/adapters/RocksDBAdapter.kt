package org.kotlinlsp.index.db.adapters

import org.rocksdb.InfoLogLevel
import org.rocksdb.Options
import org.rocksdb.ReadOptions
import org.rocksdb.RocksDB
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.absolutePathString

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

    override fun <T> put(key: String, value: T, serializer: (T) -> ByteArray) {
        db.put(key.toByteArray(), serializer(value))
    }

    override fun <T> get(key: String, deserializer: (ByteArray) -> T): T? {
        val data: ByteArray? = db.get(key.toByteArray())
        if(data == null) return null
        return deserializer(data)
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
