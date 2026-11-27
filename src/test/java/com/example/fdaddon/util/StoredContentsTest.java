package com.example.fdaddon.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slot handling and the confirmed-drop rule of the container example. */
class StoredContentsTest {

    @Test
    void storingFillsTheFirstFreeSlotWithOneItem() {
        StoredContents contents = new StoredContents(2);

        assertTrue(contents.store(stack(Material.STONE, 5)));
        assertTrue(contents.store(stack(Material.DIRT, 2)));
        assertFalse(contents.store(stack(Material.STONE, 1)), "a full container refuses the third stack");
        assertEquals(1, contents.get(0).getAmount(), "one item per interaction, not the whole held stack");
        assertEquals(2, contents.slotCount());
    }

    @Test
    void takeOneRemovesFromTheHighestFilledSlot() {
        StoredContents contents = new StoredContents(2);
        contents.store(stack(Material.STONE, 1));
        contents.store(stack(Material.DIRT, 1));

        ItemStack taken = contents.takeOne();

        assertNotNull(taken);
        assertEquals(Material.DIRT, taken.getType());
        assertNull(contents.get(1));
        assertTrue(contents.takeOne().getType() == Material.STONE);
        assertNull(contents.takeOne(), "an empty container returns null");
    }

    @Test
    void aRefusedSpawnKeepsItsSlotAndDeliveredSlotsAreCleared() {
        StoredContents contents = new StoredContents(2);
        contents.store(stack(Material.STONE, 1));
        contents.store(stack(Material.DIRT, 1));

        assertFalse(contents.dropAll(world(item -> ((StubStack) item).material != Material.DIRT), location()),
                "the rejected stack must make the caller keep the block");
        assertNull(contents.get(0), "a confirmed delivery is removed");
        assertNotNull(contents.get(1), "a refused spawn keeps its stack");
    }

    @Test
    void aRetryDropsTheStackThatWasKept() {
        StoredContents contents = new StoredContents(1);
        contents.store(stack(Material.DIRT, 1));
        assertFalse(contents.dropAll(world(item -> false), location()));
        assertNotNull(contents.get(0));

        assertTrue(contents.dropAll(world(item -> true), location()));
        assertTrue(contents.isEmpty());
    }

    @Test
    void remainingCopiesTheStoredStacksForAnArchive() {
        StoredContents contents = new StoredContents(2);
        contents.store(stack(Material.STONE, 1));
        contents.store(stack(Material.DIRT, 1));

        assertEquals(2, contents.remaining().size());
        assertTrue(contents.remaining().get(0).getType() == Material.STONE);
    }

    private static ItemStack stack(Material material, int amount) {
        return new StubStack(material, amount);
    }

    private static Location location() {
        return new Location(null, 0, 64, 0);
    }

    @SuppressWarnings("unchecked")
    private static World world(java.util.function.Predicate<ItemStack> accepted) {
        Item entity = proxy(Item.class, (object, method, args) -> {
            if (method.getName().equals("isValid")) {
                return true;
            }
            throw new AssertionError(method.getName());
        });
        return proxy(World.class, (object, method, args) -> {
            if (!method.getName().equals("dropItemNaturally")) {
                throw new AssertionError(method.getName());
            }
            boolean ok = accepted.test((ItemStack) args[1]);
            ((Consumer<Item>) args[2]).accept(entity);
            return ok ? entity : null;
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    /** Skips the Bukkit item registry, which a unit test does not have. */
    static final class StubStack extends ItemStack {
        private final Material material;
        private int amount;

        StubStack(Material material, int amount) {
            super();
            this.material = material;
            this.amount = amount;
        }

        @Override public Material getType() { return material; }
        @Override public int getAmount() { return amount; }
        @Override public void setAmount(int amount) { this.amount = amount; }
        @Override public boolean isEmpty() { return amount <= 0; }
        @Override public ItemStack clone() { return new StubStack(material, amount); }
    }
}
