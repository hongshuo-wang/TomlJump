<p align="center">
  <img src="https://plugins.jetbrains.com/files/32933/1111804/icon/default.png" alt="TomlJump logo" width="96" height="96">
</p>

<h1 align="center">TomlJump</h1>

<p align="center">
  Navigate both ways between TOML configuration and matching source code.<br>
  在 TOML 配置与匹配的源码之间双向跳转。
</p>

TomlJump adds focused bidirectional navigation between TOML configuration, project files, and matching source configuration declarations. It uses the stable VS Code Extension API and packages as a standard VSIX for VS Code and compatible editors.

## Features

- Jump from TOML string file paths to project files.
- Navigate both ways between TOML tables and keys and matching source declarations.
- Navigate `pyproject.toml` project scripts to Python modules and callables.
- Resolve conservative configuration declarations in Go, Python, Java, TypeScript, and JavaScript.
- Use the editor's native Go to Definition action and command/control click.
- Return multiple credible targets through the editor's native definition UI.

TomlJump intentionally does not replace TOML syntax highlighting, formatting, schema validation, diagnostics, completion, or rename support. It focuses on high-confidence navigation and stays quiet when a configuration relationship is not credible.

## Compatibility

TomlJump requires VS Code Extension API 1.85 or later and uses no proposed or product-specific APIs. The standard VSIX is designed for editors that implement this API baseline.

## Installation

Build a local VSIX from the repository:

```bash
cd plugins/vscode
npm ci
npm run package
```

Install `build/tomljump-vscode-1.4.0.vsix` through the editor's Extensions view using `Install from VSIX...`.

## Usage

Use the editor's normal Go to Definition action or Ctrl/Cmd-click on supported TOML paths, table/key leaf segments, source configuration declarations, and Python entry-point segments. When multiple credible definitions exist, the editor presents its native target list.

## Privacy

TomlJump does not collect telemetry and does not send project files or navigation data over the network. Workspace files are read locally only when resolving a definition request.

---

TomlJump 为 TOML 配置、项目文件与匹配的源码配置声明提供专注的双向跳转能力。扩展使用稳定版 VS Code Extension API，并以标准 VSIX 形式支持 VS Code 及兼容编辑器。

## 功能

- 从 TOML 字符串文件路径跳转到项目文件。
- 在 TOML table、key 与匹配的源码配置声明之间双向跳转。
- 从 `pyproject.toml` 的 project scripts 跳转到 Python 模块和 callable。
- 支持 Go、Python、Java、TypeScript、JavaScript 中的保守配置匹配。
- 使用编辑器原生 Go to Definition 和 command/control click。
- 存在多个可信目标时，使用编辑器原生定义列表展示。

TomlJump 不替代 TOML 语法高亮、格式化、schema 校验、诊断、补全或重命名。它只专注于高置信度导航；当配置关系不够可信时会静默跳过。

## 兼容性

TomlJump 需要编辑器实现 VS Code Extension API 1.85 或更高版本，不使用 proposed API 或产品专用 API。标准 VSIX 可安装到实现该 API 基线的编辑器。

## 安装

从仓库构建本地 VSIX：

```bash
cd plugins/vscode
npm ci
npm run package
```

在编辑器 Extensions 视图中选择 `Install from VSIX...`，安装 `build/tomljump-vscode-1.4.0.vsix`。

## 使用

在支持的 TOML 路径、table/key 叶子段、源码配置声明和 Python entry point 片段上，使用编辑器原生 Go to Definition 或 Ctrl/Cmd-click。存在多个可信定义时，编辑器会展示原生目标列表。

## 隐私

TomlJump 不收集遥测数据，也不会通过网络发送项目文件或导航数据。仅在解析定义请求时读取本地工作区文件。

## License / 许可证

TomlJump is released under the MIT License. TomlJump 使用 MIT 许可证发布。
