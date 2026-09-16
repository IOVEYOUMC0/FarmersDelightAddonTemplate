package com.example.fdaddon.listener;

import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.event.FarmersDelightReloadEvent;
import com.huidu.farmersdelight.api.event.FarmersDelightWarmupEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Bridges FarmersDelight's and CraftEngine's reload events to this addon's reload routine, so a single
 * /fd reload all (or /ce reload) re-syncs everything — the addon needs no own command.
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
     * Register recipes here, NOT in CraftEngine's own reload event.
     *
     * <p>When CraftEngine broadcasts CraftEngineReloadEvent its items are not built yet, so
     * {@code FarmersDelightItems.create} still returns null and any recipe referencing a custom item is
     * silently dropped. FarmersDelight waits for readiness and then broadcasts this event; every real
     * addon uses it (see EndsDelight.onFarmersDelightWarmup or BrewinWarmupListener).
     */
    @EventHandler
    public void onFarmersDelightWarmup(FarmersDelightWarmupEvent event) {
        plugin.registerRecipes();
    }
}
