package org.kotlinlsp.services

import org.jetbrains.kotlin.cli.jvm.compiler.*

val latestLanguageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)

