package com.example.fdaddon.util;

import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.world.CEWorld;

import java.util.UUID;

/**
 * Version bridge for CraftEngine 26.7.4 vs 26.8. In 26.8, BukkitWorldManager#getWorld(UUID)
 * returns BukkitWorld instead of CEWorld, so a binary-incompatible call must go through this
 * shim to stay source/binary compatible with both versions. Builds are versioned per jar:
 * -PceVersion=26.7.4 (maven deps) vs -PceVersion=26.8 (local jar), see build.gradle.kts.
 */
public final class CraftEngineCompat {

    private CraftEngineCompat() {
    }

    public static CEWorld getCEWorld(BukkitWorldManager worldManager, UUID uuid) {
        Object world = worldManager.getWorld(uuid);
        if (world instanceof CEWorld ceWorld) {
            return ceWorld;
        }
        if (world instanceof net.momirealms.craftengine.core.world.World ceWorld) {
            return ceWorld.ceWorld();
        }
        return null;
    }
}
