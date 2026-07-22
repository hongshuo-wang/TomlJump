<p align="center">
  <img src="plugins/jetbrains/src/main/resources/META-INF/pluginIcon.svg" alt="TomlJump logo" width="96" height="96">
</p>

<h1 align="center">TomlJump</h1>

<p align="center">
  在 TOML 配置、项目文件与定义它们的源码之间快速跳转。
</p>

<p align="center">
  <a href="README.md">English README</a>
</p>

<p align="center">
  <a href="https://plugins.jetbrains.com/plugin/32933-tomljump"><img alt="JetBrains Marketplace" src="https://img.shields.io/jetbrains/plugin/v/32933-tomljump?label=JetBrains"></a>
  <a href="https://marketplace.visualstudio.com/items?itemName=harrisonwang.tomljump"><img alt="Visual Studio Marketplace" src="https://img.shields.io/visual-studio-marketplace/v/harrisonwang.tomljump?label=VS%20Marketplace"></a>
  <a href="https://open-vsx.org/extension/harrisonwang/tomljump"><img alt="Open VSX" src="https://img.shields.io/open-vsx/v/harrisonwang/tomljump?label=Open%20VSX"></a>
  <a href="https://github.com/hongshuo-wang/TomlJump/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/hongshuo-wang/TomlJump?label=Release"></a>
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-green"></a>
</p>

TomlJump 是面向 TOML 配置项目的开源导航扩展。它在 TOML table、key 与匹配的源码声明之间建立专注的双向链接，也支持从 TOML value 直接跳转到项目文件和 Python entry point。

TomlJump 优先保证可信度，而不是盲目扩大匹配范围。当关系存在语法错误、歧义或不受支持时，它会保持安静，不改变编辑器原有的导航行为。

## 安装

| 编辑器 | 推荐安装方式 |
| --- | --- |
| IntelliJ IDEA、PyCharm、GoLand、WebStorm 与兼容的 JetBrains IDE | 从 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32933-tomljump) 安装 TomlJump。 |
| Visual Studio Code | 从 [Visual Studio Marketplace](https://marketplace.visualstudio.com/items?itemName=harrisonwang.tomljump) 安装 TomlJump。 |
| 其他 VS Code 兼容编辑器 | 从 [Open VSX](https://open-vsx.org/extension/harrisonwang/tomljump) 安装，或使用 GitHub Releases 中的 VSIX。 |

需要离线或手动安装时，可从[最新 GitHub Release](https://github.com/hongshuo-wang/TomlJump/releases/latest)下载已签名的 JetBrains ZIP 或标准 VSIX。

## 支持哪些跳转

在受支持的 TOML 和源码声明上，使用编辑器原生的 **Go to Definition/Declaration** 操作或 Ctrl/Cmd-click。

```toml
schema = "./schemas/user.json"

[server]
port = 8080

[project.scripts]
tomljump = "tomljump.cli:main"
```

- **项目文件：** `./schemas/user.json` 会打开相对于当前 TOML 文件的目标文件。
- **配置代码：** `server` 和 `port` 会跳转到匹配的源码容器和字段；受支持的源码声明也可以反向跳回对应的 TOML table 或 key。
- **Python entry point：** 在 `pyproject.toml` 中，`tomljump.cli` 会解析到 Python module，`main` 则独立解析到顶层 `def` 或 `async def` callable。

存在多个可信目标时，TomlJump 会使用编辑器原生的目标选择器，而不是自行猜测。

## 支持的语言与 TOML 形式

配置匹配支持：

- Go
- Python
- Java
- TypeScript
- JavaScript

TomlJump 支持标准 table、array of tables、嵌套 table path、quoted key 与 dotted key。Owner-aware matching 会把同名字段限定在正确的配置容器中。

可复用的[导航演示项目](examples/navigation-demo)包含跨语言正向案例，以及验证保守行为的负面案例。

## TomlJump 不做什么

TomlJump 不提供 TOML 语法高亮、格式化、schema 校验、诊断、补全、Find Usages 或重命名。请搭配你常用的 TOML 语言工具使用。

Inline table 中的 key、语法错误的路径、dotted key 的非叶子段、局部或嵌套声明，以及其他低可信关系都不会产生推测性跳转。

## 兼容性

- **JetBrains：** 支持平台构建号 243（2024.3）及之后版本。每个版本都会针对 IntelliJ IDEA Community、PyCharm、GoLand 和 WebStorm 的 2024.3 基线进行兼容性检查。
- **VS Code 及兼容编辑器：** 需要实现 VS Code Extension API 1.85 或更高版本。TomlJump 不使用 proposed API 或产品专用 API，标准 VSIX 可用于实现该 API 基线的编辑器。

## 隐私

TomlJump 不包含遥测，也不会通过网络发送项目文件或导航数据。仅在编辑器请求解析定义时，于本地完成匹配。

## 参与开发

TomlJump 是一个 monorepo，JetBrains 与 VS Code 兼容扩展拥有彼此独立的交付路径。

| 路径 | 用途 |
| --- | --- |
| `core/tomljump-core` | 与编辑器无关的 Kotlin 分类与匹配逻辑 |
| `plugins/jetbrains` | IntelliJ Platform 集成 |
| `plugins/vscode` | 面向 VS Code 兼容编辑器的 TypeScript 扩展 |
| `fixtures` | 测试使用的跨语言样例 |

环境要求：

- Gradle 和 JetBrains 验证使用 JDK 21
- VS Code 验证使用 Node.js 22 与 npm 10.9.8
- Git

标准验证命令：

```bash
# 同时验证两条产品线（以下 JAVA_HOME 写法适用于 macOS）
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/all.sh

# 仅验证 JetBrains
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/verify/jetbrains.sh

# 仅验证 VS Code 兼容扩展
./scripts/verify/vscode.sh
```

在其他操作系统上运行 Gradle 或 JetBrains 验证命令前，请将 `JAVA_HOME` 指向 JDK 21。

## 社区讨论

欢迎在 [Linux DO 的 TomlJump 主题](https://linux.do/t/topic/2589906)或 [GitHub Issues](https://github.com/hongshuo-wang/TomlJump/issues)中交流使用反馈、问题和想法。

## 许可证

TomlJump 使用 [MIT License](LICENSE) 发布。
