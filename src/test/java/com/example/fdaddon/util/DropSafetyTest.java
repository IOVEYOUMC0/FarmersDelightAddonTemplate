package com.example.fdaddon.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A refused spawn must be reported as a failure so callers keep their stored copy. */
class DropSafetyTest {

    @Test
    void anInvalidSpawnIsReportedAsFailure() {
        assertFalse(DropSafety.spawnVerified(world(false, false), location(), stack()));
    }

    @Test
    void aValidSpawnIsReportedAsSuccess() {
        assertTrue(DropSafety.spawnVerified(world(true, false), location(), stack()));
    }

    @Test
    void aThrowAfterInsertionWithAValidEntityStillCountsAsDelivered() {
        assertTrue(DropSafety.spawnVerified(world(true, true), location(), stack()));
    }

    @Test
    void aThrowBeforeAnyEntityIsPropagated() {
        assertThrows(IllegalStateException.class,
                () -> DropSafety.spawnVerified(world(false, true), location(), stack()));
    }

    @Test
    void missingInputsAreNeverDelivered() {
        assertFalse(DropSafety.spawnVerified(null, location(), stack()));
        assertFalse(DropSafety.spawnVerified(world(true, false), null, stack()));
        assertFalse(DropSafety.spawnVerified(world(true, false), location(), null));
    }

    private static ItemStack stack() {
        return new StubStack();
    }

    private static Location location() {
        return new Location(null, 0, 64, 0);
    }

    @SuppressWarnings("unchecked")
    private static World world(boolean valid, boolean throwAfterCallback) {
        Item entity = proxy(Item.class, (object, method, args) -> {
            if (method.getName().equals("isValid")) {
                return valid;
            }
            throw new AssertionError(method.getName());
        });
        return proxy(World.class, (object, method, args) -> {
            if (!method.getName().equals("dropItemNaturally")) {
                throw new AssertionError(method.getName());
            }
            ((Consumer<Item>) args[2]).accept(entity);
            if (throwAfterCallback) {
                throw new IllegalStateException("spawn failed after insertion");
            }
            return entity;
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    /** Skips the Bukkit item registry, which a unit test does not have. */
    static final class StubStack extends ItemStack {
        @Override public boolean isEmpty() { return false; }
    }
}
