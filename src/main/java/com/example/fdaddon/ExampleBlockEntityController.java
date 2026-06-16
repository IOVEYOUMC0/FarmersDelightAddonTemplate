package com.example.fdaddon;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;

/**
 * Per-block content, stored IN the CraftEngine block entity — NOT in a side file. CraftEngine creates one
 * controller per placed block (via {@link ExampleBlockBehavior#createBlockEntityController}) and persists
 * whatever you write in {@link #saveCustomData} together with the chunk; {@link #loadCustomData} restores
 * it on load. This is exactly how FarmersDelight's cooking pot / skillet / stove keep their state — no
 * separate yml, no manual save/load lifecycle. (A plugin-managed yml only exists in FD as legacy migration.)
 *
 * <p>Here we store a single use-counter. In a real block store an inventory, fluid level, owner, etc. —
 * CraftEngine's {@code CompoundTag} (and {@code ListTag}, {@code ItemStackUtils}) serialize all of it.
 */
public final class ExampleBlockEntityController extends BlockEntityController {

    private static final String USES = "uses";

    private int uses;

    public ExampleBlockEntityController(BlockEntity blockEntity) {
        super(blockEntity);
    }

    /** Bump the counter and tell CraftEngine this block entity changed so it gets re-saved with the chunk. */
    public int increment() {
        this.uses++;
        if (this.blockEntity.world != null) {
            this.blockEntity.world.blockEntityChanged(this.blockEntity.pos);
        }
        return this.uses;
    }

    // Persistence: write to / read from the block entity's own NBT. CraftEngine handles when (chunk save)
    // and where (in the chunk data). Namespace your keys if your block has multiple controllers.
    @Override
    public void saveCustomData(CompoundTag data) {
        data.putInt(USES, this.uses);
    }

    @Override
    public void loadCustomData(CompoundTag data) {
        this.uses = data.getInt(USES, 0);
    }
}
