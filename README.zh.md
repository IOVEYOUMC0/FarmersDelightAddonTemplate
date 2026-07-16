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

1. 构建 FarmersDelight 的 **api-only 桩 jar**（在 FarmersDelight 仓库中执行 `gradlew apiJar`），并将 `build/libs/farmersdelight-plugin-*-api.jar` 复制到本项目的 **`libs/`** 目录、重命名为 `farmersdelight-api-1.0.0.jar`（仅编译期、被 gitignore 忽略）。它**只含** `com.huidu.farmersdelight.api.**` —— 不含 FD 内部类、不是可运行插件；真正的 FarmersDelight 插件会在运行时提供实现。
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
| `openRecipeBook(player)` / `openRecipeBook(player, filler)` | 打开 FD 的共享配方书（filler 会添加一个 “Fill” 按钮） |
| `openRecipeBook(player, typeId, filler)` | 只为你的类型打开**独立**配方书（用它自己的布局；不与其他附属挤在同一菜单） |
| `openRecipeEditor(player, typeId, recipeId)` | 为可编辑类型打开编辑器 |
| `isHeatSource(block)` / `isConductor(block)` | 为你的烹饪方块进行热源查询 |
| `runAtLocation(loc, task)` / `runLaterAtLocation(loc, task, ticks)` / `runRepeating(task, delay, period)` | Folia 安全的调度（重复任务返回一个 `ApiTask`） |
| `awardCraftingExperience(player, loc, result, baseExp, source)` | 经验球 + AuraSkills + ProfessionCookingExperienceEvent |

辅助类 + 事件类（同样位于 `api.**` 之下）：

- `api.item.FarmersDelightItems` —— `idOf` / `create` / `matchesId` / `matchesTag` / `displayNameOf` / `idsOf` / `tagIdsOf` / `isCustomItem` / `customIdOf` / `craftingRemainderOf` / `applyDisplay` / `buildIcon`。可感知 CraftEngine；请用它替代原始的 `Material` 检查（其中的显示辅助方法见“更多 API”一节）。
- `api.recipe.*` —— `RecipeType`、`ViewableRecipe`、`RecipeFiller`、`RecipeEditor`、`EditableRecipe`、`NumericField`、`JumpTarget`、`IngredientMatching`、`RecipeBookLayout`（给你的类型一套独立配方书布局，见 `ExampleRecipeType`），以及只读的 `FarmersDelightRecipes`（查询 FD 自身的炖锅 / 切菜板配方）。
  - **独立配方书**：重写 `RecipeType.listLayout()` / `detailLayout()` 返回 `RecipeBookLayout`（标题 + 格子 + 装饰物品，建议从你自己的 gui.yml 读取；标题可含 CraftEngine `<image:>`/`<shift:>` 实现自定义贴图背景）。`ViewableRecipe.displaySlots()` 把自定义角色（如 `fluid`、`tool`）映射到额外的详情槽位，`jumpTargets()` 让这样的槽位可点击跳转。用 `openRecipeBook(player, typeId, filler)` 打开。布局返回 null 则回退到 FD 共享配方书。
  - **可编辑类型**：重写 `RecipeType.editor()` 返回 `RecipeEditor`（物品槽标签 + `NumericField`），管理员即可通过 FD 的编辑器 GUI 编辑配方——见 `recipe/ExampleRecipeEditor`。`RecipeFiller`（见 `recipe/ExampleRecipeFiller`，用 `IngredientMatching` 匹配）提供“填充”按钮。
- `api.buff.*` —— `CustomBuff` + `CustomBuffRegistry` + `BuffBossbar`：一个持久化的扩展 buff，由 FD 的牛奶清除监听器与进/退服持久化生命周期驱动，并在 FD 的频道上显示每玩家的 boss 血条。见 `buff/ExampleCustomBuff` + `buff/ExampleBuffBossbar`。
- **发包物品显示** —— `FarmersDelightApi.createItemDisplay` / `updateItemDisplay` / `removeItemDisplay`：向附近玩家展示的悬浮物品，不生成真实实体。保存返回的 int 句柄，区块加载时重建，并在 `FarmersDelightCollectLiveDisplaysEvent` 上上报它们，使 `/fd cleanup` 不会清除。见 `display/ExampleItemDisplayManager`。
- `api.scheduler.ApiTask` —— 重复任务的取消句柄。
- `api.event.*` —— `FarmersDelightReloadEvent`、`FarmersDelightProduceEvent`、`ProfessionCookingExperienceEvent`，以及管理 / 生命周期桥接 `FarmersDelightCleanupEvent`、`FarmersDelightMigrateEvent`、`FarmersDelightWarmupEvent`、`FarmersDelightCollectLiveDisplaysEvent`——见 `listener/ExampleFarmersDelightEventsListener`（collect-live-displays 在显示管理器里处理）。
- `api.util.*` —— `PluginManagerGuard`（阻止 PlugMan 之类在运行时重载 / 卸载你的插件；在 onEnable 注册）与 `TooltipUtils.hideDurabilityLine`（当物品的耐久值被用作某种计量条时隐藏耐久行——见 `util/ExampleTooltipCustomizer`）。

## 更多 API（文本、物品、食物效果、成就、配方查询、配方解锁）

均位于 `com.huidu.farmersdelight.api.**`。调用前用 `FarmersDelightApi.get().isAvailable()` 保护（成就另有 `FarmersDelightAdvancements.isAvailable()`）。[`FDAddonTemplate.java`](src/main/java/com/example/fdaddon/FDAddonTemplate.java) 内有可运行示例：`foodEffects`、`advancements`、`recipeQueries`、`textAndMessages`、`recipeDiscovery`。

### 富文本与消息 —— `api.text.FarmersDelightText` / `FarmersDelightMessages`

`render` 按观察者把模板渲染成 Component：`{key}` 占位符 → `<l10n:key>` / `<lang:key>` 翻译标签（按观察者语言）→ CraftEngine `<image:ns:id>` / `<shift:N>` 字形 → MiniMessage + 传统 `&` 颜色。

```java
Component c = FarmersDelightText.render("<green>Hi {name}!", player, Map.of("name", player.getName()));
List<Component> lore = FarmersDelightText.buildLore(List.of("<gray>Line"), player, Map.of()); // 去除斜体
Component glyph = FarmersDelightText.glyph("ns:icon_id");        // 单个 CraftEngine 图片字形
FarmersDelightMessages.send(player, "<gold>Saved");
FarmersDelightMessages.actionBar(player, "<gray>{n} left", Map.of("n", "3"));
FarmersDelightMessages.title(player, "<aqua>Title", "<gray>Subtitle", 10, 40, 10, Map.of());
```

线程：消息和食物效果会操作玩家 —— 必须在玩家所属线程调用（Paper 主线程、Folia 玩家所在区域线程），不要在异步任务里调用。

### 物品显示与辅助 —— `api.item.FarmersDelightItems`

`isCustomItem` / `customIdOf` / `craftingRemainderOf`，以及共享 FD 渲染器（l10n 标签 + 字形 + 颜色，去斜体）的物品显示构建器：

```java
FarmersDelightItems.applyDisplay(item, "<gold>Name", List.of("<gray>Lore"), player, Map.of());
ItemStack icon = FarmersDelightItems.buildIcon("ns:id", "<gold>Name", List.of("<gray>Lore"), player, Map.of());
```

### 食物效果 —— `api.effect.FarmersDelightFoodEffects`

FarmersDelight 的舒适（未饱食时缓慢回血）与滋养（抑制消耗度）：

```java
FarmersDelightFoodEffects.applyComfort(player, 600);             // 秒
FarmersDelightFoodEffects.registerComfortFood("ns:food", 600);  // 吃 ns:food 即获得；可在 /fd reload 后保留
boolean has = FarmersDelightFoodEffects.hasNourishment(player);
```

如果要映射多于几个食物，**别**在 Java 里硬编码 id 列表，把映射放到自己的 `config.yml` 里——管理员可调，
你的 reload 监听器自然能拾起，模式对任意附属都通用。参考
`BrewinAndChewin/src/main/java/com/huidu/brewinandchewin/util/FoodEffectRegistrar.java`（约 50 行可直接抄）：
插件持一个实例，在 onEnable + reload 调 `apply(getConfig().getConfigurationSection("food-effects"))`，
onDisable 调 `clear()`。配置形状：

```yaml
food-effects:
  nourishment:
    "myaddon:hearty_stew": 300   # 秒
  comfort:
    "myaddon:warm_tea": 60
```

### 成就 —— `api.advancement.FarmersDelightAdvancements`（+ `AdvancementTree`）

需要 **UltimateAdvancementAPI** 插件。可授予 / 查询 FarmersDelight 自身的成就页，或用纯数据注册你自己的成就页 —— FD 会构建真正的 UAA 页并在 `/fd reload` 后重建：

```java
if (FarmersDelightAdvancements.isAvailable()) {
    FarmersDelightAdvancements.tree("myaddon")
        .root("root", icon, "Title", "Desc", "minecraft:textures/block/stone.png")
        .advancement("step1", "root", icon, "Step 1", "Do a thing.", "task", 1, 0)
        .register();                                            // 禁用时调用 unregister("myaddon")
}
FarmersDelightAdvancements.award("myaddon", player, "step1");
FarmersDelightAdvancements.award(player, "master_chef");        // FD 自身的成就页（不带 tabId）
```

标题 / 描述是客户端语言键（从资源包解析）或字面文本。

### 配方查询 —— `api.recipe.FarmersDelightRecipes` → `RecipeInfo`

只读地查看 FD 自身的炖锅 / 切菜板配方（只含 Bukkit 类型）：

```java
for (String id : FarmersDelightRecipes.cookingPotRecipeIds()) {
    RecipeInfo r = FarmersDelightRecipes.cookingPotRecipe(id);  // 原料(id 字符串)、产物、cookTimeTicks、experience、category、container
}
// 还有：cuttingBoardRecipeIds()、cuttingBoardRecipe(id)、matchesCookingPot(...)、cookingPotResult(...)
```

### 配方解锁 —— `api.recipe.FarmersDelightRecipeDiscovery`（默认关闭）

配方书里按玩家的配方锁定 / 解锁（在 FD 配置里开启 `recipe-discovery`）：

```java
if (FarmersDelightRecipeDiscovery.isEnabled()) {
    FarmersDelightRecipeDiscovery.unlock(player, "myaddon:type", "recipe_id");
    boolean known = FarmersDelightRecipeDiscovery.isUnlocked(player,
        FarmersDelightRecipeDiscovery.TYPE_COOKING_POT, "farmersdelight:beef_stew");
}
```

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
3. **逐方块内容** —— [`ExampleBlockEntityController.java`](src/main/java/com/example/fdaddon/ExampleBlockEntityController.java)：因为该行为实现了 `EntityBlock`，CraftEngine 会为每个已放置方块创建一个 `BlockEntityController` 并**将其持久化在区块内**。你在 `saveCustomData(CompoundTag)` 中序列化你的状态，并在 `loadCustomData(CompoundTag)` 中恢复它 —— 这就是持久化的全部内容。**不要将方块状态存储在额外的 yml 中**；没有由插件管理的保存 / 加载生命周期可供写入。这正是 FD 的炖锅 / 煎锅 / 灶台保存其物品栏和进度的方式。要将某个方块标记为已更改以便 CraftEngine 重新保存它，请调用 `blockEntity.world.blockEntityChanged(pos)`。对于逐 tick 的逻辑，请在控制器上重写 `createBlockEntityTicker(...)`。

## 调试工具 —— 接入 `/fd debugtools` —— `api.util.DebugToolExtension`

把你的方块挂进 FarmersDelight 的调试命令（批量放置 / 批量激活 / 状态 / 撤销），无需自带 CLI。
参见 [`debug/ExampleDebugExtension.java`](src/main/java/com/example/fdaddon/debug/ExampleDebugExtension.java)
中的接线方式；在 `onEnable` 中一次 `DebugToolRegistry.register(...)`，管理员就能用：

```
/fd debugtools place <name> [count] [spacing] [layers]   # 批量放置你的方块
/fd debugtools activate <name>                            # 填充 / tick / 激活你跟踪的方块
/fd debugtools activate all                               # 同时触发所有已注册的扩展
/fd debugtools status                                     # 在 FD 自身快照之后追加你的 status() 行
/fd debugtools undo                                       # 撤销上一批（也会撤你的方块）
```

实现四个方法（接口在 `com.huidu.farmersdelight.api.util`）：

- `name()` —— 关键字（如 `"example_block"`），同时也是 tab 补全项。
- `place(player, origin, count, spacing, layers, undo)` —— 批量放置你的方块。**在每次写入前先调
  `undo.capture(loc)`**，这样 `/fd debugtools undo` 才能回滚。一般按 `ceil(sqrt(count))` × `layers` 排成网格。
- `activate(player)`（可选）—— 唤醒已放置的方块（填料、开始发酵、累加计数器等）。返回实际被激活的数量。
- `status(player)`（可选）—— 短行，附加在 FD TickManager 快照之后。
- `cleanupBeforeUndo(loc)`（可选）—— 当该位置即将被撤销时，释放内存追踪 + 移除你方块的 block-entity NBT。
  位置不属于你时要廉价地直接 return。

即使 FD 是非 debug build（不带 `-PdebugTools=true`），注册也是安全的 —— 注册表此时处于 dormant
状态，你的方法不会被调用。有中央 manager 的插件（如 FD 的 `StoveManager` 或 BAC 的 `KegManager`）
应该在 `activate` / `cleanupBeforeUndo` 里直接遍历 manager，而不是另外维护一份追踪；模板里因为示例方块
没有 manager，所以才在 extension 内部存了一个 set。
