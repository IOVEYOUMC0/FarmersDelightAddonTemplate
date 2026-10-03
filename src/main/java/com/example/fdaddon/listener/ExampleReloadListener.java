package com.example.fdaddon.listener;

import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.event.FarmersDelightReloadEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightWarmupEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Bridges FarmersDelight's reload and warmup events to this addon, so a single /fd reload all re-syncs
 * everything — the addon needs no own command.
 *
 * Why a separate class (vs. handling on the plugin main):
 * - Keeps listener registration explicit in onEnable — easier to disable temporarily.
 * - Lets the plugin main implement only its core responsibilities (lifecycle, registries).
 * - Mirrors the production pattern in BrewinAndChewin's BrewinReloadListener.
 */
public final class ExampleReloadListener implements Listener {

    private final FDAddonTemplate plugin;

    public ExampleReloadListener(FDAddonTemplate plugin) {
        this.plugin = plugin;
    }

    /**
     * Fired by FarmersDelight on any /fd reload <target>. React by reloading YOUR config so the
     * whole plugin stays in sync from one command.
     */
    @EventHandler
    public void onFarmersDelightReload(FarmersDelightReloadEvent event) {
        plugin.reloadAddon(event.getReason());
    }

    /**
     * Fired once CraftEngine has built its items. This is where anything item-dependent belongs — most
     * commonly registering a recipe that must be decided at runtime (a static one belongs in this addon's
     * pack instead; see the recipe comment in the plugin main).
     *
     *
     * Do NOT use CraftEngine's own reload event for this: when CraftEngine broadcasts
     * CraftEngineReloadEvent its items are not built yet, so FarmersDelightItems.create still
     * returns null and any recipe referencing a custom item is silently dropped. FarmersDelight waits for
     * readiness and then broadcasts this event; every real addon uses it (see EndsDelight's warmup handler
     * or BrewinAndChewin's BrewinWarmupListener).
     */
    @EventHandler
    public void onFarmersDelightWarmup(FarmersDelightWarmupEvent event) {
    }
}
