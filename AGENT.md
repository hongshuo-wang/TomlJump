# TomlJump Maintainer Notes

TomlJump is a monorepo with a JetBrains-first delivery path.

Project shape:
- `core/tomljump-core`: editor-agnostic Kotlin logic
- `plugins/jetbrains`: IntelliJ Platform integration only
- `plugins/vscode`: reserved for a later VS Code extension
- `fixtures`: cross-editor samples used to keep classification behavior grounded
- `docs/publishing`: release and packaging notes

Commands:
- Run all checks: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew check`
- Core tests: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :core:tomljump-core:test`
- JetBrains tests: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :plugins:jetbrains:test`
- Build plugin: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :plugins:jetbrains:buildPlugin`

Notes:
- The local shell JVM may default to Java 8, so use the Java 17-prefixed Gradle commands above when building or testing.
- JetBrains `BasePlatformTestCase` tests require JUnit 3/4 discovery, so the JetBrains module intentionally does not use JUnit Platform for its `test` task.

Rules:
- Do not implement TOML formatting, syntax highlighting, or schema validation here.
- Prefer high-confidence navigation over broad guesses.
- Update fixtures when changing classification behavior.
- Keep language-specific resolution optional.
- Do not remove the reserved `plugins/vscode` folder.
