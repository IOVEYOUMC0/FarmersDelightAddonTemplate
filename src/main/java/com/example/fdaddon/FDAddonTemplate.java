package com.example.fdaddon;

import com.huidu.farmersdelight.api.FarmersDelightApi;
import com.huidu.farmersdelight.api.event.FarmersDelightReloadEvent;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.scheduler.ApiTask;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

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
 * <p>This class is a tour of every api call. Delete what you don't need.
 */
public final class FDAddonTemplate extends JavaPlugin implements Listener {

    // Recipe ids you register should be namespaced to your addon so they never clash with other addons.
    private static final String NS = "fdaddon";

    private ApiTask heartbeat;

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

        // 1) Register your addon's recipe TYPE so its recipes appear in FD's recipe book
        //    (opened via /fd recipe book, the recipe-menu button, or FarmersDelightApi.openRecipeBook).
        //    When yours is the only registered type, the book opens straight to your recipe list.
        FarmersDelightApi.get().registerRecipeType(new ExampleRecipeType());

        // 2) Cooking-pot / cutting-board recipes load when CraftEngine items are ready. FD itself defers
        //    its recipe load to CraftEngineReloadEvent, so register yours there too (and re-register on
        //    every CE reload). These survive /fd reload (FD keeps externally-registered recipes).
        getServer().getPluginManager().registerEvents(this, this);

        // If CraftEngine items are already loaded by the time we enable, register now as well.
        registerRecipes();

        // 3) Folia-safe scheduling example: a repeating task. runRepeating returns an ApiTask handle.
        heartbeat = FarmersDelightApi.get().runRepeating(this::heartbeat, 20L, 20L * 60L);

        getLogger().info("FDAddonTemplate enabled.");
    }

    @Override
    public void onDisable() {
        if (heartbeat != null) {
            heartbeat.cancel();
        }
        if (FarmersDelightApi.get().isAvailable()) {
            // Clean up what you registered.
            FarmersDelightApi.get().unregisterRecipeType(NS + ":example");
            FarmersDelightApi.get().unregisterCookingPotRecipe(NS + ":example_stew");
            FarmersDelightApi.get().unregisterCuttingBoardRecipe(NS + ":example_cut");
        }
    }

    // ── Reload integration ──────────────────────────────────────────────────────────────────────
    // FarmersDelight fires this on any `/fd reload <target>`. React by reloading YOUR config so the
    // whole plugin stays in sync from one command (your addon needs no /reload command of its own).
    @EventHandler
    public void onFarmersDelightReload(FarmersDelightReloadEvent event) {
        reloadConfig();
        registerRecipes(); // re-apply your recipes (idempotent; FD coalesces the rebuild)
        getLogger().info("Reloaded with FarmersDelight (reason: " + event.getReason() + ").");
    }

    // CraftEngine (re)loaded its items — now item ids resolve, so (re)register item-dependent recipes.
    @EventHandler
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        registerRecipes();
    }

    // ── Recipe registration via the FD API ──────────────────────────────────────────────────────
    private void registerRecipes() {
        FarmersDelightApi api = FarmersDelightApi.get();

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

    /** Open FD's recipe book for a player (e.g. from your own block/GUI). */
    public void showRecipes(Player player) {
        // No filler -> read-only book. Pass a RecipeFiller to add a "Fill" button that fills your station.
        FarmersDelightApi.get().openRecipeBook(player);
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
        getLogger().fine(id + " carrot=" + isCarrot + " log=" + isLog);
    }
}
