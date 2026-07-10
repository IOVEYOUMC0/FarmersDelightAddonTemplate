package com.example.fdaddon.listener;

import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.event.FarmersDelightReloadEvent;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Bridges FarmersDelight's and CraftEngine's reload events to this addon's reload routine, so a single
 * {@code /fd reload all} (or {@code /ce reload}) re-syncs everything — the addon needs no own command.
 *
 * <p>Why a separate class (vs. handling on the plugin main):
 *   <ul>
 *     <li>Keeps listener registration explicit in {@code onEnable} — easier to disable temporarily.</li>
 *     <li>Lets the plugin main implement only its core responsibilities (lifecycle, registries).</li>
 *     <li>Mirrors the production pattern in BrewinAndChewin's {@code BrewinReloadListener}.</li>
 *   </ul>
 */
public final class ExampleReloadListener implements Listener {

    private final FDAddonTemplate plugin;

    public ExampleReloadListener(FDAddonTemplate plugin) {
        this.plugin = plugin;
    }

    /**
     * Fired by FarmersDelight on any {@code /fd reload <target>}. React by reloading YOUR config so the
     * whole plugin stays in sync from one command.
     */
    @EventHandler
    public void onFarmersDelightReload(FarmersDelightReloadEvent event) {
        plugin.reloadAddon(event.getReason());
    }

    /**
     * Fired by CraftEngine when items/blocks are (re)loaded. Re-register any recipes that reference CE
     * custom items here — by now their ids resolve via {@code FarmersDelightItems.create(...)}.
     */
    @EventHandler
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        plugin.registerRecipes();
    }
}
