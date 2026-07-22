# FarmersDelight Addon Template

**English** | [中文](README.zh.md)

A minimal, fully-commented starting point for building your own addon on top of the **FarmersDelight**
(CraftEngine) plugin. Copy this folder, rename the namespace, and replace the example content.

## How an addon fits together

```
CraftEngine  ── defines custom items / blocks / furniture / models / recipes from YAML
FarmersDelight ── server-side gameplay + a stable public API (com.huidu.farmersdelight.api.**)
Your addon   ── bundles CraftEngine YAML  +  Java glue that calls the FarmersDelight api
```

Three pieces, three responsibilities:

1. **CraftEngine YAML** (`src/main/resources/craftengine/<namespace>/`) — your items, blocks, food
   blocks, recipes, GUIs, images, translations. `AddonResources` copies these into
   `plugins/CraftEngine/resources/<namespace>/` on first load (install-if-missing).
2. **FarmersDelight API** (`FarmersDelightApi`) — the things CraftEngine can't do alone: cooking-pot /
   cutting-board recipes, the in-game recipe book, Folia-safe scheduling, heat-source queries, crafting XP.
3. **Your Java** — gameplay glue. See [`FDAddonTemplate.java`](src/main/java/com/example/fdaddon/FDAddonTemplate.java).

## ⚠️ Obfuscation rule (read this)

FarmersDelight ships **obfuscated** — ProGuard renames/repackages everything **except**
`com.huidu.farmersdelight.api.**`, whose names are kept stable. So:

- Reference **only** `com.huidu.farmersdelight.api.**` from your addon. Never import FD internals
  (managers, GUIs, util) — those names change between builds and won't exist at runtime.
- Audit with: `grep -rn "com.huidu.farmersdelight" src` → every hit must be `…api…`.
- CraftEngine and Paper classes are fine to use directly (they aren't obfuscated by FD).

## Build setup

1. Build FarmersDelight's **api-only** stub (in the FarmersDelight repo: `gradlew apiJar`) and copy
   `build/libs/farmersdelight-plugin-*-api.jar` into this project's **`libs/`** as
   `farmersdelight-api-1.0.0.jar` (compile-only, gitignored). It contains ONLY
   `com.huidu.farmersdelight.api.**` — no FD internals, not a runnable plugin — and the real FarmersDelight
   plugin provides the implementation at runtime.
2. `./gradlew shadowJar` → `build/libs/fdaddontemplate-1.0.0.jar`.
3. Drop the jar in `plugins/` next to FarmersDelight + CraftEngine. `plugin.yml` `depend:` ensures load order.

## `FarmersDelightApi` reference

Get the singleton with `FarmersDelightApi.get()`. **Guard every call with `isAvailable()`.**

| Method | Purpose |
|---|---|
| `isAvailable()` / `isFolia()` | FD present+enabled / running on Folia |
| `registerRecipeType(RecipeType)` / `unregisterRecipeType(id)` | add your recipes to FD's recipe book |
| `recipeTypes()` / `recipeType(id)` | query registered types |
| `registerCookingPotRecipe(id, ingredients, container, result, exp, cookTime, category)` | real cooking-pot recipe; survives `/fd reload` |
| `registerCuttingBoardRecipe(id, input, tool, results, sound)` | real cutting-board recipe |
| `unregisterCookingPotRecipe(id)` / `unregisterCuttingBoardRecipe(id)` | remove the above |
| `openRecipeBook(player)` / `openRecipeBook(player, filler)` | open FD's shared recipe book (filler adds a "Fill" button) |
| `openRecipeBook(player, typeId, filler)` | open an **independent** book for just your type (uses its own layout; never a shared menu) |
| `openRecipeEditor(player, typeId, recipeId)` | open the editor for an editable type |
| `isHeatSource(block)` / `isConductor(block)` | heat-source queries for your cooking blocks |
| `runAtLocation(loc, task)` / `runLaterAtLocation(loc, task, ticks)` / `runRepeating(task, delay, period)` | Folia-safe scheduling (repeating returns an `ApiTask`) |
| `awardCraftingExperience(player, loc, result, baseExp, source)` | XP orbs + AuraSkills + ProfessionCookingExperienceEvent |

Helper + event classes (also under `api.**`):

- `api.item.FarmersDelightItems` — `idOf` / `create` / `matchesId` / `matchesTag` / `displayNameOf` /
  `idsOf` / `tagIdsOf` / `isCustomItem` / `customIdOf` / `craftingRemainderOf` / `applyDisplay` / `buildIcon`.
  CraftEngine-aware; use instead of raw `Material` checks (the display helpers are covered under "More API surface").
- `api.recipe.*` — `RecipeType`, `ViewableRecipe`, `RecipeFiller`, `RecipeEditor`, `EditableRecipe`,
  `NumericField`, `JumpTarget`, `IngredientMatching`, `RecipeBookLayout` (give your type its own book
  layout — see `ExampleRecipeType`), and read-only `FarmersDelightRecipes` (query FD's own
  cooking-pot/cutting-board recipes).
  - **Independent recipe book**: override `RecipeType.listLayout()` / `detailLayout()` to return a
    `RecipeBookLayout` (title + grid + decoration items, built from your own gui.yml; titles may carry
    CraftEngine `<image:>`/`<shift:>` for custom-texture backgrounds). `ViewableRecipe.displaySlots()`
    maps custom roles (e.g. `fluid`, `tool`) to extra detail slots, and `jumpTargets()` makes such a slot
    clickable. Open with `openRecipeBook(player, typeId, filler)`. Return null from the layouts to fall
    back to FD's shared book.
  - **Editable type**: override `RecipeType.editor()` to return a `RecipeEditor` (item-slot labels +
    `NumericField`s) so admins edit recipes through FD's editor GUI — see `recipe/ExampleRecipeEditor`. A
    `RecipeFiller` (see `recipe/ExampleRecipeFiller`, matching with `IngredientMatching`) adds the "Fill" button.
- `api.buff.*` — `CustomBuff` + `CustomBuffRegistry` + `BuffBossbar`: a persistent addon buff that FD's
  milk-cure listener and its join/quit persistence lifecycle drive, with a per-player boss-bar readout on
  FD's channel. See `buff/ExampleCustomBuff` + `buff/ExampleBuffBossbar`.
- **Packet item displays** — `FarmersDelightApi.createItemDisplay` / `updateItemDisplay` / `removeItemDisplay`:
  floating items shown to nearby players with no real entity. Keep the returned int handles, recreate on
  chunk load, and report them on `FarmersDelightCollectLiveDisplaysEvent` so `/fd cleanup` keeps them. See
  `display/ExampleItemDisplayManager`.
- `api.scheduler.ApiTask` — cancel handle for repeating tasks.
- `api.event.*` — `FarmersDelightReloadEvent`, `FarmersDelightProduceEvent`, `ProfessionCookingExperienceEvent`,
  plus the admin / lifecycle bridges `FarmersDelightCleanupEvent`, `FarmersDelightMigrateEvent`,
  `FarmersDelightWarmupEvent` and `FarmersDelightCollectLiveDisplaysEvent` — see
  `listener/ExampleFarmersDelightEventsListener` (and the display manager for collect-live-displays).
- `api.util.*` — `PluginManagerGuard` (blocks a live PlugMan reload/unload of your plugin; register it in
  onEnable) and `TooltipUtils.hideDurabilityLine` (hide the durability line when an item's damage encodes a
  meter — see `util/ExampleTooltipCustomizer`).
- `api.config.*` — `ConfigFileUpdater` + `ConfigUpdatePolicy` + `ConfigUpdateReport` + `ConfigKeyRename`:
  keep the `config.yml` an operator already has in step with the one your build ships (covered under
  "More API surface").

## More API surface (text, items, effects, config updates, advancements, recipe queries, discovery)

All under `com.huidu.farmersdelight.api.**`. Guard with `FarmersDelightApi.get().isAvailable()` (advancements also have their own `FarmersDelightAdvancements.isAvailable()`). [`FDAddonTemplate.java`](src/main/java/com/example/fdaddon/FDAddonTemplate.java) has runnable examples: `foodEffects`, `advancements`, `recipeQueries`, `textAndMessages`, `recipeDiscovery`.

### Rich text & messages — `api.text.FarmersDelightText` / `FarmersDelightMessages`

`render` turns a template into a Component per viewer: `{key}` placeholders → `<l10n:key>` / `<lang:key>` translation tags (viewer locale) → CraftEngine `<image:ns:id>` / `<shift:N>` glyphs → MiniMessage + legacy `&` colors.

```java
Component c = FarmersDelightText.render("<green>Hi {name}!", player, Map.of("name", player.getName()));
List<Component> lore = FarmersDelightText.buildLore(List.of("<gray>Line"), player, Map.of()); // italic stripped
Component glyph = FarmersDelightText.glyph("ns:icon_id");        // a single CraftEngine image glyph
FarmersDelightMessages.send(player, "<gold>Saved");
FarmersDelightMessages.actionBar(player, "<gray>{n} left", Map.of("n", "3"));
FarmersDelightMessages.title(player, "<aqua>Title", "<gray>Subtitle", 10, 40, 10, Map.of());
```

Threading: messages and food effects touch the player — call them on the player's owning thread (main on Paper, the player's region thread on Folia), never from an async task.

### Item display & helpers — `api.item.FarmersDelightItems`

`isCustomItem` / `customIdOf` / `craftingRemainderOf`, plus item-display builders that share FD's renderer (l10n tags + glyphs + colors, italic stripped):

```java
FarmersDelightItems.applyDisplay(item, "<gold>Name", List.of("<gray>Lore"), player, Map.of());
ItemStack icon = FarmersDelightItems.buildIcon("ns:id", "<gold>Name", List.of("<gray>Lore"), player, Map.of());
```

### Food effects — `api.effect.FarmersDelightFoodEffects`

FarmersDelight's Comfort (slow regen while unsaturated) and Nourishment (suppress exhaustion):

```java
FarmersDelightFoodEffects.applyComfort(player, 600);             // seconds
FarmersDelightFoodEffects.registerComfortFood("ns:food", 600);  // eating ns:food grants it; survives /fd reload
boolean has = FarmersDelightFoodEffects.hasNourishment(player);
```

For more than a handful of foods, drive the mapping from your own `config.yml` instead of hard-coding ids
in Java — admins can tune it, your reload listener picks it up, and the pattern stays the same for any
addon. The template ships this helper:
[`util/ExampleFoodEffectRegistrar`](src/main/java/com/example/fdaddon/util/ExampleFoodEffectRegistrar.java)
(mirrors BrewinAndChewin's production `FoodEffectRegistrar`) — hold one per plugin, call
`apply(getConfig().getConfigurationSection("food-effects"))` on enable + reload, `clear()` on disable. It
parses each effect key as a **list** of `id`/`duration` entries (durations in **seconds**), matching the
shipped `config.yml`:

```yaml
food-effects:
  comfort:
    - id: "myaddon:warm_tea"
      duration: 60      # seconds
  nourishment:
    - id: "myaddon:hearty_stew"
      duration: 300     # seconds
```

### Keeping your `config.yml` up to date — `api.config.ConfigFileUpdater`

Bukkit's `saveDefaultConfig()` writes the bundled file **only when none exists**. It never adds a key to a
file that is already there, so every setting you add in a later version stays absent on a server that has
been running since before that version, and the feature behind it silently reads its hardcoded fallback
forever. Moving or dropping a setting is worse: the operator's tuned value is left sitting at a path
nothing reads.

`ConfigFileUpdater` fixes that. It renames paths that moved, deletes paths nothing reads any more, and
fills in settings a later version introduced — leaving every value the operator set exactly as it was. The
machinery is generic; *what* to rename, retire and protect is your data, supplied as a `ConfigUpdatePolicy`.

**Use `updateMainConfig(plugin, policy)`.** It performs the whole ordered sequence and writes the file back,
and the order is load-bearing:

1. **Renames first.** A rename is skipped when the new path is already set. Merging first would plant the
   bundled default at the new path, make every rename a no-op, and silently drop the value the operator had
   tuned under the old name.
2. **Retired keys next** — after a rename has carried a value to its current path, and before the merge, so
   a setting nothing reads is not written back in.
3. **The additive merge last.** Existing values are never overwritten; only genuinely absent keys are added,
   together with the comment that documents them.

Nothing is written when nothing changed, and a timestamped copy of the file is taken before the rewrite
(the same pass that adds settings also deletes values the operator wrote).

**Registry sections.** A registry section is one whose children are *content keyed by id* rather than fixed
settings — a food list, a drop table, a per-item override map. Deleting an entry there is the operator's way
of disabling that item, so the merge must never add entries back one at a time or it silently undoes their
decision. Declare them with `registrySection(...)` and the merge will only create such a section when the
operator's file has none of it at all, never touching individual entries afterwards. Everything not declared
is treated as a plain setting and merged key by key.

**The api never logs.** It returns a `ConfigUpdateReport` and you say what happened, in your own wording and
your own language files. A backup that could not be written is reported on `report.backupError()` rather than
thrown — the update still goes ahead, and it is up to you whether the operator hears that they lost the
safety net.

Modelled on BrewinAndChewin's production `BrewinConfigBootstrap`:

```java
import com.huidu.farmersdelight.api.config.ConfigFileUpdater;
import com.huidu.farmersdelight.api.config.ConfigKeyRename;
import com.huidu.farmersdelight.api.config.ConfigUpdatePolicy;
import com.huidu.farmersdelight.api.config.ConfigUpdateReport;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyAddonConfigBootstrap {

    private static final ConfigUpdatePolicy POLICY = ConfigUpdatePolicy.builder()
            // Old path -> new path, applied in the order added, so two entries can chain.
            .migrate("tipsy.max-points", "tipsy.max-duration-seconds")
            // Deleted from the operator's file because no code reads it any more.
            .retire("legacy.fade-warning-ticks")
            // Keyed by item id: a deleted entry means the operator turned that item off.
            .registrySection("food-effects.comfort", "food-effects.nourishment")
            .build();

    private final JavaPlugin plugin;

    public MyAddonConfigBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Call from onEnable, after saveDefaultConfig() and before reading any config value. */
    public void updateConfig() {
        ConfigUpdateReport report;
        try {
            report = ConfigFileUpdater.updateMainConfig(plugin, POLICY);
        } catch (Exception e) {
            plugin.getLogger().warning("config.yml could not be updated: " + e.getMessage());
            return;
        }
        if (report.backupError() != null) {
            plugin.getLogger().warning("config.yml was not backed up: " + report.backupError());
        }
        for (ConfigKeyRename rename : report.migratedKeys()) {
            plugin.getLogger().info("Moved " + rename.oldPath() + " to " + rename.newPath() + ".");
        }
        if (!report.retiredKeys().isEmpty()) {
            plugin.getLogger().info("Removed unread settings: " + String.join(", ", report.retiredKeys()));
        }
        if (report.addedKeys() > 0) {
            plugin.getLogger().info("Added " + report.addedKeys() + " new settings to config.yml.");
        }
    }
}
```

The report also has `changed()`, true when anything at all happened. `addedKeys()` counts value keys only,
not the sections around them.

For a plugin with more than one file, the class holds no state: build one policy per file and drive the
steps yourself with `readBundledYaml` / `readYamlFile` → `applyTo(bundled, existing, policy)` → `backup` →
`tidy` → save. **Call `tidy(config)` immediately before any `save`** — Bukkit's YAML dumper folds values
longer than 80 characters across several physical lines, which turns item-id lists and message templates
into multi-line blocks that are painful to edit by hand. `tidy` pins the width, the 2-space indent and
comment parsing; `updateMainConfig` already does it for you.

`installBundledResource` / `writeStringAtomically` / `needsRestore` round the package out: install a bundled
file through a temp file and an atomic move (a crash leaves the old file intact, not half a file), and
detect a config that no longer parses or was saved in the wrong encoding so you can restore it from the jar.

### Advancements — `api.advancement.FarmersDelightAdvancements` (+ `AdvancementTree`)

Needs the **UltimateAdvancementAPI** plugin. Grant/check FarmersDelight's own tab, or register your own tab from plain data — FD builds the real UAA tab and rebuilds it across `/fd reload`:

```java
if (FarmersDelightAdvancements.isAvailable()) {
    FarmersDelightAdvancements.tree("myaddon")
        .root("root", icon, "Title", "Desc", "minecraft:textures/block/stone.png")
        .advancement("step1", "root", icon, "Step 1", "Do a thing.", "task", 1, 0)
        .register();                                            // unregister("myaddon") on disable
}
FarmersDelightAdvancements.award("myaddon", player, "step1");
FarmersDelightAdvancements.award(player, "master_chef");        // FD's own tab (no tabId)
```

Titles/descriptions are client lang keys (resolved from the resource pack) or literal text.

### Recipe queries — `api.recipe.FarmersDelightRecipes` → `RecipeInfo`

Read-only views of FD's own cooking-pot / cutting-board recipes (Bukkit types only):

```java
for (String id : FarmersDelightRecipes.cookingPotRecipeIds()) {
    RecipeInfo r = FarmersDelightRecipes.cookingPotRecipe(id);  // ingredients (id strings), results, cookTimeTicks, experience, category, container
}
// also: cuttingBoardRecipeIds(), cuttingBoardRecipe(id), matchesCookingPot(...), cookingPotResult(...)
```

### Recipe discovery — `api.recipe.FarmersDelightRecipeDiscovery` (off by default)

Per-player recipe lock/unlock in the recipe books (enable `recipe-discovery` in FD's config):

```java
if (FarmersDelightRecipeDiscovery.isEnabled()) {
    FarmersDelightRecipeDiscovery.unlock(player, "myaddon:type", "recipe_id");
    boolean known = FarmersDelightRecipeDiscovery.isUnlocked(player,
        FarmersDelightRecipeDiscovery.TYPE_COOKING_POT, "farmersdelight:beef_stew");
}
```

## Reloading is driven by FarmersDelight

Your addon needs **no command of its own**. FarmersDelight fires `FarmersDelightReloadEvent` on any
`/fd reload <target>`; listen for it and reload your config (see the example). Recipes you registered
survive the reload. Item-dependent recipes should also (re)register on `CraftEngineReloadEvent` (CE items
only resolve after CE has loaded).

## CraftEngine YAML conventions (match FarmersDelight)

- Organize **by type**, like FD: `items.yml`, `food_block.yml`, `blocks.yml`, `gui.yml`, `translations.yml`.
- **Block style only** — no inline `{ }` flow maps. Leave `item-name`/`model` unquoted; quote only when
  YAML requires it (a `#`-leading tag, or a trailing space).
- **No comments in shipping configs** (this template comments for teaching). FD's own configs are clean.
- Native CE recipe types: `shaped`, `shapeless`, `smelting`, `smoking`, `blasting`, `campfire_cooking`,
  `stonecutting`. Cooking-pot/cutting-board recipes are **not** CE recipes — register them via the API.
- Custom blocks get collision from their host state: a solid `auto-state` host (`note_block`, `mushroom`)
  gives full-cube collision; a transparent host (`non_tintable_leaves`) collides but renders cleanly for
  non-cube models; `tripwire` is non-colliding (flat food blocks). Block right-click/break logic can be
  pure CE `events` (see FD's `food_block.yml`).

See [`configuration/items.yml`](src/main/resources/craftengine/fdaddon/configuration/items.yml) for a
commented example (custom food + on-eat effect, a vanilla-item behavior override, a shaped recipe).

## Custom blocks with interaction + per-block saved content

A custom block that *does something* and *remembers state per position* is split across three places —
all of it CraftEngine's own API, not FarmersDelight (FD does not bridge block registration; it bridges
recipes / scheduling / heat / XP). Because CraftEngine's classes keep stable names, importing
`net.momirealms.craftengine.**` directly is safe (unlike FD internals).

1. **Block definition** — [`configuration/blocks.yml`](src/main/resources/craftengine/fdaddon/configuration/blocks.yml):
   `blocks.fdaddon:example_block` with `behavior: - type: fdaddon:example_block`, a `block_item` so it can
   be placed, and a shaped recipe. Collision comes from the host state (`auto-state: note_block` → solid).
   No block-entity declaration is needed — CraftEngine detects it from the behavior (see below).
2. **Behavior** — [`ExampleBlockBehavior.java`](src/main/java/com/example/fdaddon/ExampleBlockBehavior.java):
   `extends BlockBehavior implements EntityBlock`, overrides `useOnBlock(...)` for right-click logic, exposes
   a static `FACTORY`. Registered in `onLoad()` with `BlockBehaviors.register(Key.of("fdaddon:example_block"),
   FACTORY)` — do this *before* CraftEngine parses `blocks.yml`, since the config references it by id. The
   behavior is a **singleton** shared by all placed blocks, so it holds no per-block state; on interaction it
   looks up the block entity at the clicked position and acts on that block's controller.
3. **Per-block content** — [`ExampleBlockEntityController.java`](src/main/java/com/example/fdaddon/ExampleBlockEntityController.java):
   because the behavior implements `EntityBlock`, CraftEngine creates one `BlockEntityController` per placed
   block and **persists it inside the chunk**. You serialize your state in `saveCustomData(CompoundTag)` and
   restore it in `loadCustomData(CompoundTag)` — that's the whole persistence story. **Do not store block
   state in a side yml**; there's no plugin-managed save/load lifecycle to write. This is exactly how FD's
   cooking pot / skillet / stove keep their inventories and progress.
   To mark a block changed so CraftEngine re-saves it, call `blockEntity.world.blockEntityChanged(pos)`.
   For per-tick logic, override `createBlockEntityTicker(...)` on the controller.

## Debug tooling — plug into `/fd debugtools` — `api.util.DebugToolExtension`

Plug your block into FarmersDelight's debug command (mass-place / mass-activate / status / undo) without
shipping a CLI of your own. See [`debug/ExampleDebugExtension.java`](src/main/java/com/example/fdaddon/debug/ExampleDebugExtension.java)
for the wiring; one `DebugToolRegistry.register(...)` call in `onEnable` and admins get:

```
/fd debugtools place <name> [count] [spacing] [layers]   # mass-place your block
/fd debugtools activate <name>                            # fill / tick / wake your tracked blocks
/fd debugtools activate all                               # also triggers every registered extension
/fd debugtools status                                     # appends your status() lines under the FD snapshot
/fd debugtools undo                                       # reverts the last batch (your blocks too)
```

Implement four methods (interface in `com.huidu.farmersdelight.api.util`):

- `name()` — the keyword (e.g. `"example_block"`). Becomes the tab-complete entry.
- `place(player, origin, count, spacing, layers, undo)` — mass-place your block. **Call `undo.capture(loc)`
  before each mutation** so `/fd debugtools undo` can revert it. Implementations typically grid as
  `ceil(sqrt(count))` × `layers`.
- `activate(player)` (optional) — wake placed blocks (e.g. fill ingredients, start fermentation, bump a
  counter). Return the count actually transitioned.
- `status(player)` (optional) — short lines added after FD's TickManager snapshot.
- `cleanupBeforeUndo(loc)` (optional) — release in-memory tracking + remove block-entity NBT when this
  location is being undone. Skip cheaply when the location isn't yours.

Registration is safe even on a non-debug FD build (no `-PdebugTools=true`) — the registry is dormant and
your methods are never called. Plugins with a central manager (e.g. FD's `StoveManager` or BAC's
`KegManager`) should iterate the manager in `activate` / `cleanupBeforeUndo` rather than duplicate
tracking; the template's example keeps an in-memory set since the demo block has no manager.
