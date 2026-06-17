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
  `idsOf` / `tagIdsOf`. CraftEngine-aware; use instead of raw `Material` checks.
- `api.recipe.*` — `RecipeType`, `ViewableRecipe`, `RecipeFiller`, `RecipeEditor`, `EditableRecipe`,
  `NumericField`, `RecipeBookLayout` (give your type its own book layout — see `ExampleRecipeType`), and
  read-only `FarmersDelightRecipes` (query FD's own cooking-pot/cutting-board recipes).
  - **Independent recipe book**: override `RecipeType.listLayout()` / `detailLayout()` to return a
    `RecipeBookLayout` (title + grid + decoration items, built from your own gui.yml; titles may carry
    CraftEngine `<image:>`/`<shift:>` for custom-texture backgrounds). `ViewableRecipe.displaySlots()`
    maps custom roles (e.g. `fluid`, `tool`) to extra detail slots. Open with `openRecipeBook(player,
    typeId, filler)`. Return null from the layouts to fall back to FD's shared book.
- `api.scheduler.ApiTask` — cancel handle for repeating tasks.
- `api.event.*` — `FarmersDelightReloadEvent`, `FarmersDelightProduceEvent`, `ProfessionCookingExperienceEvent`.

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
   cooking pot / skillet / stove keep their inventories and progress. (FD *does* have a `block_storage.yml`,
   but that is legacy migration for data written before block entities existed — not the pattern to copy.)
   To mark a block changed so CraftEngine re-saves it, call `blockEntity.world.blockEntityChanged(pos)`.
   For per-tick logic, override `createBlockEntityTicker(...)` on the controller.
