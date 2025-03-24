# Kotlin incremental analysis PoC

This is a PoC to perform incremental analysis on a JVM kotlin project leveraring the [JetBrains Kotlin Analysis API](https://github.com/JetBrains/kotlin/blob/master/docs/analysis/analysis-api/analysis-api.md). 

This API is used by implementing the [Platform Interface](https://github.com/JetBrains/kotlin/blob/master/docs/analysis/analysis-api/analysis-api-platform-interface.md), which needs to provide the Analysis API information about the project files and means to get there (e.g. from memory, from disk...), an indexing solution, among other components. This is the same way the IntelliJ Kotlin Plugin uses the API.

If this PoC is successful, it could serve as a base to develop a Kotlin Language Server which much better performance than the [existing one](https://github.com/fwcd/kotlin-language-server) (or even integrate the results of this PoC in that language server). Also maintenance will be drastically reduced as the most complex part will be handled by Analysis API and won't change frequently as long as it becomes stable.

## Current state
Currently, the PoC is non functional, as we need to implement more and more interfaces to comply with the platform requirements. The goal right now is to implement the minimum subset needed to perform diagnostics over a kotlin file and a modification which updates the diagnostics in real time.

## General recommendations 
It is recommended to clone the following repositories (with `--depth=1` and sparse checkout) to ease the development of the PoC:
- [Kotlin](https://github.com/JetBrains/kotlin): just the `analysis` folder which contains the Analysis API source code. The most useful folders here are the platform interface module and the `standalone` module which is a read-only, static implementation of the platform interface, which can be used as a base to know how to develop a new platform interface.
- [IntelliJ IDEA Community](https://github.com/JetBrains/intellij-community): just the `plugins/kotlin` folder. The IDEA kotlin plugin implements the platform interface to do the analysis, and it serves us as a base implementation as well.

## How to contribute 
- Clone the project
- Run `./gradlew run` and see the next error in the PoC (much likely a "missing service" or "missing extension point" exception)
- Implement the missing platform interface (using the standalone and IDEA kotlin plugin as a reference) and register it. All interfaces which begin with `Kotlin...` are platform interfaces, which we need to implement ourselves. Interfaces beginning with `Ka...` are engine services and are already implemented by the Analysis API (most likely by the FIR module), so we need to search in the analysis API module a XML file which names the corresponding `Ka...` interface, which will contain the class name of the implementation for that engine service. After that, just register it the same way as the platform interfaces.
