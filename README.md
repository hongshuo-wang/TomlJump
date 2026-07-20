<p align="center">
  <img src="plugins/jetbrains/src/main/resources/META-INF/pluginIcon.svg" alt="TomlJump logo" width="96" height="96">
</p>

<h1 align="center">TomlJump</h1>

<p align="center">
  Navigate both ways between TOML configuration and the source code that defines it.
</p>

<p align="center">
  <a href="README.zh-CN.md">中文文档</a>
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white">
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white">
  <img alt="IntelliJ Platform" src="https://img.shields.io/badge/IntelliJ%20Platform-000000?logo=intellijidea&logoColor=white">
  <img alt="VS Code API" src="https://img.shields.io/badge/VS%20Code%20API-007ACC?logo=visualstudiocode&logoColor=white">
  <img alt="TOML" src="https://img.shields.io/badge/TOML-9C4121">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white">
  <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-green">
  <a href="https://linux.do/t/topic/2589906"><img alt="LINUX DO" src="https://img.shields.io/badge/LINUX-DO-FFB003.svg?logo=data:image/svg%2bxml;base64,DQo8c3ZnIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgd2lkdGg9IjEwMCIgaGVpZ2h0PSIxMDAiPjxwYXRoIGQ9Ik00Ni44Mi0uMDU1aDYuMjVxMjMuOTY5IDIuMDYyIDM4IDIxLjQyNmM1LjI1OCA3LjY3NiA4LjIxNSAxNi4xNTYgOC44NzUgMjUuNDV2Ni4yNXEtMi4wNjQgMjMuOTY4LTIxLjQzIDM4LTExLjUxMiA3Ljg4NS0yNS40NDUgOC44NzRoLTYuMjVxLTIzLjk3LTIuMDY0LTM4LjAwNC0yMS40M1EuOTcxIDY3LjA1Ni0uMDU0IDUzLjE4di02LjQ3M0MxLjM2MiAzMC43ODEgOC41MDMgMTguMTQ4IDIxLjM3IDguODE3IDI5LjA0NyAzLjU2MiAzNy41MjcuNjA0IDQ2LjgyMS0uMDU2IiBzdHlsZT0ic3Ryb2tlOm5vbmU7ZmlsbC1ydWxlOmV2ZW5vZGQ7ZmlsbDojZWNlY2VjO2ZpbGwtb3BhY2l0eToxIi8+PHBhdGggZD0iTTQ3LjI2NiAyLjk1N3EyMi41My0uNjUgMzcuNzc3IDE1LjczOGE0OS43IDQ5LjcgMCAwIDEgNi44NjcgMTAuMTU3cS00MS45NjQuMjIyLTgzLjkzIDAgOS43NS0xOC42MTYgMzAuMDI0LTI0LjM4N2E2MSA2MSAwIDAgMSA5LjI2Mi0xLjUwOCIgc3R5bGU9InN0cm9rZTpub25lO2ZpbGwtcnVsZTpldmVub2RkO2ZpbGw6IzE5MTkxOTtmaWxsLW9wYWNpdHk6MSIvPjxwYXRoIGQ9Ik03Ljk4IDcwLjkyNmMyNy45NzctLjAzNSA1NS45NTQgMCA4My45My4xMTNRODMuNDI2IDg3LjQ3MyA2Ni4xMyA5NC4wODZxLTE4LjgxIDYuNTQ0LTM2LjgzMi0xLjg5OC0xNC4yMDMtNy4wOS0yMS4zMTctMjEuMjYyIiBzdHlsZT0ic3Ryb2tlOm5vbmU7ZmlsbC1ydWxlOmV2ZW5vZGQ7ZmlsbDojZjlhZjAwO2ZpbGwtb3BhY2l0eToxIi8+PC9zdmc+"></a>
</p>

TomlJump provides editor plugins for projects that use TOML as configuration. It adds focused navigation from TOML values, tables, and keys to project files and source declarations, and from supported source declarations back to TOML.

The plugin is intentionally conservative: when a configuration relationship is not credible, TomlJump stays quiet.

## Features

- Jump from TOML string file paths to project files, for example `schema = "./schemas/user.json"`.
- Jump from TOML tables and keys to matching configuration code.
- Jump from supported configuration containers and fields in source code back to matching TOML tables and keys.
- Navigate `[project.scripts]` entry-point module and callable segments to Python source declarations.
- Resolve clear configuration references in Go, Python, Java, TypeScript, and JavaScript.
- Use the editor's normal Go to Definition/Declaration action and command/control click.
- Fail quietly when a configuration relationship is not credible.

## Tech Stack

| Area | JetBrains | VS Code-compatible editors |
| --- | --- | --- |
| Language | Kotlin | TypeScript |
| Platform | IntelliJ Platform SDK | Stable VS Code Extension API |
| Build | Gradle | npm, esbuild, VSCE |
| TOML parsing | JetBrains TOML language plugin | TOML 1.0 AST parser |
| Tests | Kotlin test, JUnit 4, IntelliJ Platform test framework | Vitest, VS Code Extension Host |

## Installation

### JetBrains Marketplace

If TomlJump is available in JetBrains Marketplace, install it from your IDE:

1. Open your JetBrains IDE.
2. Go to `Settings` or `Preferences` > `Plugins` > `Marketplace`.
3. Search for `TomlJump`.
4. Install the plugin and restart the IDE if prompted.

### VS Code, Cursor, And Trae

Build the standard VSIX from source:

```bash
cd plugins/vscode
npm ci
npm run package
```

Choose `Install from VSIX...` in the editor's Extensions view and select `plugins/vscode/build/tomljump-vscode-1.4.0.vsix`.

### Compatibility

TomlJump targets JetBrains IDEs on platform build 243 (2024.3) and later. Each release is checked with JetBrains Plugin Verifier against the 2024.3 baseline for IntelliJ IDEA Community, PyCharm, GoLand, and WebStorm before publication.

The VSIX targets stable VS Code API 1.85 and later without proposed APIs or product-specific integrations. It is designed for compatible VS Code, Cursor, and Trae versions.

### Build From Source

Requirements:

- JDK 21 or newer for JetBrains builds.
- Node.js 22 for VS Code builds.
- Git.

Build and verify both plugin lines:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/all.sh
```

Build and verify only the JetBrains plugin:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/jetbrains.sh
```

Build only the JetBrains plugin ZIP:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :plugins:jetbrains:buildPlugin
```

The ZIP artifact is written to:

```text
plugins/jetbrains/build/distributions/
```

Build and verify only the VS Code-compatible extension:

```bash
./scripts/verify/vscode.sh
```

The VSIX artifact is written to:

```text
plugins/vscode/build/
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
- `plugins/vscode`: TypeScript extension for VS Code-compatible editors.
- `fixtures`: cross-language samples used by tests.

Useful commands:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :core:tomljump-core:test
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :plugins:jetbrains:test
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :plugins:jetbrains:buildPlugin
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/jetbrains.sh
./scripts/verify/vscode.sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/all.sh
```

## Community

You are welcome to discuss TomlJump usage feedback, questions, and ideas in the [TomlJump topic on Linux DO](https://linux.do/t/topic/2589906).

## License

TomlJump is released under the MIT License. See [LICENSE](LICENSE).
