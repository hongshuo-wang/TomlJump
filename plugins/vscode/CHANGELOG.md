# Changelog / 更改日志

## 1.4.0 - 2026-07-20

- 首次提供与 JetBrains 插件共有能力对齐的 VS Code 兼容扩展。
- 支持从 TOML 字符串文件路径跳转到相对项目文件。
- 支持 TOML table、key 与 Go、Python、Java、TypeScript、JavaScript 配置声明之间的双向跳转。
- 支持 nested table、quoted key、dotted key 和 array of tables，并继续静默处理 inline table、畸形路径和不可信匹配。
- 支持从 `pyproject.toml` `[project.scripts]` 的模块和 callable 片段跳转到 Python 源码声明。
- 使用稳定版 VS Code Extension API，同一个 VSIX 可安装到实现 API 1.85 基线的 VS Code 及兼容编辑器。

---

- Introduced the VS Code-compatible extension with shared capabilities aligned with the JetBrains plugin.
- Added navigation from TOML string file paths to relative project files.
- Added bidirectional navigation between TOML tables and keys and Go, Python, Java, TypeScript, and JavaScript configuration declarations.
- Added nested tables, quoted keys, dotted keys, and arrays of tables while keeping inline tables, malformed paths, and low-confidence matches quiet.
- Added navigation from `pyproject.toml` `[project.scripts]` module and callable segments to Python source declarations.
- Used the stable VS Code Extension API so the same VSIX can run in VS Code and compatible editors that implement the API 1.85 baseline.
