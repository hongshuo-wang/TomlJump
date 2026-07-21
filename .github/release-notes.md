## JetBrains 插件

- 新增从 `pyproject.toml` `[project.scripts]` 值跳转到 Python 模块和顶层 callable。
- 模块片段与 callable 片段可分别跳转，支持普通与 `async` 顶层函数以及 package `__init__.py` 模块。
- 继续保持保守匹配：缺失模块、缺失 callable、嵌套函数和非标准 TOML 上下文不会产生猜测式导航。
- 扩充自动化测试与 `examples/navigation-demo`，覆盖正向跳转和不应跳转的负面案例。

- 支持 JetBrains Platform 2024.3 及以上版本，并完成 IntelliJ IDEA Community、PyCharm、GoLand 和 WebStorm 2024.3 发布前验证。

## VS Code 兼容扩展

- 首次提供与 JetBrains 插件共有能力对齐的 VS Code 兼容扩展。
- 支持 TOML table、key 与 Go、Python、Java、TypeScript、JavaScript 配置声明之间的双向跳转。
- 支持 TOML 相对文件路径、nested table、quoted key、dotted key 和 array of tables。
- 支持从 `pyproject.toml` `[project.scripts]` 的模块和 callable 片段跳转到 Python 源码。
- 使用稳定 VS Code Extension API，同一个 VSIX 可安装到兼容版本的 VS Code、Cursor 与 Trae。
