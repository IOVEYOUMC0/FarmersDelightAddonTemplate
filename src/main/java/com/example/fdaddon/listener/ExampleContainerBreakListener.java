package com.example.fdaddon.listener;

import com.example.fdaddon.ExampleContainerBehavior;
import com.example.fdaddon.ExampleContainerController;
import com.example.fdaddon.util.AddonLang;
import com.huidu.farmersdelight.api.compat.ce.CraftEngineBlockAccess;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Break path for the container example block: CraftEngine fake blocks never fire the vanilla break event, so a
 * refused drop cancels this event and the block keeps the undelivered stacks.
 */
public final class ExampleContainerBreakListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onContainerBreak(CustomBlockBreakEvent event) {
        ImmutableBlockState state = event.blockState();
        if (state == null || state.isEmpty()
                || state.behavior().getFirst(ExampleContainerBehavior.class) == null) {
            return;
        }
        Block block = event.bukkitBlock();
        BlockEntityController controller = CraftEngineBlockAccess.blockEntityController(
                block.getWorld(), block.getX(), block.getY(), block.getZ());
        if (!(controller instanceof ExampleContainerController container)) {
            return;
        }
        if (!container.dropContents()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(AddonLang.get("fdaddon.example_container_drop_refused"));
        }
    }
}
