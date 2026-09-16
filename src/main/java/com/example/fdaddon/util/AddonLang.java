package com.example.fdaddon.util;

import com.huidu.farmersdelight.api.lang.AddonLanguage;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Resolves this addon's console messages, delegating to FarmersDelight's shared implementation.
 *
 * <p>Do not hand-roll this. {@link AddonLanguage} releases lang/*.yml into plugins/&lt;Addon&gt;/lang/
 * so operators can edit a line, honours the {@code language} key in config.yml, inherits the locale
 * FarmersDelight already resolved (so every addon on a server speaks the same language), merges keys
 * missing from an older file, and restores a corrupted one from backup. A private copy gets none of
 * that. The per-addon namespace is the constructor's key prefix, not a separate implementation.
 *
 * <p>Keys are dotted paths ("fdaddon.enabled"); {name} placeholders are filled from alternating
 * key/value args.
 */
public final class AddonLang {

    private static volatile AddonLanguage lang;

    private AddonLang() {
    }

    public static void init(JavaPlugin plugin) {
        AddonLanguage created = new AddonLanguage(plugin, "fdaddon");
        created.init();
        lang = created;
    }

    public static void reload() {
        AddonLanguage current = lang;
        if (current != null) {
            current.reload();
        }
    }

    public static String get(String key, Object... args) {
        AddonLanguage current = lang;
        return current != null ? current.get(key, args) : key;
    }
}
