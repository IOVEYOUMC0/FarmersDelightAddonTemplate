package com.example.fdaddon.util;

import com.huidu.farmersdelight.api.util.TooltipUtils;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.item.Item;
import org.bukkit.inventory.ItemStack;

/**
 * Hides the advanced-tooltip "Durability: X / Y" line on a CraftEngine item whose damage value is being
 * repurposed as a meter (a fill bar, a serving count, and so on) rather than tool wear. The damage bar then
 * still renders, showing the fill, but the raw number is not leaked into the tooltip.
 *
 * The pattern: wrap the Bukkit stack in CraftEngine's live item wrapper, set max-damage and damage to encode
 * your value, then hide the line. For a CraftEngine (custom) item — the intended input — the wrapper mutates
 * the same underlying stack, so the returned stack reflects the change; for a plain Bukkit stack the wrapper
 * copies and the mutation is lost. Only call this when CraftEngine is present, since it uses CraftEngine's item wrapper.
 */
public final class ExampleTooltipCustomizer {

    private ExampleTooltipCustomizer() {
    }

    /**
     * Encode amount out of capacity into the item's damage bar and hide the numeric durability line.
     * capacity and amount are your own units, for example millibuckets of a fluid. Returns a modified copy;
     * the original stack is left untouched.
     */
    public static ItemStack encodeAsMeter(ItemStack stack, int capacity, int amount) {
        if (stack == null || capacity <= 0) {
            return stack;
        }
        ItemStack copy = stack.clone();
        Item wrapped = BukkitItemManager.instance().wrap(copy);
        // max_damage = capacity, damage = capacity - amount (clamped >= 1 so a full meter still shows a
        // near-full bar, since vanilla hides the bar at damage 0). The bar width then scales with the fill.
        wrapped.maxDamage(capacity);
        wrapped.damage(Math.max(1, capacity - Math.min(capacity, amount)));
        TooltipUtils.hideDurabilityLine(wrapped);
        return copy;
    }
}
