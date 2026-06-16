package com.example.fdaddon;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Copies this addon's bundled CraftEngine resources (everything under {@code craftengine/<namespace>/} in
 * the jar) into {@code plugins/CraftEngine/resources/<namespace>/} on load. Install-if-missing: existing
 * files are left untouched so server owners can edit them. No-ops on an exploded/IDE run (no jar to scan).
 *
 * <p>Change {@code RESOURCE_PREFIX}/{@code TARGET} to your namespace.
 */
public final class AddonResources {

    private static final String RESOURCE_PREFIX = "craftengine/fdaddon/";
    private static final Path TARGET = Path.of("CraftEngine", "resources", "fdaddon");

    private AddonResources() {
    }

    public static void release(JavaPlugin plugin) {
        Path pluginsFolder = plugin.getDataFolder().toPath().getParent();
        if (pluginsFolder == null) {
            return;
        }
        Path targetRoot = pluginsFolder.resolve(TARGET);
        try {
            URI source = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            Path jar = Path.of(source);
            if (!Files.isRegularFile(jar)) {
                return; // exploded/IDE run
            }
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().startsWith(RESOURCE_PREFIX)) {
                        continue;
                    }
                    Path out = targetRoot.resolve(entry.getName().substring(RESOURCE_PREFIX.length())).normalize();
                    if (!out.startsWith(targetRoot.normalize()) || Files.exists(out)) {
                        continue;
                    }
                    Files.createDirectories(out.getParent());
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to release CraftEngine resources: " + e.getMessage());
        }
    }
}
