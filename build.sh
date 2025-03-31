./gradlew clean distZip
rm -rf lsp-dist
unzip app/build/distributions/app.zip -d lsp-dist
