package org.kotlinlsp.index.queries

import org.kotlinlsp.index.Index

fun Index.getCompletions(prefix: String): List<String> = query {
    emptyList() // TODO
}
