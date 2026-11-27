package com.example.fdaddon.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Item slots for a block entity that only clears a slot after its stack is confirmed in the world. Every method
 * is synchronized so an interaction, a chunk save and a removal callback can share the slots.
 */
public final class StoredContents {

    private final ItemStack[] slots;

    public StoredContents(int slotCount) {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount must be positive");
        }
        this.slots = new ItemStack[slotCount];
    }

    public synchronized int slotCount() {
        return slots.length;
    }

    public synchronized boolean isEmpty() {
        for (ItemStack stack : slots) {
            if (!isEmpty(stack)) {
                return false;
            }
        }
        return true;
    }

    /** A copy of the stack in that slot, or null when the slot is empty. */
    public synchronized ItemStack get(int index) {
        ItemStack stack = slots[index];
        return isEmpty(stack) ? null : stack.clone();
    }

    /** Replaces a slot; used while loading persisted content. A null or empty stack clears it. */
    public synchronized void set(int index, ItemStack stack) {
        slots[index] = isEmpty(stack) ? null : stack.clone();
    }

    /** Stores one item of the given stack in the first free slot; false when the container is full. */
    public synchronized boolean store(ItemStack stack) {
        if (isEmpty(stack)) {
            return false;
        }
        for (int index = 0; index < slots.length; index++) {
            if (isEmpty(slots[index])) {
                ItemStack one = stack.clone();
                one.setAmount(1);
                slots[index] = one;
                return true;
            }
        }
        return false;
    }

    /** Removes one item from the highest non-empty slot; null when the container is empty. */
    public synchronized ItemStack takeOne() {
        for (int index = slots.length - 1; index >= 0; index--) {
            ItemStack stack = slots[index];
            if (isEmpty(stack)) {
                continue;
            }
            ItemStack one = stack.clone();
            one.setAmount(1);
            if (stack.getAmount() <= 1) {
                slots[index] = null;
            } else {
                stack.setAmount(stack.getAmount() - 1);
            }
            return one;
        }
        return null;
    }

    /**
     * Drops every slot, clearing only the stacks that the world confirmed. Returns true when the container is
     * empty afterwards; false means at least one stack is still stored and the caller must not delete the block.
     */
    public synchronized boolean dropAll(World world, Location at) {
        boolean allDelivered = true;
        for (int index = 0; index < slots.length; index++) {
            ItemStack stack = slots[index];
            if (isEmpty(stack)) {
                continue;
            }
            if (DropSafety.spawnVerified(world, at, stack)) {
                slots[index] = null;
            } else {
                allDelivered = false;
            }
        }
        return allDelivered;
    }

    /** Copies of the stacks still stored, for a recovery archive. */
    public synchronized List<ItemStack> remaining() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : slots) {
            if (!isEmpty(stack)) {
                out.add(stack.clone());
            }
        }
        return out;
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }
}
