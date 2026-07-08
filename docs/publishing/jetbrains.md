# JetBrains Publishing

Local build:
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :plugins:jetbrains:buildPlugin`

Build output:
- ZIP files are written under `plugins/jetbrains/build/distributions/`

Verification:
- `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :plugins:jetbrains:check`

Before a public release, confirm these are ready:
- Plugin icon
- Screenshots
- Description copy
- Changelog
- Signing certificate configuration
- Publish token configuration

Keep signing material and publish tokens out of git.
