<p align="center">
  <img src="plugins/jetbrains/src/main/resources/META-INF/pluginIcon.svg" alt="TomlJump logo" width="96" height="96">
</p>

<h1 align="center">TomlJump</h1>

<p align="center">
  在 TOML 配置与定义它的源码之间双向跳转。
</p>

<p align="center">
  <a href="README.md">English README</a>
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

TomlJump 为使用 TOML 作为配置的项目提供编辑器插件。它提供从 TOML 的值、table、key 到项目文件和源码声明的跳转，也支持从明确的源码配置声明反向跳回 TOML。

它的策略偏保守：当配置关系不够明确时，TomlJump 会保持安静，不打扰普通编辑。

## 功能

- 从 TOML 字符串路径跳转到项目文件，例如 `schema = "./schemas/user.json"`。
- 从 TOML table 和 key 跳转到匹配的配置代码。
- 从源码中的配置容器和字段反向跳转到匹配的 TOML table 和 key。
- 从 `[project.scripts]` entry point 的模块和 callable 片段跳转到 Python 源码声明。
- 支持 Go、Python、Java、TypeScript、JavaScript 中较明确的配置引用匹配。
- 使用编辑器原生 Go to Definition/Declaration 和 command/control click。
- 无法判断为可靠配置关系时静默跳过，不打扰普通编辑。

## 技术栈

| 模块 | JetBrains | VS Code 兼容编辑器 |
| --- | --- | --- |
| 开发语言 | Kotlin | TypeScript |
| 平台 | IntelliJ Platform SDK | 稳定版 VS Code Extension API |
| 构建工具 | Gradle | npm、esbuild、VSCE |
| TOML 解析 | JetBrains TOML language plugin | TOML 1.0 AST parser |
| 测试 | Kotlin test、JUnit 4、IntelliJ Platform test framework | Vitest、VS Code Extension Host |

## 安装

### JetBrains Marketplace

如果 TomlJump 已上架 JetBrains Marketplace，可以在 IDE 中安装：

1. 打开 JetBrains IDE。
2. 进入 `Settings` 或 `Preferences` > `Plugins` > `Marketplace`。
3. 搜索 `TomlJump`。
4. 安装插件，并按提示重启 IDE。

### VS Code、Cursor 与 Trae

从源码构建标准 VSIX：

```bash
cd plugins/vscode
npm ci
npm run package
```

在编辑器的 Extensions 视图中选择 `Install from VSIX...`，安装 `plugins/vscode/build/tomljump-vscode-1.4.0.vsix`。

### 兼容性

TomlJump 面向平台构建号 243（2024.3）及之后的 JetBrains IDE。每次发布前都会使用 JetBrains Plugin Verifier，分别针对 IntelliJ IDEA Community、PyCharm、GoLand 和 WebStorm 的 2024.3 基线进行兼容性检查。

VSIX 面向稳定版 VS Code API 1.85 及之后版本，不使用 proposed API 或产品专用集成，可安装到兼容版本的 VS Code、Cursor 与 Trae。

### 从源码打包安装

要求：

- JetBrains 构建需要 JDK 21 或更高版本。
- VS Code 构建需要 Node.js 22。
- Git。

完整验证并打包两条插件线：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/all.sh
```

仅验证并打包 JetBrains 插件：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/jetbrains.sh
```

只打包 JetBrains 插件 ZIP：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :plugins:jetbrains:buildPlugin
```

ZIP 产物路径：

```text
plugins/jetbrains/build/distributions/
```

仅验证并打包 VS Code 兼容扩展：

```bash
./scripts/verify/vscode.sh
```

VSIX 产物路径：

```text
plugins/vscode/build/
```

本地安装 ZIP：

1. 打开 JetBrains IDE。
2. 进入 `Settings` 或 `Preferences` > `Plugins`。
3. 打开齿轮菜单。
4. 选择 `Install Plugin from Disk...`。
5. 选择生成的 ZIP 文件。

## 开发

项目结构：

- `core/tomljump-core`：与编辑器无关的 Kotlin 分类和匹配逻辑。
- `plugins/jetbrains`：IntelliJ Platform 集成。
- `plugins/vscode`：面向 VS Code 兼容编辑器的 TypeScript 扩展。
- `fixtures`：测试使用的跨语言示例。

常用命令：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :core:tomljump-core:test
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :plugins:jetbrains:test
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :plugins:jetbrains:buildPlugin
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/jetbrains.sh
./scripts/verify/vscode.sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/all.sh
```

## 社区讨论

欢迎在 [Linux DO 的 TomlJump 主题](https://linux.do/t/topic/2589906)中交流使用反馈、问题和想法。

## 许可证

TomlJump 使用 MIT 许可证发布，详见 [LICENSE](LICENSE)。
