# TomlJump 双向导航示例

使用已安装本地 TomlJump 插件的 JetBrains IDE 打开本目录。所有检查都使用 Go to Declaration 或 Ctrl/Cmd-click。

## 四种交互情况

1. **唯一目标直接跳转**：打开 `go/config.go`，点击 `GoServiceConfig` 或 `GoToken`，应直接进入 `app.toml`。
2. **多个目标显示原生选择器**：打开 `go/multiple_targets.go`，点击 `MultiTargetConfig` 或 `SharedToken`，选择器应同时显示 `multiple-primary.toml` 和 `multiple-secondary.toml`。
3. **低置信度保持安静**：打开 `go/no_match.go`，点击 `NoMatchConfig` 或 `OrphanValue`，TomlJump 不应跳转。
4. **保留语言原生导航**：打开 `java/NativeNavigation.java`，点击 `render()` 中的 `formatValue()` 调用，应进入同文件的方法声明，不显示 TomlJump 弹窗。

相关源码和 TOML 文件中也标注了 `CASE 1` 到 `CASE 4`。

## TOML 到源码

打开 `app.toml` 并检查：

- table 名跳到对应语言的配置容器。
- `*_token` key 跳到所属配置容器的字段。
- 每个 `schema` key 跳到匹配 table 的源码字段。
- 每个 `schema` 字符串值跳到 `schemas/user.json`。

## 源码到 TOML

| 源文件 | 容器到 table | 字段到 key |
| --- | --- | --- |
| `go/config.go` | `GoServiceConfig` -> `go_service` | `GoToken` -> `go_token`，`Schema` -> `schema` |
| `python/config.py` | `PythonServiceConfig` -> `python_service` | `python_token` -> `python_token`，`schema` -> `schema` |
| `java/JavaServiceConfig.java` | `JavaServiceConfig` -> `java_service` | `javaToken` -> `java_token`，`schema` -> `schema` |
| `typescript/config.ts` | `TypeScriptServiceConfig` -> `typescript_service` | `typescriptToken` -> `typescript_token`，`schema` -> `schema` |
| `javascript/config.js` | `javascriptServiceConfig` -> `javascript_service` | `javascript_token` -> `javascript_token`，`schema` -> `schema` |

字段匹配始终受所属配置容器约束。Go tag 和 Java annotation 的显式别名使用精确 key 匹配；无法证明容器归属时保持静默。

---

# TomlJump Bidirectional Navigation Demo

Open this directory in a JetBrains IDE with the locally built TomlJump plugin installed. Use Go to Declaration or Ctrl/Cmd-click for every check.

## Four Interaction Cases

1. **Unique target, direct jump**: open `go/config.go` and click `GoServiceConfig` or `GoToken`; navigation should open `app.toml` directly.
2. **Multiple targets, native chooser**: open `go/multiple_targets.go` and click `MultiTargetConfig` or `SharedToken`; the chooser should list both `multiple-primary.toml` and `multiple-secondary.toml`.
3. **Low confidence, stay quiet**: open `go/no_match.go` and click `NoMatchConfig` or `OrphanValue`; TomlJump should not navigate.
4. **Native language navigation remains available**: open `java/NativeNavigation.java` and click the `formatValue()` call in `render()`; navigation should reach the method declaration without a TomlJump popup.

The relevant source and TOML files also contain `CASE 1` through `CASE 4` comments.

Field matching is always scoped to the owning configuration container. Explicit Go tags and Java annotations use exact key aliases; declarations without a credible owner relationship stay unresolved.
