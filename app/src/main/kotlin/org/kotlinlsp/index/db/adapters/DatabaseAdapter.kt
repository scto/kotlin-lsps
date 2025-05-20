package org.kotlinlsp.index.db.adapters

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

interface DatabaseAdapter {
    fun putByteArray(key: String, value: ByteArray)
    fun putByteArrayBulk(values: Iterable<Pair<String, ByteArray>>)
    fun getByteArray(key: String): ByteArray?
    fun prefixSearch(prefix: String): Sequence<Pair<String, ByteArray>>
    fun remove(key: String)
    fun close()
    fun deleteDb()
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T: Any> DatabaseAdapter.put(key: String, value: T) {
    putByteArray(key, ProtoBuf.encodeToByteArray(value))
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T: Any> DatabaseAdapter.putBulk(values: Iterable<Pair<String, T>>) {
    putByteArrayBulk(values.map { Pair(it.first, ProtoBuf.encodeToByteArray(it.second)) })
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> DatabaseAdapter.get(key: String): T? {
    val data = getByteArray(key) ?: return null
    return ProtoBuf.decodeFromByteArray<T>(data)
}
