package com.example.fdaddon.util;

import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.world.CEWorld;

import java.util.UUID;

/** Resolves the CE storage world for the configured CraftEngine 26.8 runtime. */
public final class CraftEngineCompat {

    private CraftEngineCompat() {
    }

    public static CEWorld getCEWorld(BukkitWorldManager worldManager, UUID uuid) {
        net.momirealms.craftengine.bukkit.world.BukkitWorld world = worldManager.getWorld(uuid);
        return world == null ? null : world.ceWorld();
    }
}
