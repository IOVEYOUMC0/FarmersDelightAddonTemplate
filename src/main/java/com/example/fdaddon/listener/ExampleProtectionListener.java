package com.example.fdaddon.listener;

import com.example.fdaddon.ExampleBlockBehavior;
import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.util.ProtectionCompat;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockAttemptPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Region protection for the example block's place and break paths.
 *
 *
 * CraftEngine fake blocks never fire the vanilla BlockBreakEvent the land plugins listen to, and a placed
 * custom block is not a vanilla BlockPlaceEvent either — the interaction gate alone protects only
 * right-clicking. Both events below check the addon's WorldGuard StateFlag (FDAddonTemplate.EXAMPLE_FLAG) plus
 * every AntiGriefLib-backed land plugin, so a new addon that registers a namespace and stops at canUse does not
 * silently lose break/place protection.
 */
public final class ExampleProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExamplePlace(CustomBlockAttemptPlaceEvent event) {
        ImmutableBlockState state = event.blockState();
        if (state == null || state.isEmpty()
                || state.behavior().getFirst(ExampleBlockBehavior.class) == null) {
            return;
        }
        if (!ProtectionCompat.canBuild(event.getPlayer(), event.location(), FDAddonTemplate.EXAMPLE_FLAG)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExampleBreak(CustomBlockBreakEvent event) {
        ImmutableBlockState state = event.blockState();
        if (state == null || state.isEmpty()
                || state.behavior().getFirst(ExampleBlockBehavior.class) == null) {
            return;
        }
        if (!ProtectionCompat.canBreak(event.getPlayer(), event.bukkitBlock(), FDAddonTemplate.EXAMPLE_FLAG)) {
            event.setCancelled(true);
        }
    }
}
