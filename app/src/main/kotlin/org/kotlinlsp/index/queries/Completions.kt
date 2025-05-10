package org.kotlinlsp.index.queries

import org.kotlinlsp.index.Index

fun Index.getCompletions(prefix: String): List<String> = query {
    it.declarationsDb.prefixSearch(prefix)
        .map { (key, _) -> key.split(":").first() }
        .toList()
}
