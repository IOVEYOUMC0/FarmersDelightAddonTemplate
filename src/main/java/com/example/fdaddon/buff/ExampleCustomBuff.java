package com.example.fdaddon.buff;

import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.buff.CustomBuff;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A self-contained custom buff. FarmersDelight cannot host addon-specific effects itself, so an addon
 * implements this contract and registers it with CustomBuffRegistry. FarmersDelight then drives three
 * things for the buff without knowing the addon's manager: its milk-bottle / milk-bucket consume listener
 * may clear it (clearOne / clearAll), its per-player persistence lifecycle calls saveState on quit and
 * restoreState on join, and the buff can be applied generically by id via CustomBuffRegistry.apply.
 *
 * Only id, isActive and remove are required; the rest have sensible defaults. All methods run on the target
 * player's region thread, so the per-player state map is a ConcurrentHashMap.
 *
 * This example keeps level plus an expiry wall-clock second per player entirely in memory, and persists
 * only the REMAINING seconds to the player's data container, never an absolute tick, so a restart cannot
 * inflate or zero the duration. restoreState is gap-filling: it re-applies only when the buff is not already
 * active, so a cross-server data-sync plugin that restored a newer state is not overwritten.
 */
public final class ExampleCustomBuff implements CustomBuff {

    public static final String ID = FDAddonTemplate.NS + ":example_buff";

    // PDC keys. Use fromString (never new NamespacedKey(String, String), which Paper flags as internal);
    // the namespace must equal the plugin name lowercase for the stored data to match across restarts.
    private static final NamespacedKey PDC_LEVEL =
            Objects.requireNonNull(NamespacedKey.fromString(FDAddonTemplate.NS + ":example_buff_level"));
    private static final NamespacedKey PDC_REMAINING =
            Objects.requireNonNull(NamespacedKey.fromString(FDAddonTemplate.NS + ":example_buff_remaining"));

    private record State(int level, long expiryEpochSeconds) {
    }

    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean apply(Player player, int level, int durationSeconds) {
        if (player == null || level <= 0 || durationSeconds <= 0) {
            return false;
        }
        states.put(player.getUniqueId(), new State(level, nowSeconds() + durationSeconds));
        return true;
    }

    @Override
    public boolean isActive(Player player) {
        return remainingSeconds(player) > 0;
    }

    @Override
    public void remove(Player player) {
        states.remove(player.getUniqueId());
    }

    @Override
    public int level(Player player) {
        State state = states.get(player.getUniqueId());
        return state != null && state.expiryEpochSeconds() > nowSeconds() ? state.level() : 0;
    }

    @Override
    public int remainingSeconds(Player player) {
        State state = states.get(player.getUniqueId());
        if (state == null) {
            return 0;
        }
        long remaining = state.expiryEpochSeconds() - nowSeconds();
        return remaining > 0 ? (int) remaining : 0;
    }

    @Override
    public String nameKey() {
        // Translation key rendered on the boss bar; add it to your resourcepack lang JSON / translations.
        return "buff.fdaddon.example";
    }

    @Override
    public void saveState(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        int remaining = remainingSeconds(player);
        if (remaining <= 0) {
            pdc.remove(PDC_LEVEL);
            pdc.remove(PDC_REMAINING);
            return;
        }
        pdc.set(PDC_LEVEL, PersistentDataType.INTEGER, level(player));
        pdc.set(PDC_REMAINING, PersistentDataType.INTEGER, remaining);
    }

    @Override
    public void restoreState(Player player) {
        if (isActive(player)) {
            return; // gap-filling: never overwrite a state already present (e.g. from a data-sync plugin)
        }
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Integer level = pdc.get(PDC_LEVEL, PersistentDataType.INTEGER);
        Integer remaining = pdc.get(PDC_REMAINING, PersistentDataType.INTEGER);
        if (level != null && remaining != null && level > 0 && remaining > 0) {
            apply(player, level, remaining);
        }
    }

    private static long nowSeconds() {
        return System.currentTimeMillis() / 1000L;
    }
}
