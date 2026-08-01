package com.example.fdaddon.listener;

import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.event.FarmersDelightReloadEvent;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
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
     * Fired by CraftEngine when items/blocks are (re)loaded. Re-register any recipes that reference CE
     * custom items here — by now their ids resolve via FarmersDelightItems.create(...).
     */
    @EventHandler
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        plugin.registerRecipes();
    }
}
