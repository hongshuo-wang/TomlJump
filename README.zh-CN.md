<p align="center">
  <img src="plugins/jetbrains/src/main/resources/META-INF/pluginIcon.svg" alt="TomlJump logo" width="96" height="96">
</p>

<h1 align="center">TomlJump</h1>

<p align="center">
  从 TOML 配置跳到它指向的项目文件和源码位置。
</p>

<p align="center">
  <a href="README.md">English README</a>
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white">
  <img alt="IntelliJ Platform" src="https://img.shields.io/badge/IntelliJ%20Platform-000000?logo=intellijidea&logoColor=white">
  <img alt="TOML" src="https://img.shields.io/badge/TOML-9C4121">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white">
  <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-green">
</p>

TomlJump 是一个 JetBrains IDE 插件，面向使用 TOML 作为配置的项目。它只负责更可靠的跳转：从 TOML 的值、table、key 跳到对应的文件或源码位置，不接管格式化、schema 校验、补全或语法高亮。

它的策略偏保守：当 TOML 值或 key 不像一个明确引用时，TomlJump 会保持安静，不打扰普通编辑。

## 功能

- 从 TOML 字符串路径跳转到项目文件，例如 `schema = "./schemas/user.json"`。
- 从 TOML table 和 key 跳转到匹配的配置代码。
- 支持 Go、Python、Java、TypeScript、JavaScript 中较明确的配置引用匹配。
- 使用 JetBrains 原生跳转能力，例如 Go to Declaration 和 command/control click。
- 无法判断为可靠引用时静默跳过，不打扰普通 TOML 编辑。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 开发语言 | Kotlin |
| IDE 平台 | IntelliJ Platform SDK |
| 构建工具 | Gradle |
| TOML 支持 | JetBrains TOML language plugin |
| 测试 | Kotlin test、JUnit 4、IntelliJ Platform test framework |

## 安装

### 从 JetBrains Marketplace 安装

如果 TomlJump 已上架 JetBrains Marketplace，可以在 IDE 中安装：

1. 打开 JetBrains IDE。
2. 进入 `Settings` 或 `Preferences` > `Plugins` > `Marketplace`。
3. 搜索 `TomlJump`。
4. 安装插件，并按提示重启 IDE。

### 从源码打包安装

要求：

- JDK 21 或更高版本。
- Git。

完整验证并打包：

```bash
./scripts/verify/jetbrains.sh
```

只打包 JetBrains 插件 ZIP：

```bash
./gradlew :plugins:jetbrains:buildPlugin
```

ZIP 产物路径：

```text
plugins/jetbrains/build/distributions/
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
- `plugins/vscode`：为未来 VS Code 扩展预留。
- `fixtures`：测试使用的跨语言示例。

常用命令：

```bash
./gradlew :core:tomljump-core:test
./gradlew :plugins:jetbrains:test
./gradlew :plugins:jetbrains:buildPlugin
./scripts/verify/jetbrains.sh
```

## 许可证

TomlJump 使用 MIT 许可证发布，详见 [LICENSE](LICENSE)。
