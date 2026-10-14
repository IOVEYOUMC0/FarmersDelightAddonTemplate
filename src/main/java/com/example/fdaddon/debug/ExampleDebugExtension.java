package com.example.fdaddon.debug;

import com.huidu.farmersdelight.api.block.CraftEngineBlockAccess;
import com.example.fdaddon.ExampleBlockEntityController;
import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.util.DebugToolExtension;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Position-scoped debug callbacks; FD owns batching, protection checks and scheduling. */
public final class ExampleDebugExtension implements DebugToolExtension {

    private static final Key EXAMPLE_BLOCK = Key.of(FDAddonTemplate.NS + ":example_block");

    private final Set<Location> placed = ConcurrentHashMap.newKeySet();


    @Override
    public boolean supportsPlacement() { return true; }

    @Override
    public boolean supportsActivation() { return true; }

    @Override
    public String name() {
        // The keyword admins type: /fd debugtools place example_block ...
        return "example_block";
    }

    @Override
    public int place(Player player, Location origin, int count, int spacing, int layers, UndoSink undo) {
        if (player == null || origin == null || origin.getWorld() == null) return 0;
        BlockDefinition def = CraftEngineBlocks.byId(EXAMPLE_BLOCK);
        if (def == null) {
            player.sendMessage("§c[" + FDAddonTemplate.NS + "] " + EXAMPLE_BLOCK + " not registered with CraftEngine.");
            return 0;
        }

        // ceil(sqrt(count)) gives a square-ish footprint per layer. Layers stack on Y.
        int grid = Math.max(1, (int) Math.ceil(Math.sqrt(count)));
        int total = count * Math.max(1, layers);
        int placedCount = 0;
        for (int i = 0; i < total; i++) {
            int layer = i / count;
            int layerIndex = i % count;
            int x = layerIndex % grid;
            int z = layerIndex / grid;
            Location loc = new Location(
                    origin.getWorld(),
                    origin.getBlockX() + x * spacing,
                    origin.getBlockY() + 1 + layer,
                    origin.getBlockZ() + z * spacing
            );
            Block target = loc.getBlock();
            if (target.getType() != Material.AIR
                    && !BlockStateUtils.isReplaceable(BlockStateUtils.getBlockState(target))) {
                continue;
            }
            // Snapshot pre-state into FD's debug-tool undo batch BEFORE mutating.
            if (undo != null) undo.capture(loc);
            if (CraftEngineBlocks.place(loc, def.defaultState(), false)) {
                placed.add(cell(loc));
                placedCount++;
            }
        }
        return placedCount;
    }

    @Override
    public int activateAt(Player player, Location location) {
        if (location == null || location.getWorld() == null || !placed.contains(cell(location))) return 0;
        var controller = CraftEngineBlockAccess.blockEntityController(location.getWorld(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return controller instanceof ExampleBlockEntityController example && example.increment() > 0 ? 1 : 0;
    }

    @Override
    public List<String> status(Player player) {
        return List.of(
                "<gray>debug-placed:</gray> <aqua>" + placed.size() + "</aqua>",
                "<gray>active world:</gray> " + (player == null || player.getWorld() == null
                        ? "n/a" : player.getWorld().getName())
        );
    }

    @Override
    public void cleanupBeforeUndo(Location location) {
        if (location != null && location.getWorld() != null) placed.remove(cell(location));
    }

    private static Location cell(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
