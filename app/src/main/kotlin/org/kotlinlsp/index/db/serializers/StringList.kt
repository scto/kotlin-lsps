package org.kotlinlsp.index.db.serializers

import java.nio.charset.Charset

fun serializeStringListForDb(value: List<String>): ByteArray = value.joinToString(",").toByteArray()
fun deserializeStringListForDb(data: ByteArray): List<String> = data.toString(Charset.defaultCharset()).split(",")
