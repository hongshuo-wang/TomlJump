## 本次更新

- 新增从 `pyproject.toml` `[project.scripts]` 值跳转到 Python 模块和顶层 callable。
- 模块片段与 callable 片段可分别跳转，支持普通与 `async` 顶层函数以及 package `__init__.py` 模块。
- 继续保持保守匹配：缺失模块、缺失 callable、嵌套函数和非标准 TOML 上下文不会产生猜测式导航。
- 扩充自动化测试与 `examples/navigation-demo`，覆盖正向跳转和不应跳转的负面案例。

## 兼容性

- 支持 JetBrains Platform 2024.3 及以上版本。
- 发布前验证覆盖 IntelliJ IDEA Community、PyCharm、GoLand 和 WebStorm 2024.3。
