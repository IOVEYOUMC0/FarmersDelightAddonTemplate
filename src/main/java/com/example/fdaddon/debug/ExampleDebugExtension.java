package com.example.fdaddon.debug;

import com.example.fdaddon.ExampleBlockEntityController;
import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.util.DebugToolExtension;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugs this addon into FarmersDelight's /fd debugtools command — so admins can stress-test
 * the addon's custom block (mass-place, mass-activate, status, undo) without the addon needing to
 * ship a CLI of its own.
 *
 * Register one instance in onEnable() via
 * com.huidu.farmersdelight.api.util.DebugToolRegistry.register. The registration is safe even
 * when FD is built without -PdebugTools=true: the registry is dormant and these methods are
 * never called.
 *
 * This example keeps its own in-memory set of placed locations because the demo block has no
 * central manager. Plugins with a manager (e.g. BAC's KegManager) should iterate the manager
 * directly in activate / cleanupBeforeUndo instead of duplicating tracking.
 */
public final class ExampleDebugExtension implements DebugToolExtension {

    private static final Key EXAMPLE_BLOCK = Key.of(FDAddonTemplate.NS + ":example_block");

    private final FDAddonTemplate plugin;
    // Tracks every location this extension has placed (per-world), so activate() / cleanupBeforeUndo()
    // know what to act on. ConcurrentHashMap+Set so place() can run on the player thread while a
    // long-running activate() iterates safely.
    private final Set<Location> placed = ConcurrentHashMap.newKeySet();

    public ExampleDebugExtension(FDAddonTemplate plugin) {
        this.plugin = plugin;
    }

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
                placed.add(loc.clone());
                placedCount++;
            }
        }
        return placedCount;
    }

    @Override
    public int activate(Player player) {
        if (player == null) return 0;
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(player.getWorld().getUID());
        if (ceWorld == null) return 0;

        int activated = 0;
        // Defensive copy: iterate over a snapshot so a concurrent place() on the player thread
        // can't cause a ConcurrentModificationException while we walk the placed set.
        for (Location loc : new ArrayList<>(placed)) {
            if (loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) continue;
            BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(
                    new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            if (blockEntity == null) continue;
            int[] count = {0};
            // A single-controller EntityBlock always gets controller id 0, so 0 is correct here.
            blockEntity.controller.let(ExampleBlockEntityController.class, 0,
                    controller -> { count[0] = controller.increment(); });
            if (count[0] > 0) activated++;
        }
        return activated;
    }

    @Override
    public List<String> status(Player player) {
        return List.of("tracked example_blocks (debug-placed): " + placed.size());
    }

    @Override
    public void cleanupBeforeUndo(Location location) {
        if (location == null) return;
        // O(n) but the placed set is bounded by the per-batch place count + history depth (FD limits
        // both). Use Iterator.remove() so we don't allocate a temp comparator.
        Iterator<Location> it = placed.iterator();
        while (it.hasNext()) {
            Location loc = it.next();
            if (loc.getBlockX() == location.getBlockX()
                    && loc.getBlockY() == location.getBlockY()
                    && loc.getBlockZ() == location.getBlockZ()
                    && loc.getWorld() != null && loc.getWorld().equals(location.getWorld())) {
                it.remove();
                return;
            }
        }
    }
}
