# TomlJump Maintainer Notes

TomlJump is a monorepo with a JetBrains-first delivery path.

Project shape:
- `core/tomljump-core`: editor-agnostic Kotlin logic
- `plugins/jetbrains`: IntelliJ Platform integration only
- `plugins/vscode`: reserved for a later VS Code extension
- `fixtures`: cross-editor samples used to keep classification behavior grounded
- `docs/publishing`: release and packaging notes

Commands:
- Full JetBrains verification: `./scripts/verify/jetbrains.sh`
- Core tests: `./gradlew :core:tomljump-core:test`
- JetBrains tests: `./gradlew :plugins:jetbrains:test`
- Build plugin: `./gradlew :plugins:jetbrains:buildPlugin`

Notes:
- The verification script requires Java 17 or newer and works around stale `JAVA_HOME` values when a usable Java is available on `PATH` or in common macOS locations.
- JetBrains `BasePlatformTestCase` tests require JUnit 3/4 discovery, so the JetBrains module intentionally does not use JUnit Platform for its `test` task.
- The current JetBrains MVP publishes file-path references only. Symbol-shaped strings are classified in core but not exposed as PSI references until language-specific resolvers exist.

Rules:
- Do not implement TOML formatting, syntax highlighting, or schema validation here.
- Prefer high-confidence navigation over broad guesses.
- Update fixtures when changing classification behavior.
- Keep language-specific resolution optional.
- Do not remove the reserved `plugins/vscode` folder.
