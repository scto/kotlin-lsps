package org.kotlinlsp.index.queries

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.kotlinlsp.index.Index
import org.kotlinlsp.index.db.Declaration

@OptIn(ExperimentalSerializationApi::class)
fun Index.getCompletions(prefix: String, thisRef: String, receiver: String): List<Declaration> = query {
    it.declarationsDb.prefixSearch(prefix)
        .map { ProtoBuf.decodeFromByteArray<Declaration>(it.second) }
        .filter {
            when (it) {
                is Declaration.Function -> it.receiverFqName == receiver || it.receiverFqName.isEmpty()
                is Declaration.Class -> true
                is Declaration.EnumEntry -> true
                is Declaration.Field -> it.parentFqName == receiver || it.parentFqName.isEmpty()
            }
        }
        .toList()
}
