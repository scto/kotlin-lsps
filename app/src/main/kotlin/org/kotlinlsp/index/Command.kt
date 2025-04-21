package org.kotlinlsp.index

import org.jetbrains.kotlin.psi.KtFile
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed class Command {
    data object Stop : Command()
    data class IndexFile @OptIn(ExperimentalTime::class) constructor(val ktFile: KtFile, val timestamp: Instant) : Command()
}
