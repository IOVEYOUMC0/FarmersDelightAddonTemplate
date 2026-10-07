package com.example.fdaddon.util;

import com.huidu.farmersdelight.api.config.ConfigFileUpdater;
import com.huidu.farmersdelight.api.config.ConfigUpdateNotices;
import com.huidu.farmersdelight.api.config.ConfigUpdatePolicy;
import com.huidu.farmersdelight.api.config.ConfigUpdateReport;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Merges bundled defaults into an existing config.yml. Bukkit's saveDefaultConfig creates only missing
 * files and does not add missing keys.
 *
 * FarmersDelight's shared api.config handles renames, retired paths, default merging, and registry sections.
 * This class supplies the addon-specific policy.
 *
 * Hold one instance per plugin and call updateConfig() from onEnable, after saveDefaultConfig() and before
 * reading any config value.
 */
public final class ExampleConfigBootstrap {

    /**
     * Configuration update policy for this addon.
     *
     * The two food-effect sections are declared as registry sections: their children are keyed by item id, so
     * an operator deleting an entry means "turn that food off". The merge must not add such entries back one at
     * a time, so a registry section is only recreated when the operator's file has none of it at all.
     *
     * The migrate/retire calls below are illustrative — the shipped config.yml has neither path, and a rename
     * or retirement of a path that is not present is simply skipped. Replace them with your own as the config
     * evolves; a rename must run before the merge so the bundled default cannot occupy the destination before
     * the operator's value moves from the source path.
     */
    private static final ConfigUpdatePolicy POLICY = ConfigUpdatePolicy.builder()
            .migrate("food-effects.warmth", "food-effects.comfort")
            .retire("legacy.debug-food-effects")
            .registrySection("food-effects.comfort", "food-effects.nourishment")
            .build();

    private final JavaPlugin plugin;

    public ExampleConfigBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Renames the paths that moved, drops the ones nothing reads and merges the keys the operator's file is
     * missing, rewriting the file only when something actually changed. A timestamped copy is kept first,
     * because the same pass that adds settings also deletes values the operator wrote. The report is logged by
     * the family's shared notices, so this addon reads exactly like every other plugin in the family.
     */
    public void updateConfig() {
        ConfigUpdateReport report;
        try {
            report = ConfigFileUpdater.updateMainConfig(plugin, POLICY);
        } catch (Exception e) {
            plugin.getLogger().warning(AddonLang.get("fdaddon.config_update_failed",
                    "error", String.valueOf(e.getMessage())));
            return;
        }
        ConfigUpdateNotices.logAll(plugin, "config.yml", report);
    }
}
