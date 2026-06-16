package com.example.fdaddon;

import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Custom-block logic, registered straight into CraftEngine (NOT via FarmersDelight — block/item/recipe
 * definition is CraftEngine's job; FD bridges only gameplay services like cooking-pot recipes & scheduling).
 *
 * <p>Two halves:
 * <ul>
 *   <li>This {@code BlockBehavior} is a SINGLETON shared by every placed block of this id. It holds no
 *       per-block state — it only reacts to interactions and routes to the right block entity.</li>
 *   <li>Per-block state lives in {@link ExampleBlockEntityController}, one per placed block, persisted by
 *       CraftEngine inside the chunk. Implementing {@link EntityBlock} is all it takes for CraftEngine to
 *       create + save/load that controller automatically (no block-entity declaration in blocks.yml).</li>
 * </ul>
 *
 * <p>CraftEngine's classes are a STABLE, non-obfuscated API (it's a separate plugin), so importing
 * {@code net.momirealms.craftengine.**} directly is fine — unlike FarmersDelight internals.
 */
public final class ExampleBlockBehavior extends BlockBehavior implements EntityBlock {

    private int controllerId;

    private ExampleBlockBehavior(BlockDefinition block) {
        super(block);
    }

    // ── EntityBlock: CraftEngine wires these automatically once it sees the behavior implements it ──
    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        return new ExampleBlockEntityController(blockEntity);
    }

    @Override
    public void initControllerId(int id) {
        this.controllerId = id;
    }

    // BlockBehavior declares a few abstract hooks (Object-typed to stay version-agnostic). Satisfy with no-ops.
    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        return false; // mobs won't path through this block
    }

    @Override
    public void fallOn(Object thisBlock, Object[] args) {
    }

    @Override
    public void updateEntityMovementAfterFallOn(Object thisBlock, Object[] args) {
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        Player player = Bukkit.getPlayer(context.getPlayer().uuid());
        if (player == null) {
            return InteractionResult.PASS;
        }
        // Look up the block entity at the clicked position and act on OUR controller. Per-block state never
        // lives on this singleton behavior — it lives in (and persists with) the block entity.
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(player.getWorld().getUID());
        if (ceWorld == null) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null) {
            return InteractionResult.PASS;
        }
        int[] uses = {0};
        // let(class, controllerId, action) resolves OUR controller even if the block stacks several.
        blockEntity.controller.let(ExampleBlockEntityController.class, this.controllerId,
                controller -> { uses[0] = controller.increment(); });
        player.sendMessage("Used this block " + uses[0] + " time(s) — saved in the block entity.");
        return InteractionResult.SUCCESS_AND_CANCEL; // handled; cancel the vanilla interaction
    }

    /** CraftEngine instantiates one behavior per block definition via this factory. */
    public static final BlockBehaviorFactory<ExampleBlockBehavior> FACTORY =
            new BlockBehaviorFactory<ExampleBlockBehavior>() {
                @Override
                public ExampleBlockBehavior create(BlockDefinition block, ConfigSection section) {
                    // `section` is the YAML under `behavior:` — read custom options here if you add any.
                    return new ExampleBlockBehavior(block);
                }
            };
}
