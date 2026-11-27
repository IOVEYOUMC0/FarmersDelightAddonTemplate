package com.example.fdaddon;

import com.example.fdaddon.util.AddonLang;
import com.huidu.farmersdelight.api.compat.ce.CraftEngineBlockAccess;
import com.huidu.farmersdelight.api.util.ItemDelivery;
import com.huidu.farmersdelight.api.util.ProtectionCompat;
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
import org.bukkit.inventory.ItemStack;

/**
 * Interaction for the container example block: right-click with an item stores one, right-click with an empty
 * hand gives one back. The items live in ExampleContainerController and are saved with the chunk.
 */
public final class ExampleContainerBehavior extends BlockBehavior implements EntityBlock {

    private int controllerId;

    private ExampleContainerBehavior(BlockDefinition block) {
        super(block);
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        return new ExampleContainerController(blockEntity);
    }

    @Override
    public void initControllerId(int id) {
        this.controllerId = id;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        return false;
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
        BlockPos pos = context.getClickedPos();
        if (!ProtectionCompat.canUse(player,
                player.getWorld().getBlockAt(pos.x(), pos.y(), pos.z()), FDAddonTemplate.CONTAINER_FLAG)) {
            return InteractionResult.PASS;
        }
        CEWorld ceWorld = CraftEngineBlockAccess.getCEWorld(player.getWorld());
        if (ceWorld == null) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        boolean[] stored = {false};
        boolean[] full = {false};
        ItemStack[] taken = {null};
        blockEntity.controller.let(ExampleContainerController.class, this.controllerId, controller -> {
            if (!held.isEmpty()) {
                if (controller.store(held)) {
                    stored[0] = true;
                } else {
                    full[0] = true;
                }
            } else {
                taken[0] = controller.takeOne();
            }
        });

        if (stored[0]) {
            int remaining = held.getAmount() - 1;
            if (remaining <= 0) {
                player.getInventory().setItemInMainHand(null);
            } else {
                held.setAmount(remaining);
                player.getInventory().setItemInMainHand(held);
            }
            player.sendMessage(AddonLang.get("fdaddon.example_container_stored"));
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        if (full[0]) {
            player.sendMessage(AddonLang.get("fdaddon.example_container_full"));
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        if (taken[0] != null) {
            // Verified hand-back: ItemDelivery checks the inventory-overflow drop instead of assuming it worked.
            ItemDelivery.giveOrDrop(player, taken[0]);
            player.sendMessage(AddonLang.get("fdaddon.example_container_taken"));
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        player.sendMessage(AddonLang.get("fdaddon.example_container_empty"));
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    /** CraftEngine instantiates one behavior per block definition via this factory. */
    public static final BlockBehaviorFactory<ExampleContainerBehavior> FACTORY =
            new BlockBehaviorFactory<ExampleContainerBehavior>() {
                @Override
                public ExampleContainerBehavior create(BlockDefinition block, ConfigSection section) {
                    return new ExampleContainerBehavior(block);
                }
            };
}
