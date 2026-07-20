TomlJump VS Code 兼容扩展的首个公开版本，提供与 JetBrains 插件一致的保守导航能力。

- 支持 TOML table 与 key 到 Go、Python、Java、TypeScript、JavaScript 配置声明的跳转。
- 支持上述源码配置容器与字段反向跳转到 TOML。
- 支持 TOML 相对文件路径跳转。
- 支持 nested table、quoted key、dotted key 和 array of tables，并对低置信度关系保持静默。
- 支持 `pyproject.toml` `[project.scripts]` 的 Python 模块与顶层函数跳转。
- 使用稳定 VS Code Extension API，可通过同一 VSIX 安装到兼容版本的 VS Code、Cursor 与 Trae。
