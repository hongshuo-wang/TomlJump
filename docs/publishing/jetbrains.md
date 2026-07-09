# JetBrains Publishing

Local build:
- `./scripts/verify/jetbrains.sh`

Build output:
- ZIP files are written under `plugins/jetbrains/build/distributions/`

Verification:
- `./scripts/verify/jetbrains.sh`
- Raw Gradle commands require Java 17 or newer, for example `JAVA_HOME=/path/to/jdk17 ./gradlew :plugins:jetbrains:buildPlugin`.

Before a public release, confirm these are ready:
- Plugin icon
- Screenshots
- Description copy
- Changelog
- Signing certificate configuration
- Publish token configuration

Keep signing material and publish tokens out of git.
