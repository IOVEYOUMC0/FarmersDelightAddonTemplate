package com.example.fdaddon.display;

import com.huidu.farmersdelight.api.FarmersDelightApi;
import com.huidu.farmersdelight.api.event.FarmersDelightCollectLiveDisplaysEvent;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows floating items with FarmersDelight's PACKET item-display API. No real Bukkit entity is spawned:
 * FarmersDelight syncs the display to nearby players and removes it on world unload. create, update and
 * remove all use an opaque int HANDLE; a handle of -1 means the display was not created. Keep the handles
 * yourself (there is no look-up), and skip -1.
 *
 * Because these displays are packet-only they do NOT persist, so recreate them on chunk or entity load.
 * And because FarmersDelight does not know they are in use, this class also listens for the /fd cleanup
 * sweep and reports its handles as live, so they are not removed as orphans. Register one instance as a
 * Bukkit listener in onEnable. Every display call must run on the region that owns the location; if you are
 * off-region, wrap the call with FarmersDelightApi.get().runAtLocation(location, runnable).
 */
public final class ExampleItemDisplayManager implements Listener {

    // Your own anchor key (e.g. "worldUid;x;y;z") -> the display handle for that anchor.
    private final Map<String, Integer> handles = new ConcurrentHashMap<>();

    /** Show, or replace, a floating item at a location. Returns false if FarmersDelight is unavailable. */
    public boolean show(String anchor, Location location, ItemStack item) {
        FarmersDelightApi api = FarmersDelightApi.get();
        if (!api.isAvailable() || location == null || item == null) {
            return false;
        }
        hide(anchor); // remove any previous display under this anchor first
        ItemStack visual = item.clone();
        visual.setAmount(1); // a display shows a single item, never a stack count
        int handle = api.createItemDisplay(location, visual,
                ItemDisplay.ItemDisplayTransform.FIXED, transform());
        if (handle == -1) {
            return false;
        }
        handles.put(anchor, handle);
        return true;
    }

    /** Move or re-item an existing display in place, without a remove-and-recreate. No-op if unknown. */
    public boolean update(String anchor, Location location, ItemStack item) {
        Integer handle = handles.get(anchor);
        FarmersDelightApi api = FarmersDelightApi.get();
        if (handle == null || location == null || item == null || !api.isAvailable()) {
            return false;
        }
        ItemStack visual = item.clone();
        visual.setAmount(1);
        return api.updateItemDisplay(handle, location, visual,
                ItemDisplay.ItemDisplayTransform.FIXED, transform());
    }

    /** Remove the display under an anchor. Safe to call for an unknown anchor and safe to double-remove. */
    public void hide(String anchor) {
        Integer handle = handles.remove(anchor);
        if (handle != null) {
            if (!FarmersDelightApi.get().isAvailable()) {
                return;
            }
            FarmersDelightApi.get().removeItemDisplay(handle);
        }
    }

    /** Remove every display this manager created (e.g. on disable). */
    public void hideAll() {
        FarmersDelightApi api = FarmersDelightApi.get();
        if (!api.isAvailable()) {
            handles.clear();
            return;
        }
        for (Integer handle : handles.values()) {
            api.removeItemDisplay(handle);
        }
        handles.clear();
    }

    /**
     * Report our live handles so /fd cleanup's orphan sweep keeps them. Required for any addon-owned packet
     * display; without it the sweep removes them as orphans. Only ADD to the shared set, never clear it.
     */
    @EventHandler
    public void onCollectLiveDisplays(FarmersDelightCollectLiveDisplaysEvent event) {
        event.addLiveIds(List.copyOf(handles.values()));
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
