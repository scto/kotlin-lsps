package org.kotlinlsp.analysis.services

import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings

class KotlinLSPPlatformSettings : KotlinPlatformSettings {
    override val deserializedDeclarationsOrigin: KotlinDeserializedDeclarationsOrigin
        get() = KotlinDeserializedDeclarationsOrigin.BINARIES   // Change to stubs
}
