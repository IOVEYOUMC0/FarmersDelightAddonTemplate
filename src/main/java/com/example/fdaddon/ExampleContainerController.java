package com.example.fdaddon;

import com.example.fdaddon.util.AddonLang;
import com.example.fdaddon.util.DropRecoveryArchive;
import com.example.fdaddon.util.StoredContents;
import com.huidu.farmersdelight.api.compat.ce.CraftEngineBlockAccess;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * Stores items and gives them back on interaction. A break is vetoed when a drop is refused
 * (ExampleContainerBreakListener) and a removal that cannot be vetoed archives the rest (onRemove).
 */
public final class ExampleContainerController extends BlockEntityController {

    public static final int SLOT_COUNT = 2;
    private static final String SLOT_KEY = "slot_";

    private final StoredContents contents = new StoredContents(SLOT_COUNT);

    public ExampleContainerController(BlockEntity blockEntity) {
        super(blockEntity);
    }

    /** Stores one item of the given stack; false when the container is full. */
    public boolean store(ItemStack stack) {
        boolean stored = contents.store(stack);
        if (stored) {
            markChanged();
        }
        return stored;
    }

    /** Removes one stored item; null when the container is empty. */
    public ItemStack takeOne() {
        ItemStack taken = contents.takeOne();
        if (taken != null) {
            markChanged();
        }
        return taken;
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    /**
     * Drops every stored stack. Only confirmed spawns clear their slot; true means the container is now empty,
     * false means the caller must keep the block so the remaining stacks are not deleted with it.
     */
    public boolean dropContents() {
        World world = CraftEngineBlockAccess.getBukkitWorld(this.blockEntity);
        if (world == null) {
            return false;
        }
        boolean allDelivered = contents.dropAll(world, centre(world));
        if (allDelivered) {
            markChanged();
        }
        return allDelivered;
    }

    @Override
    public void onRemove() {
        // Removal that cannot be vetoed: drop what the world accepts, keep the rest as a recovery file.
        if (dropContents()) {
            return;
        }
        archiveUndelivered();
    }

    @Override
    public void saveCustomData(CompoundTag data) {
        synchronized (contents) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                ItemStack stack = contents.get(slot);
                if (stack == null) {
                    continue;
                }
                CompoundTag itemTag = ItemStackUtils.saveBukkitItemAsTag(stack);
                if (itemTag != null) {
                    data.put(SLOT_KEY + slot, itemTag);
                }
            }
        }
    }

    @Override
    public void loadCustomData(CompoundTag data) {
        synchronized (contents) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                CompoundTag itemTag = data.getCompound(SLOT_KEY + slot);
                if (itemTag == null || itemTag.isEmpty()) {
                    continue;
                }
                try {
                    ItemStack stack = ItemStackUtils.parseBukkitItem(itemTag, VersionHelper.WORLD_VERSION);
                    if (stack != null && !stack.isEmpty()) {
                        contents.set(slot, stack);
                    }
                } catch (RuntimeException unreadable) {
                    // A stack this server cannot parse must not stop the rest of the block entity from loading.
                }
            }
        }
    }

    private void archiveUndelivered() {
        List<ItemStack> remaining = contents.remaining();
        if (remaining.isEmpty()) {
            return;
        }
        World world = CraftEngineBlockAccess.getBukkitWorld(this.blockEntity);
        if (world == null) {
            return;
        }
        FDAddonTemplate plugin;
        try {
            plugin = JavaPlugin.getPlugin(FDAddonTemplate.class);
        } catch (IllegalStateException notLoaded) {
            // Chunk unload during shutdown can outlive plugin enablement; there is nobody to archive for then.
            return;
        }
        try {
            CompoundTag data = new CompoundTag();
            int index = 0;
            for (ItemStack stack : remaining) {
                CompoundTag itemTag = ItemStackUtils.saveBukkitItemAsTag(stack);
                if (itemTag != null) {
                    data.put(SLOT_KEY + (index++), itemTag);
                }
            }
            if (data.isEmpty()) {
                return;
            }
            Path file = DropRecoveryArchive.write(
                    plugin.getDataFolder().toPath().resolve("container-drop-recovery"),
                    world.getUID().toString(), world.getName(), this.blockEntity.pos,
                    VersionHelper.WORLD_VERSION, data);
            plugin.getLogger().warning(AddonLang.get("fdaddon.example_container_recovery_saved",
                    "file", file.toAbsolutePath(), "pos", this.blockEntity.pos));
        } catch (IOException | RuntimeException | LinkageError failure) {
            plugin.getLogger().log(Level.SEVERE,
                    AddonLang.get("fdaddon.example_container_recovery_failed", "pos", this.blockEntity.pos), failure);
        }
    }

    private Location centre(World world) {
        BlockPos pos = this.blockEntity.pos;
        return new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
    }

    private void markChanged() {
        if (this.blockEntity.world != null) {
            this.blockEntity.world.blockEntityChanged(this.blockEntity.pos);
        }
    }
}
