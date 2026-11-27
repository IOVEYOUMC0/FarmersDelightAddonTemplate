# FDAddonTemplate 1.2.0 验证记录（2026-11-28）

状态：当前（模板 1.2.0，2026-11-28）。证据边界：构建、单元/契约测试、模板自检与 CE 26.11 编译通过；
未在实机启动模板插件。

## 1.2.0：容器方块示例

在原先“计数器方块”之外新增一个完整容器示例，专门演示本次 FD 修复过的“先确认掉落、再清空存储”契约：

| 组成 | 职责 |
|---|---|
| `ExampleContainerBehavior` | `fdaddon:example_container` 的交互：手持物品右键存入一个，空手右键取回一个（交给玩家走 `ItemDelivery.giveOrDrop`） |
| `ExampleContainerController` | 两个物品槽，`saveCustomData`/`loadCustomData` 通过 `ItemStackUtils` 持久化；`dropContents()` 返回“是否全部确认交付”；`onRemove()` 在无法取消移除时写恢复归档 |
| `ExampleContainerBreakListener` | 监听 CraftEngine 的 `CustomBlockBreakEvent`，`dropContents()` 返回 false 时**取消破坏**，方块与未交付物品一起保留 |
| `util/StoredContents` | 与 CE 解耦的槽位逻辑：存入/取回、`dropAll` 逐槽确认后才清空、`remaining()` 供归档 |
| `util/DropSafety` | 校验生成实体 `isValid()` 的掉落助手 |
| `util/DropRecoveryArchive` | 压缩 NBT 恢复文件（世界/坐标/DataVersion/物品），路径 `plugins/FDAddonTemplate/container-drop-recovery/` |
| 资源 | `blocks.yml` 新增方块定义、block item 与合成配方（复用计数器方块的模型，不带额外美术）；中英翻译键 |

测试：`StoredContentsTest`（5 项：存入/取回/拒绝生成保留/重试/remaining）、`DropRecoveryArchiveTest`（压缩 NBT 往返）、
`ExampleContainerWiringTest`（5 项：破坏取消顺序、onRemove 归档、CE 物品编解码、交付路径、方块定义与注册）。

## 1.1.0：构建与启动行

| 项目 | 内容 |
|---|---|
| 本地 CE jar 编译 | 新增 `-PceJar` / `-PceLibraries`，可直接用 `craft-engine-paper-plugin-26.10/26.11-SNAPSHOT.jar` 编译（快照未发布到 Maven）；`-PceVersion` 仍用于 Maven 发行版 |
| 掉落安全说明 | README 增加三条路径：破坏取消、无法取消时写恢复文件、玩家交付用 `ItemDelivery.giveOrDrop` |
| 启动行收敛 | 合并原来的 `api_version` + `enabled` 两行 INFO 为一行：`FDAddonTemplate {version} enabled \| FarmersDelight api v{api_version} (station-query=...)` |
| 版本 | 1.0.2 → 1.1.0 → 1.2.0，产物 `fdaddontemplate-1.2.0.jar` |

## CE 兼容性

- 编译基线仍是 Paper 1.21.5 / CE 26.9.1（Maven）。
- 用 CE **26.11-SNAPSHOT** 插件 jar 编译通过：
  `gradlew compileJava -PceJar=<26.11 jar> -PceLibraries=<CE libs>` → BUILD SUCCESSFUL。
  （在 FD 工作区内构建时该属性也会传给 FD 复合构建，所以 `-PceLibraries` 必填；独立副本无此限制。）
- 26.11 的运行期兼容结论见 FarmersDelight 仓库的 `verification-records/ce-2026-11-28/README.md`
  （工作区级记录，不随本模板分发）：FD 不改代码即可编译、启动并通过 29/29 探针，因此基于同版 FD api
  的附属无需为 26.11 改代码。

## 验证结果

| 检查 | 结果 |
|---|---|
| `gradlew build` | BUILD SUCCESSFUL |
| `check_test_results.py FDAddonTemplate` | 8 suites / **22 tests**，0 失败、0 跳过 |
| `gradlew addonCheck` | **5/5 PASS**：api-boundary、deprecated-api、paper-dependency、feature-gate、comment-tags |
| CE 26.11 jar 编译 | BUILD SUCCESSFUL |
| 产物 | `build/libs/fdaddontemplate-1.2.0.jar` |

## 边界

- 这是模板工程，未部署到测试服（示例插件只用于编译/契约验证）。
- 未在实机启动模板插件；容器示例的交互/破坏/爆炸路径由单元测试与源码契约测试覆盖，真正的高压验证在各附属仓库进行。
- 26.10/26.11 之外的快照若移除模板用到的 CE 类，需按 README 的版本门控方式处理。
