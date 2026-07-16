package com.example.fdaddon.buff;

import com.example.fdaddon.FDAddonTemplate;
import com.huidu.farmersdelight.api.buff.BuffBossbar;
import com.huidu.farmersdelight.api.text.FarmersDelightText;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Pushes the example buff's live state to FarmersDelight's boss-bar channel. FarmersDelight owns the master
 * on/off toggle and the bar's channel and layout in its own config; the addon only pushes a per-player
 * readout keyed by a stable NamespacedKey. update is idempotent per player and key, so call it every tick
 * the buff is active and hide it when it is not. Drive tick() from a repeating task (see the plugin main).
 */
public final class ExampleBuffBossbar {

    private static final NamespacedKey KEY =
            Objects.requireNonNull(NamespacedKey.fromString(FDAddonTemplate.NS + ":example_buff"));

    // The buff's nominal full duration, used to scale the 0..1 bar. A real addon would read this per buff.
    private static final float FULL_SECONDS = 60f;

    private final Plugin plugin;
    private final ExampleCustomBuff buff;

    public ExampleBuffBossbar(Plugin plugin, ExampleCustomBuff buff) {
        this.plugin = plugin;
        this.buff = buff;
    }

    /** Refresh every online player's bar. Cheap no-op when FarmersDelight's boss-bar feature is disabled. */
    public void tick() {
        if (!BuffBossbar.isEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            int remaining = buff.remainingSeconds(player);
            if (remaining <= 0) {
                BuffBossbar.hide(plugin, player, KEY);
                continue;
            }
            // The title mirrors FarmersDelight's own buff bars: a translatable name plus a formatted duration.
            Component title = FarmersDelightText.translatable(buff.nameKey(),
                    FarmersDelightText.formatDuration(remaining));
            float progress = Math.max(0f, Math.min(1f, remaining / FULL_SECONDS));
            BuffBossbar.update(plugin, player, KEY, title, progress,
                    BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        }
    }

    /** Remove this addon's bar from one player immediately (e.g. when the buff is cleared). */
    public void hide(Player player) {
        BuffBossbar.hide(plugin, player, KEY);
    }
}
