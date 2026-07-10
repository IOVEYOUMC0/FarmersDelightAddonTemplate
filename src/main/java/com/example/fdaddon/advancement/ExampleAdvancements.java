package com.example.fdaddon.advancement;

import com.huidu.farmersdelight.api.advancement.AdvancementTree;
import com.huidu.farmersdelight.api.advancement.FarmersDelightAdvancements;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;

/**
 * Builds and exposes this addon's advancement tab. Demonstrates the three patterns most addons need:
 *   <ul>
 *     <li>A root advancement (always required).</li>
 *     <li>A simple child advancement awarded by one action.</li>
 *     <li>A {@link AdvancementTree#multiTask multiTask} advancement that completes when every named
 *         criterion is granted — used for "do all N of these" challenges.</li>
 *   </ul>
 *
 * <p>Translation keys: titles/descriptions are resolved from the client's resource pack via
 * {@code <tab-id>.advancement.<id>} and {@code <tab-id>.advancement.<id>.desc}. Add matching entries to your
 * lang file or the keys will render verbatim.
 *
 * <p>Requires {@code UltimateAdvancementAPI}. All calls go through {@link FarmersDelightAdvancements} so
 * they are no-ops when UAA is missing.
 */
public final class ExampleAdvancements {

    public static final String TAB_ID = "fdaddon";

    // Advancement ids (used in award/has calls + as translation key suffixes).
    public static final String ROOT = "root";
    public static final String FIRST_STEW = "first_stew";
    public static final String SAMPLER_PLATTER = "sampler_platter";

    // Criteria for the multiTask advancement. The player must trigger all three to complete it.
    public static final List<String> SAMPLER_PLATTER_CRITERIA = List.of(
            "rabbit_stew",
            "beetroot_soup",
            "mushroom_stew"
    );

    private ExampleAdvancements() {
    }

    /**
     * Builds the tab and registers it with FarmersDelight. Call from {@code onEnable} after FD is ready.
     * Returns false if UAA is missing — that's a normal soft-dep no-op, not an error.
     */
    public static boolean register(Logger log) {
        if (!FarmersDelightAdvancements.isAvailable()) {
            if (log != null) log.info("Example advancements disabled (UltimateAdvancementAPI not loaded).");
            return false;
        }

        boolean ok = FarmersDelightAdvancements.tree(TAB_ID)
                .root(ROOT,
                        icon("minecraft:apple", Material.APPLE),
                        prefix(ROOT), prefix(ROOT) + ".desc",
                        "minecraft:textures/block/oak_planks.png")
                .advancement(FIRST_STEW, ROOT,
                        icon("minecraft:rabbit_stew", Material.RABBIT_STEW),
                        prefix(FIRST_STEW), prefix(FIRST_STEW) + ".desc", "task", 1, 0)
                .multiTask(SAMPLER_PLATTER, FIRST_STEW,
                        icon("minecraft:mushroom_stew", Material.MUSHROOM_STEW),
                        prefix(SAMPLER_PLATTER), prefix(SAMPLER_PLATTER) + ".desc",
                        "challenge", 2, 0, SAMPLER_PLATTER_CRITERIA)
                .register();

        if (log != null) {
            log.info(ok ? "Example advancements registered." : "Example advancements registration failed.");
        }
        return ok;
    }

    /** Unregister the tab. Call from {@code onDisable}. */
    public static void unregister() {
        FarmersDelightAdvancements.unregister(TAB_ID);
    }

    // ── Convenience wrappers so callers can use one short call instead of TAB_ID + advancement id pairs. ──

    public static void award(Player player, String advancementId) {
        FarmersDelightAdvancements.award(TAB_ID, player, advancementId);
    }

    public static void awardCriterion(Player player, String advancementId, String criterion) {
        FarmersDelightAdvancements.awardCriteria(TAB_ID, player, advancementId, criterion);
    }

    public static boolean has(Player player, String advancementId) {
        return FarmersDelightAdvancements.has(TAB_ID, player, advancementId);
    }

    /** Build an icon, falling back to a vanilla {@link Material} if the CraftEngine item id won't resolve. */
    private static ItemStack icon(String ceId, Material fallback) {
        ItemStack stack = ceId == null ? null : FarmersDelightItems.create(ceId);
        return stack != null && !stack.getType().isAir() ? stack : new ItemStack(fallback);
    }

    private static String prefix(String key) {
        return TAB_ID + ".advancement." + key;
    }
}
