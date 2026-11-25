package com.example.fdaddon.util;

import com.huidu.farmersdelight.api.lang.AddonLanguage;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Resolves this addon's console messages through FarmersDelight's shared language holder.
 *
 * AddonLanguage releases lang/*.yml into plugins/&lt;Addon&gt;/lang/, honours the language key in
 * config.yml, inherits the locale FarmersDelight already resolved (so every addon on a server speaks
 * the same language), merges keys missing from an older file, and restores a corrupted one. A private
 * copy gets none of that. The per-addon namespace is the prefix, not a separate implementation.
 */
public final class AddonLang {

    private static final String KEY_PREFIX = "fdaddon";

    private AddonLang() {
    }

    public static void init(JavaPlugin plugin) {
        AddonLanguage.install(plugin, KEY_PREFIX);
    }

    public static void reload() {
        AddonLanguage.reload(KEY_PREFIX);
    }

    public static void uninstall() {
        AddonLanguage.uninstall(KEY_PREFIX);
    }

    public static String get(String key, Object... args) {
        return AddonLanguage.text(KEY_PREFIX, key, args);
    }
}
