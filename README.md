<p align="center">
  <img src="plugins/jetbrains/src/main/resources/META-INF/pluginIcon.svg" alt="TomlJump logo" width="96" height="96">
</p>

<h1 align="center">TomlJump</h1>

<p align="center">
  Navigate between TOML configuration, project files, and the source code that defines them.
</p>

<p align="center">
  <a href="README.zh-CN.md">中文文档</a>
</p>

<p align="center">
  <a href="https://plugins.jetbrains.com/plugin/32933-tomljump"><img alt="JetBrains Marketplace" src="https://img.shields.io/jetbrains/plugin/v/32933-tomljump?label=JetBrains"></a>
  <a href="https://marketplace.visualstudio.com/items?itemName=harrisonwang.tomljump"><img alt="Visual Studio Marketplace" src="https://img.shields.io/visual-studio-marketplace/v/harrisonwang.tomljump?label=VS%20Marketplace"></a>
  <a href="https://open-vsx.org/extension/harrisonwang/tomljump"><img alt="Open VSX" src="https://img.shields.io/open-vsx/v/harrisonwang/tomljump?label=Open%20VSX"></a>
  <a href="https://github.com/hongshuo-wang/TomlJump/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/hongshuo-wang/TomlJump?label=Release"></a>
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-green"></a>
</p>

TomlJump is an open-source navigation extension for projects that use TOML as configuration. It adds focused, bidirectional links between TOML tables and keys and matching source declarations, plus direct links from TOML values to project files and Python entry points.

TomlJump favors confidence over coverage. When a relationship is malformed, ambiguous, or unsupported, it stays quiet and leaves the editor's existing navigation unchanged.

## Install

| Editor | Recommended installation |
| --- | --- |
| IntelliJ IDEA, PyCharm, GoLand, WebStorm, and compatible JetBrains IDEs | Install [TomlJump from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32933-tomljump). |
| Visual Studio Code | Install [TomlJump from Visual Studio Marketplace](https://marketplace.visualstudio.com/items?itemName=harrisonwang.tomljump). |
| Cursor, Trae, and other compatible editors | Install from [Open VSX](https://open-vsx.org/extension/harrisonwang/tomljump), or use the VSIX from GitHub Releases. |

For offline or manual installation, download the signed JetBrains ZIP or standard VSIX from the [latest GitHub Release](https://github.com/hongshuo-wang/TomlJump/releases/latest).

## What You Can Navigate

Use the editor's normal **Go to Definition/Declaration** action or Ctrl/Cmd-click on supported TOML and source declarations.

```toml
schema = "./schemas/user.json"

[server]
port = 8080

[project.scripts]
tomljump = "tomljump.cli:main"
```

- **Project files:** `./schemas/user.json` opens the matching file relative to the TOML file.
- **Configuration code:** `server` and `port` navigate to a matching source container and field. Supported source declarations navigate back to the matching TOML table or key.
- **Python entry points:** in `pyproject.toml`, `tomljump.cli` resolves to the Python module and `main` resolves separately to a top-level `def` or `async def` callable.

When more than one target is credible, TomlJump uses the editor's native target chooser instead of guessing.

## Supported Languages And TOML Forms

Configuration matching is available for:

- Go
- Python
- Java
- TypeScript
- JavaScript

TomlJump understands standard tables, arrays of tables, nested table paths, quoted keys, and dotted keys. Owner-aware matching keeps fields with the same name attached to the correct configuration container.

The reusable [navigation demo](examples/navigation-demo) contains positive and conservative negative cases across the supported languages.

## What TomlJump Does Not Do

TomlJump does not provide TOML syntax highlighting, formatting, schema validation, diagnostics, completion, Find Usages, or rename support. Use it alongside your preferred TOML language tooling.

Inline-table keys, malformed paths, non-leaf dotted-key segments, local or nested declarations, and other low-confidence relationships do not produce speculative navigation.

## Compatibility

- **JetBrains:** platform build 243 (2024.3) or later. Releases are checked against the 2024.3 baselines of IntelliJ IDEA Community, PyCharm, GoLand, and WebStorm.
- **VS Code-compatible editors:** stable VS Code API 1.85 or later. TomlJump uses no proposed or product-specific APIs, so the same VSIX is designed for compatible VS Code, Cursor, and Trae versions.

## Privacy

TomlJump contains no telemetry and does not send project files or navigation data over the network. Resolution happens locally when the editor requests a definition.

## Contributing

TomlJump is a monorepo with independent JetBrains and VS Code-compatible delivery paths.

| Path | Purpose |
| --- | --- |
| `core/tomljump-core` | Editor-agnostic Kotlin classification and matching logic |
| `plugins/jetbrains` | IntelliJ Platform integration |
| `plugins/vscode` | TypeScript extension for VS Code-compatible editors |
| `fixtures` | Cross-language samples used by tests |

Requirements:

- JDK 21 for Gradle and JetBrains verification
- Node.js 22 and npm 10.9.8 for VS Code verification
- Git

Canonical verification commands:

```bash
# Both product lines (the JAVA_HOME form below is for macOS)
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/all.sh

# JetBrains only
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/jetbrains.sh

# VS Code-compatible extension only
./scripts/verify/vscode.sh
```

On other operating systems, set `JAVA_HOME` to a JDK 21 installation before running the Gradle or JetBrains verification commands.

## Community

Questions, usage feedback, and ideas are welcome in the [TomlJump topic on Linux DO](https://linux.do/t/topic/2589906) and in [GitHub Issues](https://github.com/hongshuo-wang/TomlJump/issues).

## License

TomlJump is released under the [MIT License](LICENSE).
