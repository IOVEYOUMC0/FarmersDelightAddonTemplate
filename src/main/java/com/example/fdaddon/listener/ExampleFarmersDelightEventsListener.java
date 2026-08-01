package com.example.fdaddon.listener;

import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.block.CookingPotSnapshot;
import com.huidu.farmersdelight.api.block.FarmersDelightBlocks;
import com.huidu.farmersdelight.api.event.FarmersDelightBuffChangeEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightCleanupEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightCookStartEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightHarvestEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightMigrateEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightRecipeDiscoveryEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightWarmupEvent;
import com.huidu.farmersdelight.api.event.ProfessionCookingExperienceEvent;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
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
     * getResult() returns a defensive clone and may be null, so guard it before use. getLocation() carries
     * the station the experience came from (the "cooking-experience-location" feature), or null.
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

    /**
     * Fired once when a cooking pot goes from idle to cooking a matched recipe (the "cook-start-event"
     * feature) — the edge at the start of a batch, the counterpart to FarmersDelightProduceEvent at the end.
     * Not cancellable and fired on the pot's region thread outside the block-entity lock, so a listener may
     * read the pot straight away through the block station-query facade (FarmersDelightBlocks#cookingPot).
     */
    @EventHandler
    public void onCookStart(FarmersDelightCookStartEvent event) {
        Location location = event.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        // We are on the region thread that owns the pot (see the event javadoc), so a read-only snapshot of
        // the block is safe here without scheduling. See FDAddonTemplate#readCookingPot for the full pattern.
        CookingPotSnapshot pot = FarmersDelightBlocks.cookingPot(location.getBlock());
        double progress = pot == null ? 0.0D : pot.progressFraction();
        logger.fine("Cooking started: " + event.getRecipeId() + " (" + event.getCookTimeTicks()
                + " ticks, progress=" + progress + ").");
    }

    /**
     * Fired after a player harvests a FarmersDelight crop through one of the plugin's Java harvest handlers
     * (mushroom colonies with shears/knife, mature rice) — the "harvest-event" feature. A notification hook
     * for stats / quests. Not cancellable (vetoing belongs to the protection layer, which FarmersDelight has
     * already consulted). getDrops() is best-effort and may be empty when the drops come from a CraftEngine
     * loot table; an empty list means "not enumerable", not "no drops". Tomato harvests are NOT reported here
     * (they are pure CraftEngine YAML) — listen to CraftEngine's own CustomBlockInteractEvent for those.
     */
    @EventHandler
    public void onHarvest(FarmersDelightHarvestEvent event) {
        logger.fine(event.getPlayerName() + " harvested " + event.getBlockId()
                + " with " + (event.getTool() == null ? "bare hands" : event.getTool().getType())
                + " -> " + event.getDrops().size() + " enumerable drop(s).");
    }

    /**
     * Fired when a registered custom buff's level on a player really changes — gained, lost, or moved between
     * non-zero levels (the "buff-change-event" feature). NOT fired on a mere refresh (re-drinking at the same
     * level), so a listener sees one event per real transition rather than one per tick. Gate on getBuffId()
     * for buffs you care about. Fired on the affected player's region thread.
     */
    @EventHandler
    public void onBuffChange(FarmersDelightBuffChangeEvent event) {
        if (!(NAMESPACE + ":example_buff").equals(event.getBuffId())) {
            return; // not a buff this addon owns
        }
        if (event.isGained()) {
            logger.fine(event.getPlayerName() + " gained the buff at level " + event.getNewLevel()
                    + " (" + event.getRemainingSeconds() + "s).");
        } else if (event.isLost()) {
            logger.fine(event.getPlayerName() + " lost the buff.");
        } else {
            logger.fine(event.getPlayerName() + " moved from level " + event.getPreviousLevel()
                    + " to " + event.getNewLevel() + ".");
        }
    }

    /**
     * Fired when a player's recipe-discovery state really changes: a locked recipe is unlocked, or an
     * unlocked one is re-locked (the "recipe-discovery" surface). Redundant calls fire nothing, so a listener
     * sees exactly one event per transition. Not cancellable — the state is already committed. The named
     * player may be offline (the admin command can change stored state for an absent player), so act on the
     * id, not on live world state. Gate on getTypeId() for the types you own.
     */
    @EventHandler
    public void onRecipeDiscovery(FarmersDelightRecipeDiscoveryEvent event) {
        if (!(NAMESPACE + ":example").equals(event.getTypeId())) {
            return; // only react to this addon's own recipe type
        }
        logger.fine(event.getAction() + " " + event.getRecipeId()
                + " for " + event.getPlayerId() + " (source=" + event.getSource() + ").");
    }
}
