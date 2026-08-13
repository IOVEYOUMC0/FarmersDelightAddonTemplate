package com.example.fdaddon.util;

import net.momirealms.craftengine.core.plugin.config.Config;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal per-addon language resolver so this addon logs its own console lines in the server language
 * without leaning on FarmersDelight's translation files. Bundled lang/en_us.yml + lang/zh_cn.yml are read
 * from the jar on init; the selected locale follows CraftEngine's forced locale, then the JVM locale. Keys
 * are plain dotted paths (e.g. "fdaddon.enabled") and {name} placeholders are filled from alternating
 * key/value args. Copy this class (and the lang files) when you fork the template.
 */
public final class AddonLang {

    private static final String FALLBACK = "en_us";
    private static final Map<String, YamlConfiguration> LOCALES = new HashMap<>();
    private static volatile String selected = FALLBACK;

    private AddonLang() {
    }

    public static void init(JavaPlugin plugin) {
        synchronized (LOCALES) {
            LOCALES.clear();
            for (String name : new String[]{"en_us", "zh_cn"}) {
                try (InputStream in = plugin.getResource("lang/" + name + ".yml")) {
                    if (in == null) {
                        continue;
                    }
                    YamlConfiguration cfg = new YamlConfiguration();
                    cfg.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    LOCALES.put(name, cfg);
                } catch (Exception ignored) {
                }
            }
            selected = selectServerLocale();
        }
    }

    private static String selectServerLocale() {
        try {
            Locale forced = Config.forcedLocale();
            if (forced != null) {
                String matched = match(normalize(forced.toLanguageTag()));
                if (matched != null) {
                    return matched;
                }
            }
        } catch (LinkageError ignored) {
        }
        String matched = match(normalize(Locale.getDefault().toLanguageTag()));
        if (matched != null) {
            return matched;
        }
        return LOCALES.containsKey(FALLBACK) ? FALLBACK : LOCALES.keySet().iterator().next();
    }

    private static String match(String tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        if (LOCALES.containsKey(tag)) {
            return tag;
        }
        String language = tag.split("_")[0];
        for (String installed : LOCALES.keySet()) {
            if (installed.startsWith(language + "_")) {
                return installed;
            }
        }
        return null;
    }

    private static String normalize(String tag) {
        return tag == null ? "" : tag.replace('-', '_').toLowerCase(Locale.ROOT);
    }

    /** Resolve a key in the selected locale, falling back to en_us, filling {name} placeholders. */
    public static String get(String key, Object... args) {
        String message = null;
        synchronized (LOCALES) {
            YamlConfiguration cfg = LOCALES.get(selected);
            if (cfg != null) {
                message = cfg.getString(key);
            }
            if (message == null) {
                YamlConfiguration fallback = LOCALES.get(FALLBACK);
                if (fallback != null) {
                    message = fallback.getString(key);
                }
            }
        }
        if (message == null) {
            return key;
        }
        for (int i = 0; i + 1 < args.length; i += 2) {
            Object name = args[i];
            if (name != null) {
                message = message.replace("{" + name + "}", String.valueOf(args[i + 1]));
            }
        }
        return message;
    }
}
