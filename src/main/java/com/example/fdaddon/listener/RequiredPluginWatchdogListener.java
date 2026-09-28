package com.example.fdaddon.listener;

import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.FarmersDelightApi;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

import java.util.Set;

/**
 * Disables this addon with a clear message when FarmersDelight or CraftEngine is disabled at runtime,
 * instead of letting its tasks, listeners and CraftEngine-bound callbacks throw NoClassDefFoundError.
 * FarmersDelight installs the same guard one level down for CraftEngine.
 */
public final class RequiredPluginWatchdogListener implements Listener {

    private static final Set<String> REQUIRED = Set.of("FarmersDelight", "CraftEngine");

    private final FDAddonTemplate plugin;

    public RequiredPluginWatchdogListener(FDAddonTemplate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (!REQUIRED.contains(event.getPlugin().getName())) {
            return;
        }
        if (plugin.getServer().isStopping() || !plugin.isEnabled()) {
            // Normal shutdown, or this addon is already going down; nothing to do.
            return;
        }
        plugin.getLogger().severe(FarmersDelightApi.consoleMessage("plugin.missing_dependency",
                "name", "FDAddonTemplate", "dependency", event.getPlugin().getName()));
        plugin.getServer().getPluginManager().disablePlugin(plugin);
    }
}
