package com.example.fdaddon.listener;

import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.event.FarmersDelightCleanupEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightMigrateEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightWarmupEvent;
import com.huidu.farmersdelight.api.event.ProfessionCookingExperienceEvent;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

/**
 * Demonstrates the FarmersDelight lifecycle event bridges an addon can subscribe to. All are plain Bukkit
 * events, so register this class in onEnable. None are cancellable; the only way to influence FarmersDelight
 * is the accumulators on the cleanup and migrate events.
 */
public final class ExampleFarmersDelightEventsListener implements Listener {

    private static final String NAMESPACE = FDAddonTemplate.NS;

    private final Logger logger;

    public ExampleFarmersDelightEventsListener(Logger logger) {
        this.logger = logger;
    }

    /**
     * Fired by /fd cleanup after FarmersDelight removed its own orphans. Remove your addon's orphan state in
     * the same admin pass and report the count, which the command sums into its reply. On Folia, do best-
     * effort regional scheduling and count what you scheduled to remove.
     */
    @EventHandler
    public void onCleanup(FarmersDelightCleanupEvent event) {
        int removed = 0;
        // Sweep your own orphan entities or stale records here, incrementing removed for each.
        event.addRemoved(removed);
    }

    /**
     * Fired after a /fd migration ran. Gate on the migration key so you only act on migrations you own, then
     * migrate your own legacy data and report how many entries you converted. The accessor is migrationKey(),
     * with no get prefix.
     */
    @EventHandler
    public void onMigrate(FarmersDelightMigrateEvent event) {
        if (!"example".equals(event.migrationKey())) {
            return; // not a migration this addon owns
        }
        int migrated = 0;
        // Convert your own legacy data here, incrementing migrated for each entry.
        event.addRemoved(migrated);
    }

    /**
     * Fired once CraftEngine items are ready, on startup and after each /ce reload. Pre-build your own
     * CraftEngine item stacks off the hot path so the first interaction is cheap. Handlers must be PURE
     * computation: no world, entity, region or block access. getReason() is "enable" or "reload", or null.
     */
    @EventHandler
    public void onWarmup(FarmersDelightWarmupEvent event) {
        int warmed = 0;
        for (Key key : CraftEngineItems.loadedItems().keySet()) {
            if (NAMESPACE.equals(key.namespace())) {
                FarmersDelightItems.create(key.toString()); // build (and cache in your own code) each item
                warmed++;
            }
        }
        logger.fine("Warmed " + warmed + " items (reason=" + event.getReason() + ").");
    }

    /**
     * Read-only notification of cooking-pot or addon crafting experience. It cannot alter the experience.
     * getResult() returns a defensive clone and may be null, so guard it before use.
     */
    @EventHandler
    public void onCookingExperience(ProfessionCookingExperienceEvent event) {
        ItemStack result = event.getResult();
        if (result == null) {
            return;
        }
        logger.fine(event.getPlayerName() + " earned " + event.getBaseExperience()
                + " XP from " + event.getSource() + " -> " + result.getType());
    }
}
