./gradlew clean distZip
rm -rf lsp-dist
unzip app/build/distributions/kotlin-lsp-*.zip -d lsp-dist
