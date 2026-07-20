## 本次更新

- 新增嵌套 TOML 路径双向导航，可在嵌套 table、key、root dotted key 与最近的明确源码配置容器之间跳转。
- TOML 路径提取改用 JetBrains TOML PSI，支持 quoted key、dotted key、普通 table 和 array of tables。
- 继续保持保守匹配：dotted path 只有叶子段参与跳转；inline table 成员、畸形路径和容器不匹配不会产生猜测式导航。
- 扩充 `examples/navigation-demo`，加入新增语法的正向、反向和负面手测案例。

## 兼容性

- 支持 JetBrains Platform 2024.3 及以上版本。
- 发布前验证覆盖 IntelliJ IDEA Community、PyCharm、GoLand 和 WebStorm 2024.3。
