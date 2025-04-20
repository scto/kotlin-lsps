# Kotlin Language Server

[![Status](https://github.com/amgdev9/kotlin-lsp/actions/workflows/push.yml/badge.svg)](https://github.com/amgdev9/kotlin-lsp/actions/workflows/push.yml)

This is an implementation of the [Language Server Protocol](https://microsoft.github.io/language-server-protocol/specification) for the [Kotlin](https://kotlinlang.org) programming language, leveraging the Kotlin [Analysis API](https://github.com/JetBrains/kotlin/blob/master/docs/analysis/analysis-api/analysis-api.md) to provide real time diagnostics, syntax and semantic analysis of Kotlin source files and libraries.

## Current status

Right now, this language server is at its infancy and thus not ready to use for production environments (yet). As of now, this language server is being prepared to analyse its own codebase, with upcoming support for other projects. Here are the most important steps to make in order to improve its usability:

- Integration with build systems: as of now, this language server does not integrate with any build system. For testing and development purposes, a Gradle implementation can be found in `buildsystem/Gradle.kt`, which just defines explicitly the dependencies this codebase uses (that's the reason why this LS can only analyse this codebase). In the near future integration with existing build systems (gradle, maven...) will be added so this LS will be usable for other projects

- Implement stubbed Analysis API services: To use the analysis API, we need to provide implementations for the services needed to define the so called [Platform Interface](https://github.com/JetBrains/kotlin/blob/master/analysis/analysis-api-platform-interface/README.md). Right now, only a subset of the platform interface is implemented (without caching mechanisms, just a quick implementation to have it working for now, so slowness is expected in this phase of the project). The rest of the services are stubbed and need development.

- Indexing solution: to provide features like autocomplete and search references, we need to create an index where we store all the references used in the project. This feature has not been started yet, the idea is to use something like a sqlite database and perform a background indexing of the whole project, and incrementally update it as the user modifies the source files. One of the goals of this LS is to provide a fast startup time, so diagnostics are reported as quick as possible.

### Implemented features
- 🚧 Real time diagnostics: mostly working for this codebase, need to finish implement the Analysis API platform to solve the remaining diagnostics false errors
- 🚧 Go to definition: working except for kotlin binary dependencies, it would also be nice to use a decompiler to jump into .class files (the analysis api provides `KotlinClassFileDecompiler` for kotlin .class files, fernflower may be used for java .class files)
- 🚧 Hover: only implemented for function calls, need to implement for the rest of use cases
- 🚧 Build system integration: there is initial basic support for gradle at this [PR](https://github.com/amgdev9/kotlin-lsp/pull/1), but needs work to be usable

## Building and running

To build the language server, just run the `./build.sh` script at the root directory, which compiles the project using gradle, packs it as a distribution zip and decompresses it in the `./lsp-dist` folder. Once built, you need to integrate it in a code editor to test its functionality. For example, in neovim the following config can be used:

```lua
local root_dir = vim.fs.root(0, {"settings.gradle.kts", "settings.gradle"})
if not root_dir then
    root_dir = vim.fs.root(0, {"build.gradle.kts", "build.gradle"})
end
local lsp_folder = "... path to lsp-dist folder ..."
vim.lsp.config['kotlinlsp'] = {
    cmd = { '' .. lsp_folder .. '/app-0.1/bin/app' },
    filetypes = { 'kotlin' },
    root_dir = root_dir
}
vim.lsp.enable('kotlinlsp')
```

We need to do an extra step to configure the modules which will be used by the LSP. As of today the language server does not have integrations with build systems like gradle, so for the time being the modules used are read from a `.kotlinlsp-modules.json` file. To set it up, run these commands:

```bash
cp .kotlinlsp-modules.template.json .kotlinlsp-modules.json
# Changes the template with your home folder so jar dependencies are picked up correctly
sed -i "s|<your-home-folder>|$HOME|g" .kotlinlsp-modules.json
```

After that, run the code editor in a kotlin file from this project and you should see diagnostics being reported. Also a `./log.txt` file will be created logging the calls to the services in the platform interface, to help troubleshoot bugs and track missing functionality. In the `Log.kt` file you can configure the verbosity of the logs.

## Running tests

To run the tests, just run the `./gradlew test` command. The tests are made around the LSP interface so we test against real user interactions, providing a good safety net in case of refactoring or updating dependencies.

## Contributions

Contributions are welcome! I try to improve this language server in my spare time but progress will be slow if I do it all by myself, so the more contributors this project has, the faster the development will be. Feel free to contact me if you want to contribute, have any doubts about how to start or if you need some more context about the Analysis API (which I'm not an expert, but I can provide my own research to help the development of the project).

Feel free to create issues and submit pull requests, I'll answer them as soon as I can.

## Resources

To help in the development of this project, these resources are extremely valuable:
- [Kotlin Analysis API](https://github.com/JetBrains/kotlin/tree/master/analysis): especially the standalone platform, which is a static read only platform implementation we can use as a baseline
- [IntelliJ IDEA Kotlin plugin](https://github.com/JetBrains/intellij-community/tree/master/plugins/kotlin): the kotlin plugin implements the Analysis API as well implementing the platform interface, so we have it as a base

## Sponsor this project

If you want to economically support this project, I accept donations via 

[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/amgdev9)
