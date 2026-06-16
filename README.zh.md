# FarmersDelight Addon Template

[English](README.md) | **中文**

一个精简且注释完整的起始模板，用于在 **FarmersDelight**（CraftEngine）插件之上构建你自己的扩展（addon）。复制此文件夹，重命名命名空间，然后替换示例内容即可。

## 扩展的整体结构

```
CraftEngine  ── defines custom items / blocks / furniture / models / recipes from YAML
FarmersDelight ── server-side gameplay + a stable public API (com.huidu.farmersdelight.api.**)
Your addon   ── bundles CraftEngine YAML  +  Java glue that calls the FarmersDelight api
```

三个部分，三种职责：

1. **CraftEngine YAML**（`src/main/resources/craftengine/<namespace>/`）—— 你的物品、方块、食物方块、配方、GUI、图片、翻译。`AddonResources` 会在首次加载时将它们复制到 `plugins/CraftEngine/resources/<namespace>/`（缺失则安装）。
2. **FarmersDelight API**（`FarmersDelightApi`）—— CraftEngine 单独无法完成的部分：炖锅 / 切菜板配方、游戏内配方书、Folia 安全调度、热源查询、合成经验。
3. **你的 Java 代码** —— 玩法粘合层。参见 [`FDAddonTemplate.java`](src/main/java/com/example/fdaddon/FDAddonTemplate.java)。

## ⚠️ 混淆规则（务必阅读）

FarmersDelight 以**混淆**形式发布 —— ProGuard 会重命名 / 重新打包**除** `com.huidu.farmersdelight.api.**` 之外的所有内容，而该包下的名称保持稳定。因此：

- 在你的扩展中**只**引用 `com.huidu.farmersdelight.api.**`。绝不要 import FD 的内部类（管理器、GUI、工具类）—— 这些名称在不同构建之间会变化，并且在运行时不存在。
- 用以下命令审查：`grep -rn "com.huidu.farmersdelight" src` → 每一处命中都必须是 `…api…`。
- CraftEngine 和 Paper 的类可以直接使用（它们不会被 FD 混淆）。

## 构建配置

1. 构建 FarmersDelight 的 jar（在 Farmersdelight 仓库中执行 `gradlew shadowJar`），并将 `build/libs/farmersdelight-*.jar` 复制到本项目的 **`libs/`** 目录中（这是一个仅编译期使用、被 gitignore 忽略的内置 jar —— 真正的插件会在运行时提供那些 `api.**` 类）。
2. `./gradlew shadowJar` → `build/libs/fdaddontemplate-1.0.0.jar`。
3. 将该 jar 放入 `plugins/` 目录，与 FarmersDelight + CraftEngine 并列。`plugin.yml` 中的 `depend:` 可确保加载顺序。

## `FarmersDelightApi` 参考

通过 `FarmersDelightApi.get()` 获取单例。**每次调用前都要用 `isAvailable()` 加以保护。**

| 方法 | 用途 |
|---|---|
| `isAvailable()` / `isFolia()` | FD 是否存在且已启用 / 是否运行在 Folia 上 |
| `registerRecipeType(RecipeType)` / `unregisterRecipeType(id)` | 将你的配方加入 FD 的配方书 |
| `recipeTypes()` / `recipeType(id)` | 查询已注册的类型 |
| `registerCookingPotRecipe(id, ingredients, container, result, exp, cookTime, category)` | 真实的炖锅配方；可在 `/fd reload` 后保留 |
| `registerCuttingBoardRecipe(id, input, tool, results, sound)` | 真实的切菜板配方 |
| `unregisterCookingPotRecipe(id)` / `unregisterCuttingBoardRecipe(id)` | 移除上述配方 |
| `openRecipeBook(player)` / `openRecipeBook(player, filler)` | 打开配方书（filler 会添加一个 “Fill” 按钮） |
| `openRecipeEditor(player, typeId, recipeId)` | 为可编辑类型打开编辑器 |
| `isHeatSource(block)` / `isConductor(block)` | 为你的烹饪方块进行热源查询 |
| `runAtLocation(loc, task)` / `runLaterAtLocation(loc, task, ticks)` / `runRepeating(task, delay, period)` | Folia 安全的调度（重复任务返回一个 `ApiTask`） |
| `awardCraftingExperience(player, loc, result, baseExp, source)` | 经验球 + AuraSkills + ProfessionCookingExperienceEvent |

辅助类 + 事件类（同样位于 `api.**` 之下）：

- `api.item.FarmersDelightItems` —— `idOf` / `create` / `matchesId` / `matchesTag` / `displayNameOf` / `idsOf` / `tagIdsOf`。可感知 CraftEngine；请用它替代原始的 `Material` 检查。
- `api.recipe.*` —— `RecipeType`、`ViewableRecipe`、`RecipeFiller`、`RecipeEditor`、`EditableRecipe`、`NumericField`，以及只读的 `FarmersDelightRecipes`（查询 FD 自身的炖锅 / 切菜板配方）。
- `api.scheduler.ApiTask` —— 重复任务的取消句柄。
- `api.event.*` —— `FarmersDelightReloadEvent`、`FarmersDelightProduceEvent`、`ProfessionCookingExperienceEvent`。

## 重载由 FarmersDelight 驱动

你的扩展**不需要自己的命令**。FarmersDelight 会在任何 `/fd reload <target>` 时触发 `FarmersDelightReloadEvent`；监听它并重载你的配置即可（参见示例）。你注册的配方会在重载后保留。依赖物品的配方还应在 `CraftEngineReloadEvent` 上（重新）注册（CE 物品只有在 CE 加载之后才能解析）。

## CraftEngine YAML 约定（与 FarmersDelight 保持一致）

- 像 FD 一样**按类型组织**：`items.yml`、`food_block.yml`、`blocks.yml`、`gui.yml`、`translations.yml`。
- **只使用块状（block）风格** —— 不要使用内联的 `{ }` 流式映射。`item-name` / `model` 保持不加引号；仅在 YAML 必需时才加引号（以 `#` 开头的标签，或带有尾随空格时）。
- **发布的配置中不要有注释**（本模板的注释仅用于教学）。FD 自身的配置是干净的。
- CE 原生配方类型：`shaped`、`shapeless`、`smelting`、`smoking`、`blasting`、`campfire_cooking`、`stonecutting`。炖锅 / 切菜板配方**不是** CE 配方 —— 请通过 API 注册它们。
- 自定义方块从其宿主状态（host state）获得碰撞箱：实心的 `auto-state` 宿主（`note_block`、`mushroom`）提供完整立方体碰撞；透明宿主（`non_tintable_leaves`）会产生碰撞，但能为非立方体模型干净地渲染；`tripwire` 无碰撞（适用于扁平的食物方块）。方块的右键 / 破坏逻辑可以是纯 CE 的 `events`（参见 FD 的 `food_block.yml`）。

参见 [`configuration/items.yml`](src/main/resources/craftengine/fdaddon/configuration/items.yml) 中带注释的示例（自定义食物 + 食用时效果、对原版物品行为的覆盖、一个有序合成配方）。

## 带交互 + 逐方块保存内容的自定义方块

一个*会执行某些操作*并*按位置记住状态*的自定义方块，分散在三个地方 —— 全部属于 CraftEngine 自己的 API，而非 FarmersDelight（FD 不桥接方块注册；它桥接的是配方 / 调度 / 热源 / 经验）。由于 CraftEngine 的类保持稳定的名称，直接 import `net.momirealms.craftengine.**` 是安全的（不像 FD 的内部类）。

1. **方块定义** —— [`configuration/blocks.yml`](src/main/resources/craftengine/fdaddon/configuration/blocks.yml)：`blocks.fdaddon:example_block`，带有 `behavior: - type: fdaddon:example_block`、一个使其可被放置的 `block_item`，以及一个有序合成配方。碰撞来自宿主状态（`auto-state: note_block` → 实心）。无需声明 block-entity —— CraftEngine 会从 behavior 中检测到它（见下文）。
2. **行为（Behavior）** —— [`ExampleBlockBehavior.java`](src/main/java/com/example/fdaddon/ExampleBlockBehavior.java)：`extends BlockBehavior implements EntityBlock`，重写 `useOnBlock(...)` 实现右键逻辑，并暴露一个静态的 `FACTORY`。在 `onLoad()` 中通过 `BlockBehaviors.register(Key.of("fdaddon:example_block"), FACTORY)` 注册 —— 必须在 CraftEngine 解析 `blocks.yml` *之前*完成，因为配置会通过 id 引用它。该行为是被所有已放置方块共享的**单例**，因此它本身不持有逐方块状态；在交互时它会查找被点击位置处的 block entity，并作用于该方块的控制器。
3. **逐方块内容** —— [`ExampleBlockEntityController.java`](src/main/java/com/example/fdaddon/ExampleBlockEntityController.java)：因为该行为实现了 `EntityBlock`，CraftEngine 会为每个已放置方块创建一个 `BlockEntityController` 并**将其持久化在区块内**。你在 `saveCustomData(CompoundTag)` 中序列化你的状态，并在 `loadCustomData(CompoundTag)` 中恢复它 —— 这就是持久化的全部内容。**不要将方块状态存储在额外的 yml 中**；没有由插件管理的保存 / 加载生命周期可供写入。这正是 FD 的炖锅 / 煎锅 / 灶台保存其物品栏和进度的方式。（FD *确实*有一个 `block_storage.yml`，但那是为 block entity 出现之前写入的数据所做的遗留迁移 —— 并非应当效仿的模式。）要将某个方块标记为已更改以便 CraftEngine 重新保存它，请调用 `blockEntity.world.blockEntityChanged(pos)`。对于逐 tick 的逻辑，请在控制器上重写 `createBlockEntityTicker(...)`。
