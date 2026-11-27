package com.example.fdaddon.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

/**
 * Spawns a stack and reports whether it entered the world. Paper returns an entity wrapper even when
 * ItemSpawnEvent is cancelled, so callers must only clear their stored copy after this returns true.
 */
public final class DropSafety {

    private DropSafety() {
    }

    /** True when the stack is in the world; false when another plugin refused the spawn or the input is empty. */
    public static boolean spawnVerified(World world, Location at, ItemStack item) {
        if (world == null || at == null || item == null || item.isEmpty()) {
            return false;
        }
        Item[] created = new Item[1];
        Item dropped;
        try {
            dropped = world.dropItemNaturally(at, item, entity -> created[0] = entity);
        } catch (RuntimeException | LinkageError failure) {
            // The wrapper captured by the callback is the only proof when the throw happens after insertion.
            if (created[0] != null && created[0].isValid()) {
                return true;
            }
            throw failure;
        }
        return dropped != null && dropped.isValid();
    }
}
