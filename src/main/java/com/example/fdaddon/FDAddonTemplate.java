package com.example.fdaddon;

import com.example.fdaddon.advancement.ExampleAdvancementListener;
import com.example.fdaddon.advancement.ExampleAdvancements;
import com.example.fdaddon.buff.ExampleBuffBossbar;
import com.example.fdaddon.buff.ExampleCustomBuff;
import com.example.fdaddon.debug.ExampleDebugExtension;
import com.example.fdaddon.display.ExampleItemDisplayManager;
import com.example.fdaddon.listener.ExampleFarmersDelightEventsListener;
import com.example.fdaddon.listener.ExampleReloadListener;
import com.example.fdaddon.recipe.ExampleRecipeFiller;
import com.example.fdaddon.util.ExampleFoodEffectRegistrar;
import com.example.fdaddon.util.ExampleTooltipCustomizer;
import com.huidu.farmersdelight.api.FarmersDelightApi;
import com.huidu.farmersdelight.api.buff.CustomBuffRegistry;
import com.huidu.farmersdelight.api.util.DebugToolRegistry;
import com.huidu.farmersdelight.api.util.PluginManagerGuard;
import com.huidu.farmersdelight.api.advancement.FarmersDelightAdvancements;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.recipe.FarmersDelightRecipeDiscovery;
import com.huidu.farmersdelight.api.recipe.FarmersDelightRecipes;
import com.huidu.farmersdelight.api.recipe.RecipeInfo;
import com.huidu.farmersdelight.api.scheduler.ApiTask;
import com.huidu.farmersdelight.api.text.FarmersDelightMessages;
import com.huidu.farmersdelight.api.text.FarmersDelightText;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/**
 * Example addon for the FarmersDelight (CraftEngine) plugin.
 *
 * <p>Architecture, in three sentences:
 * <ol>
 *   <li>CraftEngine defines your custom items/blocks/recipes from YAML (resources under
 *       {@code craftengine/<namespace>/}); this addon bundles them and {@link AddonResources releases}
 *       them into CraftEngine's resource folder on first load.</li>
 *   <li>FarmersDelight exposes an obfuscation-safe facade, {@link FarmersDelightApi}, for the things CE
 *       can't do alone — registering cooking-pot / cutting-board recipes, showing recipes in FD's recipe
 *       book, Folia-safe scheduling, heat-source queries, and crafting XP.</li>
 *   <li>You write the gameplay glue in Java against {@code api.**} ONLY (see README — never touch FD
 *       internals, they are renamed by ProGuard).</li>
 * </ol>
 *
 * <p>This class wires together the addon's modules. Look at the dedicated classes for examples of the
 * common patterns:
 * <ul>
 *   <li>{@link ExampleAdvancements} + {@link ExampleAdvancementListener} — advancement tab with a
 *       simple child and a multiTask challenge, awarded on FD's ProduceEvent + vanilla consume events.</li>
 *   <li>{@link ExampleReloadListener} — bridges {@code FarmersDelightReloadEvent} and
 *       {@code CraftEngineReloadEvent} to {@link #reloadAddon} / {@link #registerRecipes}.</li>
 *   <li>{@link ExampleFoodEffectRegistrar} — config-driven Comfort / Nourishment registration.</li>
 *   <li>{@link ExampleBlockBehavior} + {@link ExampleBlockEntityController} — a custom block with per-block
 *       state persisted via a CE block entity.</li>
 *   <li>{@link ExampleRecipeType} — a custom recipe type with its own book layout.</li>
 * </ul>
 *
 * <p>Delete or rename what you don't need.
 */
public final class FDAddonTemplate extends JavaPlugin {

    // Recipe ids you register should be namespaced to your addon so they never clash with other addons.
    public static final String NS = "fdaddon";

    private ApiTask heartbeat;
    private final ExampleFoodEffectRegistrar foodEffects = new ExampleFoodEffectRegistrar();
    private final ExampleCustomBuff exampleBuff = new ExampleCustomBuff();
    private final ExampleItemDisplayManager displays = new ExampleItemDisplayManager();
    private ExampleBuffBossbar buffBossbar;
    private ApiTask buffBossbarTask;

    @Override
    public void onLoad() {
        // Register custom-block behaviors with CraftEngine BEFORE it parses blocks.yml (which references
        // them by id). This is CraftEngine's OWN api (BlockBehaviors.register) — not FarmersDelight; FD
        // does not bridge block registration. CraftEngine's classes keep stable names, so this is safe.
        if (BuiltInRegistries.BLOCK_BEHAVIOR_TYPE.getValue(Key.of(NS + ":example_block")) == null) {
            BlockBehaviors.register(Key.of(NS + ":example_block"), ExampleBlockBehavior.FACTORY);
        }
        // Copy this addon's bundled CraftEngine resources into plugins/CraftEngine/resources/<namespace>/
        // (install-if-missing). Do this in onLoad so the files exist before CraftEngine scans them.
        AddonResources.release(this);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // ALWAYS guard api use with isAvailable(): it's true only when FarmersDelight is present + enabled.
        if (!FarmersDelightApi.get().isAvailable()) {
            getLogger().warning("FarmersDelight not available; addon features disabled.");
            return;
        }

        // 1) Register your addon's recipe TYPE so its recipes can be shown in a recipe book. ExampleRecipeType
        //    also provides its OWN book layout (list/detail) — see showRecipes() to open it independently.
        FarmersDelightApi.get().registerRecipeType(new ExampleRecipeType());

        // 2) Cooking-pot / cutting-board recipes load when CraftEngine items are ready. FD itself defers
        //    its recipe load to CraftEngineReloadEvent, so register yours there too (and re-register on
        //    every CE reload). These survive /fd reload (FD keeps externally-registered recipes).
        //    ExampleReloadListener listens for both FD + CE reload events.
        getServer().getPluginManager().registerEvents(new ExampleReloadListener(this), this);

        // If CraftEngine items are already loaded by the time we enable, register now as well.
        registerRecipes();

        // 3) Folia-safe scheduling example: a repeating task. runRepeating returns an ApiTask handle.
        heartbeat = FarmersDelightApi.get().runRepeating(this::heartbeat, 20L, 20L * 60L);

        // 4) Custom food effects: read item-id → duration mappings from config.yml's `food-effects` section
        //    and register them with FD. Reload-safe — applies again on /fd reload.
        foodEffects.apply(getConfig().getConfigurationSection("food-effects"));

        // 5) Advancement tab (needs UltimateAdvancementAPI): builds a tree with a root, a simple child,
        //    and a multiTask challenge. The listener awards them on FD's ProduceEvent + vanilla consume.
        ExampleAdvancements.register(getLogger());
        getServer().getPluginManager().registerEvents(new ExampleAdvancementListener(), this);

        // 6) /fd debugtools integration: lets admins mass-place / mass-activate / status / undo this
        //    addon's blocks through FD's existing debug command. Safe to register even on a non-debug
        //    FD build (the registry is dormant and the methods are never called).
        DebugToolRegistry.register(new ExampleDebugExtension(this));

        // 7) Custom buff + boss bar: register a persistent addon buff so FD's milk-cure listener and its
        //    join/quit persistence lifecycle drive it, then push its readout to FD's boss-bar channel each
        //    tick. FD owns the master boss-bar toggle/layout in its config; the addon only pushes state.
        CustomBuffRegistry.register(exampleBuff);
        buffBossbar = new ExampleBuffBossbar(this, exampleBuff);
        buffBossbarTask = FarmersDelightApi.get().runRepeating(buffBossbar::tick, 20L, 10L);

        // 8) Packet item displays: floating items rendered to nearby players with no real entity. The manager
        //    also listens for /fd cleanup so its displays are not swept as orphans, so register it as a listener.
        getServer().getPluginManager().registerEvents(displays, this);

        // 9) FarmersDelight lifecycle event bridges: cleanup / migrate / warmup / cooking-experience.
        getServer().getPluginManager().registerEvents(
                new ExampleFarmersDelightEventsListener(getLogger()), this);

        // 10) Guard against a live plugin-manager reload/unload (PlugMan and friends), which leaves
        //     CraftEngine-bound lambdas throwing after the classloader goes away. FD's guard cancels the
        //     destructive command and tells the sender to /stop instead. Pass your plugin name.
        getServer().getPluginManager().registerEvents(new PluginManagerGuard(getName()), this);

        getLogger().info("FDAddonTemplate enabled.");
    }

    @Override
    public void onDisable() {
        if (heartbeat != null) {
            heartbeat.cancel();
        }
        if (buffBossbarTask != null) {
            buffBossbarTask.cancel();
        }
        if (FarmersDelightApi.get().isAvailable()) {
            // Clean up what you registered.
            FarmersDelightApi.get().unregisterRecipeType(NS + ":example");
            FarmersDelightApi.get().unregisterCookingPotRecipe(NS + ":example_stew");
            FarmersDelightApi.get().unregisterCuttingBoardRecipe(NS + ":example_cut");
            CustomBuffRegistry.unregister(exampleBuff);
            displays.hideAll();
            foodEffects.clear();
            ExampleAdvancements.unregister();
        }
        DebugToolRegistry.unregister("example_block");
    }

    /**
     * Reload everything driven by this addon's config. Called by {@link ExampleReloadListener} on
     * {@code /fd reload all}. The reason argument is for logging only — split it out if you want
     * fine-grained reloads (recipes-only vs. effects-only).
     */
    public void reloadAddon(String reason) {
        reloadConfig();
        foodEffects.apply(getConfig().getConfigurationSection("food-effects"));
        registerRecipes();
        getLogger().info("Reloaded with FarmersDelight (reason: " + reason + ").");
    }

    // ── Recipe registration via the FD API ──────────────────────────────────────────────────────
    /**
     * Register this addon's cooking-pot and cutting-board recipes. Called from {@code onEnable},
     * {@link #reloadAddon}, and {@link ExampleReloadListener#onCraftEngineReload}.
     */
    public void registerRecipes() {
        FarmersDelightApi api = FarmersDelightApi.get();
        if (!api.isAvailable()) return;

        // Cooking-pot recipe: ingredient specs use FD's recipe syntax — "ns:id", "#ns:tag", or "a|b"
        // choices. `container` is the required bowl/bottle (null = none). `result` carries its own amount.
        ItemStack stew = FarmersDelightItems.create("minecraft:rabbit_stew");
        if (stew != null) { // create(...) returns null if the item id isn't loaded yet
            api.registerCookingPotRecipe(
                    NS + ":example_stew",
                    List.of("minecraft:carrot", "minecraft:potato", "#minecraft:rabbit"),
                    FarmersDelightItems.create("minecraft:bowl"), // container
                    stew,                                          // result
                    1.0,                                           // experience
                    200,                                           // cook time (ticks)
                    "meals");                                      // category
        }

        // Cutting-board recipe: input + tool (both recipe-syntax), one or more result stacks.
        ItemStack plank = FarmersDelightItems.create("minecraft:oak_planks");
        if (plank != null) {
            plank.setAmount(2);
            api.registerCuttingBoardRecipe(
                    NS + ":example_cut",
                    "minecraft:oak_log",       // input
                    "#minecraft:axes",         // tool (tag)
                    List.of(plank),            // results
                    "minecraft:block.wood.break"); // sound (null = default knife sound)
        }
    }

    // ── Other api capabilities ───────────────────────────────────────────────────────────────────
    private void heartbeat() {
        // Example: nothing to do here — see the methods below for what the api offers.
    }

    /** Open the recipe book for a player (e.g. from your own block/GUI). */
    public void showRecipes(Player player) {
        // Independent book: opens straight to YOUR type using the layouts ExampleRecipeType provides — never
        // a shared category menu with other addons. The RecipeFiller adds a "Fill" button; construct a fresh
        // one per open, bound to the station the book was opened from, so Fill/back target that station.
        FarmersDelightApi.get().openRecipeBook(player, NS + ":example", new ExampleRecipeFiller());
        // Alternatives:
        //   openRecipeBook(player)                 -> FD's shared book, read-only (all registered types).
        //   openRecipeBook(player, filler)         -> shared book with a Fill button.
    }

    /** Heat-source queries (e.g. for your own cooking block): is this block a configured heat source? */
    public boolean isHeated(Block below) {
        FarmersDelightApi api = FarmersDelightApi.get();
        return api.isHeatSource(below) || api.isConductor(below);
    }

    /** Folia-safe scheduling: run on the region that owns a location (immediate on Paper). */
    public void doAtBlock(Location loc, Runnable task) {
        FarmersDelightApi.get().runAtLocation(loc, task);
        // Also available: runLaterAtLocation(loc, task, delayTicks) and runRepeating(task, delay, period).
    }

    /** Award crafting XP like the cooking pot: drops XP orbs (config-gated), AuraSkills XP, fires an event. */
    public void rewardCraft(Player player, Location at, ItemStack result) {
        FarmersDelightApi.get().awardCraftingExperience(player, at, result, 1.0, NS + ":example_stew");
    }

    /** Item helpers — CraftEngine-aware id resolution + matching (use these instead of raw Material checks). */
    public void itemHelpers(ItemStack stack) {
        String id = FarmersDelightItems.idOf(stack);                 // "ns:id" (custom) or "minecraft:<material>"
        boolean isCarrot = FarmersDelightItems.matchesId(stack, "minecraft:carrot");
        boolean isLog = FarmersDelightItems.matchesTag(stack, "minecraft:logs");
        boolean custom = FarmersDelightItems.isCustomItem(stack);    // true for a CraftEngine custom item
        String customId = FarmersDelightItems.customIdOf(stack);     // custom id, or null for vanilla
        ItemStack remainder = FarmersDelightItems.craftingRemainderOf(stack); // e.g. bucket from a milk bucket
        getLogger().fine(id + " carrot=" + isCarrot + " log=" + isLog + " custom=" + custom
                + " customId=" + customId + " remainder=" + remainder);
    }

    /** Read-only recipe queries: list FD's cooking-pot / cutting-board recipes and inspect one as a RecipeInfo. */
    public void recipeQueries() {
        for (String id : FarmersDelightRecipes.cookingPotRecipeIds()) {
            RecipeInfo info = FarmersDelightRecipes.cookingPotRecipe(id);
            if (info != null) {
                getLogger().fine(id + " ingredients=" + info.ingredients() + " results=" + info.results()
                        + " time=" + info.cookTimeTicks() + " xp=" + info.experience());
            }
        }
    }

    /** Rich text + player messages: render templates with {key} placeholders, <l10n:key>/<lang:key> tags
     * (per-viewer locale), CraftEngine <image:ns:id>/<shift:N> glyphs, and MiniMessage + legacy colors. */
    public void textAndMessages(Player player) {
        FarmersDelightMessages.send(player, "<green>Hello {name}!", Map.of("name", player.getName()));
        FarmersDelightMessages.actionBar(player, "<gold>Saved");
        FarmersDelightMessages.title(player, "<aqua>Title", "<gray>Subtitle", 10, 40, 10, Map.of());
        Component rendered = FarmersDelightText.render("<yellow>x{n}", player, Map.of("n", "3"));
        // Build a GUI icon with a rendered name + lore (shares FD's rendering: l10n tags, glyphs, colors):
        ItemStack icon = FarmersDelightItems.buildIcon("minecraft:apple", "<gold>Shiny Apple",
                List.of("<gray>Line one", "<gray>Line two"), player, Map.of());
        getLogger().fine("rendered=" + rendered + " icon=" + icon);
    }

    /** Recipe discovery (lock/unlock; off by default): unlock a recipe for a player or check its state. */
    public void recipeDiscovery(Player player) {
        if (FarmersDelightRecipeDiscovery.isEnabled()) {
            FarmersDelightRecipeDiscovery.unlock(player, NS + ":example", "example");
            boolean known = FarmersDelightRecipeDiscovery.isUnlocked(player,
                    FarmersDelightRecipeDiscovery.TYPE_COOKING_POT, "farmersdelight:beef_stew");
            getLogger().fine("beef_stew unlocked=" + known);
        }
    }

    /** Advancements: grant / check on your addon tab. The {@link ExampleAdvancements} helper wraps the
     * tab-id boilerplate; the listener handles automatic awarding on player actions. */
    public void advancements(Player player) {
        ExampleAdvancements.award(player, ExampleAdvancements.FIRST_STEW);
        boolean done = ExampleAdvancements.has(player, ExampleAdvancements.FIRST_STEW);
        // For FD's own tab (not your addon's): FarmersDelightAdvancements.award(player, "master_chef").
        FarmersDelightAdvancements.award(player, "master_chef");
        getLogger().fine("first_stew=" + done);
    }

    /** Apply the example custom buff (level 1, 60s). FD's boss-bar feed renders it while it is active; FD's
     * milk-bottle cure can clear it, and it survives a rejoin via the buff's saveState/restoreState. */
    public void giveExampleBuff(Player player) {
        // Direct call, or generically by id: CustomBuffRegistry.apply(player, ExampleCustomBuff.ID, 1, 60).
        exampleBuff.apply(player, 1, 60);
    }

    /** Show a floating item at a location via FD's packet display API (no real entity is spawned). */
    public void showFloatingItem(Location where, ItemStack item) {
        if (where.getWorld() == null) {
            return;
        }
        String anchor = where.getWorld().getUID() + ";" + where.getBlockX() + ";"
                + where.getBlockY() + ";" + where.getBlockZ();
        displays.show(anchor, where, item);
        // Later: displays.update(anchor, movedLocation, item) to move it, displays.hide(anchor) to remove it.
    }

    /** Encode a fill level into a CraftEngine item's damage bar and hide the numeric durability line. */
    public ItemStack asFluidMeter(ItemStack craftEngineItem, int capacityMb, int amountMb) {
        return ExampleTooltipCustomizer.encodeAsMeter(craftEngineItem, capacityMb, amountMb);
    }
}
