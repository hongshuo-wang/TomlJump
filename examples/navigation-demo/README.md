# TomlJump 双向导航示例

使用已安装本地 TomlJump 插件的 JetBrains IDE 打开本目录。所有检查都使用 Go to Declaration 或 Ctrl/Cmd-click。

## 安装本地开发包

在仓库根目录运行：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
GRADLE_USER_HOME=/private/tmp/tomljump-gradle-home \
./gradlew :plugins:jetbrains:buildPlugin
```

1. 打开 JetBrains IDE 的 `Settings/Preferences > Plugins`。
2. 打开齿轮菜单，选择 `Install Plugin from Disk...`。
3. 选择 `plugins/jetbrains/build/distributions/jetbrains-1.3.0.zip`。
4. 重启 IDE，然后将 `examples/navigation-demo` 作为项目打开。

## 四种交互情况

1. **唯一目标直接跳转**：打开 `go/config.go`，点击 `GoServiceConfig` 或 `GoToken`，应直接进入 `app.toml`。
2. **多个目标显示原生选择器**：打开 `go/multiple_targets.go`，点击 `MultiTargetConfig` 或 `SharedToken`，选择器应同时显示 `multiple-primary.toml` 和 `multiple-secondary.toml`。
3. **低置信度保持安静**：打开 `go/no_match.go`，点击 `NoMatchConfig` 或 `OrphanValue`，TomlJump 不应跳转。
4. **保留语言原生导航**：打开 `java/NativeNavigation.java`，点击 `render()` 中的 `formatValue()` 调用，应进入同文件的方法声明，不显示 TomlJump 弹窗。

相关源码和 TOML 文件中也标注了 `CASE 1` 到 `CASE 4`。

## 嵌套路径与 TOML 常见语法

打开 `nested-syntax.toml`，逐项使用 Go to Declaration 或 Ctrl/Cmd-click：

5. **Root dotted key**：点击 `app.cache.host` 的 `host`，应进入 `go/nested_syntax.go` 的 `CacheConfig.Host`。点击中间段 `app` 或 `cache` 不应由 TomlJump 跳转。
6. **嵌套 table**：点击 `[app.database]` 的 `database`，应进入 `DatabaseConfig`；点击 `host` 应进入 `DatabaseConfig.Host`。点击非叶段 `app` 不应跳转。
7. **Quoted table/key**：点击 `[app."quoted-service"]` 的 `quoted-service`，应进入 `QuotedServiceConfig`；点击 `"base.url"` 应进入 `QuotedServiceConfig.BaseURL`。
8. **Array of tables**：点击 `[[products]]` 的 `products`，应进入 `ProductsConfig`；点击 `name` 应进入 `ProductsConfig.Name`。
9. **保守拒绝**：点击 inline table `point = { x = 1, y = 2 }` 中的 `x` 或 `y`，TomlJump 不应创建跳转。

再打开 `go/nested_syntax.go` 检查反向导航：

- `CacheConfig.Host` 应直接进入 root dotted key 的 `host`。
- `DatabaseConfig` 和它的 `Host` 应分别进入嵌套 table 的 `database` 与 `host`。
- `QuotedServiceConfig` 和 `BaseURL` 应分别进入 quoted table 与 quoted key。
- `ProductsConfig` 和 `Name` 应分别进入 array table 与 `name` key。

这些声明名称彼此独立，正常情况下都应直接跳转，不显示多目标选择器。

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

## Install The Local Development Build

Run this command from the repository root:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
GRADLE_USER_HOME=/private/tmp/tomljump-gradle-home \
./gradlew :plugins:jetbrains:buildPlugin
```

1. Open `Settings/Preferences > Plugins` in a JetBrains IDE.
2. Open the gear menu and select `Install Plugin from Disk...`.
3. Select `plugins/jetbrains/build/distributions/jetbrains-1.3.0.zip`.
4. Restart the IDE and open `examples/navigation-demo` as the project.

## Four Interaction Cases

1. **Unique target, direct jump**: open `go/config.go` and click `GoServiceConfig` or `GoToken`; navigation should open `app.toml` directly.
2. **Multiple targets, native chooser**: open `go/multiple_targets.go` and click `MultiTargetConfig` or `SharedToken`; the chooser should list both `multiple-primary.toml` and `multiple-secondary.toml`.
3. **Low confidence, stay quiet**: open `go/no_match.go` and click `NoMatchConfig` or `OrphanValue`; TomlJump should not navigate.
4. **Native language navigation remains available**: open `java/NativeNavigation.java` and click the `formatValue()` call in `render()`; navigation should reach the method declaration without a TomlJump popup.

The relevant source and TOML files also contain `CASE 1` through `CASE 4` comments.

## Nested Paths And Common TOML Syntax

Open `nested-syntax.toml` and use Go to Declaration or Ctrl/Cmd-click:

5. **Root dotted key**: `host` in `app.cache.host` should navigate to `CacheConfig.Host`. The intermediate `app` and `cache` segments should stay unresolved by TomlJump.
6. **Nested table**: `database` in `[app.database]` should navigate to `DatabaseConfig`, while `host` should navigate to `DatabaseConfig.Host`. The non-leaf `app` segment should stay unresolved.
7. **Quoted table/key**: `quoted-service` should navigate to `QuotedServiceConfig`, and `"base.url"` should navigate to `QuotedServiceConfig.BaseURL`.
8. **Array of tables**: `products` should navigate to `ProductsConfig`, and `name` should navigate to `ProductsConfig.Name`.
9. **Conservative rejection**: `x` and `y` inside `point = { x = 1, y = 2 }` should not receive TomlJump navigation.

Then open `go/nested_syntax.go` and check reverse navigation:

- `CacheConfig.Host` should open the root dotted `host` key.
- `DatabaseConfig` and its `Host` should open the nested table leaf and field key.
- `QuotedServiceConfig` and `BaseURL` should open the quoted table and quoted key.
- `ProductsConfig` and `Name` should open the array table and its `name` key.

The declarations use distinct container names, so each supported case should navigate directly without a multiple-target chooser.

Field matching is always scoped to the owning configuration container. Explicit Go tags and Java annotations use exact key aliases; declarations without a credible owner relationship stay unresolved.
