package org.kotlinlsp.index.db.adapters

interface DatabaseAdapter {
    fun <T> put(key: String, value: T, serializer: (T) -> ByteArray)
    fun <T> get(key: String, deserializer: (ByteArray) -> T): T?
    fun prefixSearch(prefix: String): Sequence<Pair<String, ByteArray>>
    fun remove(key: String)
    fun close()
    fun deleteDb()
}
