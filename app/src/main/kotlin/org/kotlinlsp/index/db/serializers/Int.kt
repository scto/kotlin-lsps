package org.kotlinlsp.index.db.serializers

import java.nio.ByteBuffer

fun serializeIntForDb(value: Int): ByteArray = ByteBuffer.allocate(4).putInt(value).array()
fun deserializeIntForDb(data: ByteArray) = ByteBuffer.wrap(data).getInt()
