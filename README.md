<p align="center">
  <img src="plugins/jetbrains/src/main/resources/META-INF/pluginIcon.svg" alt="TomlJump logo" width="96" height="96">
</p>

<h1 align="center">TomlJump</h1>

<p align="center">
  Jump from TOML configuration to the project files and source code it points at.
</p>

<p align="center">
  <a href="README.zh-CN.md">中文文档</a>
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white">
  <img alt="IntelliJ Platform" src="https://img.shields.io/badge/IntelliJ%20Platform-000000?logo=intellijidea&logoColor=white">
  <img alt="TOML" src="https://img.shields.io/badge/TOML-9C4121">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white">
  <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-green">
</p>

TomlJump is a JetBrains IDE plugin for projects that use TOML as configuration. It adds focused navigation for TOML values, tables, and keys without trying to become a TOML formatter, schema engine, or language server.

The plugin is intentionally conservative: when a TOML value or key does not look like a credible reference, TomlJump stays quiet.

## Features

- Jump from TOML string file paths to project files, for example `schema = "./schemas/user.json"`.
- Jump from TOML tables and keys to matching configuration code.
- Resolve clear configuration references in Go, Python, Java, TypeScript, and JavaScript.
- Use normal JetBrains navigation actions such as Go to Declaration and command/control click.
- Fail quietly when a value or key is not a credible reference.

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| IDE platform | IntelliJ Platform SDK |
| Build | Gradle |
| TOML support | JetBrains TOML language plugin |
| Tests | Kotlin test, JUnit 4, IntelliJ Platform test framework |

## Installation

### From JetBrains Marketplace

If TomlJump is available in JetBrains Marketplace, install it from your IDE:

1. Open your JetBrains IDE.
2. Go to `Settings` or `Preferences` > `Plugins` > `Marketplace`.
3. Search for `TomlJump`.
4. Install the plugin and restart the IDE if prompted.

### Build From Source

Requirements:

- JDK 21 or newer.
- Git.

Build and verify the plugin:

```bash
./scripts/verify/jetbrains.sh
```

Build only the JetBrains plugin ZIP:

```bash
./gradlew :plugins:jetbrains:buildPlugin
```

The ZIP artifact is written to:

```text
plugins/jetbrains/build/distributions/
```

Install the ZIP locally:

1. Open your JetBrains IDE.
2. Go to `Settings` or `Preferences` > `Plugins`.
3. Open the gear menu.
4. Choose `Install Plugin from Disk...`.
5. Select the generated ZIP file.

## Development

Project layout:

- `core/tomljump-core`: editor-agnostic Kotlin classification and matching logic.
- `plugins/jetbrains`: IntelliJ Platform integration.
- `plugins/vscode`: reserved for a later VS Code extension.
- `fixtures`: cross-language samples used by tests.

Useful commands:

```bash
./gradlew :core:tomljump-core:test
./gradlew :plugins:jetbrains:test
./gradlew :plugins:jetbrains:buildPlugin
./scripts/verify/jetbrains.sh
```

## License

TomlJump is released under the MIT License. See [LICENSE](LICENSE).
