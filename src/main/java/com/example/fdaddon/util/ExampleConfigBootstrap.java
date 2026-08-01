package com.example.fdaddon.util;

import com.huidu.farmersdelight.api.config.ConfigFileUpdater;
import com.huidu.farmersdelight.api.config.ConfigKeyRename;
import com.huidu.farmersdelight.api.config.ConfigUpdatePolicy;
import com.huidu.farmersdelight.api.config.ConfigUpdateReport;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Brings a config.yml an operator already has in step with the one this build ships. Bukkit's
 * saveDefaultConfig writes the bundled file only when none exists and never adds a key to a file that is
 * already there, so without this every setting you add in a later version stays absent on a server that has
 * been running since before that version, and the feature behind it silently reads its hardcoded fallback.
 *
 * The rename/retire/merge machinery is FarmersDelight's shared api.config — the same code that keeps FD's own
 * config.yml current. All this class supplies is the data: which paths this addon renamed, which it no longer
 * reads, and which sections list content keyed by id rather than fixed settings. Modelled on BrewinAndChewin's
 * production BrewinConfigBootstrap.
 *
 * Hold one instance per plugin and call updateConfig() from onEnable, after saveDefaultConfig() and before
 * reading any config value.
 */
public final class ExampleConfigBootstrap {

    /**
     * This addon's own update data.
     *
     * The two food-effect sections are declared as registry sections: their children are keyed by item id, so
     * an operator deleting an entry means "turn that food off". The merge must not add such entries back one at
     * a time, so a registry section is only recreated when the operator's file has none of it at all.
     *
     * The migrate/retire calls below are illustrative — the shipped config.yml has neither path, and a rename
     * or retirement of a path that is not present is simply skipped. Replace them with your own as the config
     * evolves; a rename must run before the merge so the bundled default cannot occupy the new path first and
     * strand the operator's tuned value under the old name.
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
     * because the same pass that adds settings also deletes values the operator wrote. The api never logs; the
     * report is reported here in this addon's own words.
     */
    public void updateConfig() {
        ConfigUpdateReport report;
        try {
            report = ConfigFileUpdater.updateMainConfig(plugin, POLICY);
        } catch (Exception e) {
            plugin.getLogger().warning("config.yml could not be updated: " + e.getMessage());
            return;
        }
        if (report.backupError() != null) {
            plugin.getLogger().warning("config.yml was not backed up: " + report.backupError());
        }
        for (ConfigKeyRename rename : report.migratedKeys()) {
            plugin.getLogger().info("Moved " + rename.oldPath() + " to " + rename.newPath() + ".");
        }
        if (!report.retiredKeys().isEmpty()) {
            plugin.getLogger().info("Removed unread settings: " + String.join(", ", report.retiredKeys()));
        }
        if (report.addedKeys() > 0) {
            plugin.getLogger().info("Added " + report.addedKeys() + " new settings to config.yml.");
        }
    }
}
