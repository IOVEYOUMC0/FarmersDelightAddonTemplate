package com.example.fdaddon.display;

import com.huidu.farmersdelight.api.visual.DisplayGroup;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Shows floating items with FarmersDelight's PACKET item-display API. No real Bukkit entity is spawned:
 * FarmersDelight syncs the display to nearby players and removes it on world unload.
 *
 * <p>Everything goes through a DisplayGroup, which holds the handles, replaces a display when the
 * shown item changes, and declares the handles live so the /fd cleanup orphan sweep keeps them. That last
 * part is why a group is worth using rather than the raw createItemDisplay handle: a display nothing
 * declares live is swept on every cleanup and rebuilt right after, so it looks like it flickers back.
 * Using a group means this class needs no FarmersDelightCollectLiveDisplaysEvent listener and is not a
 * Bukkit listener at all.
 *
 * <p>Packet displays do NOT persist, so recreate them on chunk or entity load. Every call must run on the
 * region that owns the location; when off-region wrap it in FarmersDelightApi.get().runAtLocation(...).
 */
public final class ExampleItemDisplayManager {

    private final DisplayGroup group;

    public ExampleItemDisplayManager(Plugin owner) {
        this.group = DisplayGroup.of(owner);
    }

    /**
     * Show, or replace, a floating item at a location. The anchor is your own key for the display -- a
     * block position, a slot index, any value with sane equals and hashCode. Returns false if
     * FarmersDelight is unavailable.
     */
    public boolean show(Object anchor, Location location, ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemStack visual = item.clone();
        visual.setAmount(1); // a display shows a single item, never a stack count
        return this.group.showItem(anchor, location, visual,
                ItemDisplay.ItemDisplayTransform.FIXED, transform()) != -1;
    }

    /** Remove the display under an anchor. Safe for an unknown anchor and safe to double-remove. */
    public void hide(Object anchor) {
        this.group.hide(anchor);
    }

    /** Remove every display this manager created (e.g. on disable). */
    public void hideAll() {
        this.group.hideAll();
    }

    // A no-offset transform: identity rotation and unit scale; the display is positioned by its location.
    // Transformation arg order is (translation, leftRotation, scale, rightRotation), all org.joml types.
    private static Transformation transform() {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
                new Vector3f(1f, 1f, 1f),
                new Quaternionf());
    }
}
