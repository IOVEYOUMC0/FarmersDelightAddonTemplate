package com.example.fdaddon.util;

import com.huidu.farmersdelight.api.config.ConfigUpdateNotices;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The template shows the shared way to report a config update: the same notices every plugin in the family
 * logs, rather than a private set of lines a new addon would copy.
 */
class ExampleConfigNoticeTest {

    private static final Path BOOTSTRAP = Path.of("src", "main", "java", "com", "example", "fdaddon", "util",
            "ExampleConfigBootstrap.java");

    @Test
    void theBootstrapLogsThroughTheSharedNotices() throws IOException {
        String flat = Files.readString(BOOTSTRAP).replaceAll("\\s+", " ");

        assertTrue(flat.contains("ConfigUpdateNotices.logAll(plugin, \"config.yml\", report)"),
                "the report goes to the one place that knows the sequence");
        assertFalse(flat.contains("AddonLang.get(\"fdaddon.config_key_migrated\""),
                "the template does not ship its own copy of the four lines");
    }

    @Test
    void theSharedNoticesAreTheLinesTheTemplateUsedToWriteOut() {
        assertEquals(new ConfigUpdateNotices.Notice(true, "plugin.config_backup_failed",
                        List.of("file", "config.yml", "error", "e")),
                ConfigUpdateNotices.backupFailed("config.yml", "e"));
        assertEquals(new ConfigUpdateNotices.Notice(false, "plugin.config_key_migrated",
                        List.of("old", "a", "new", "b")),
                ConfigUpdateNotices.migrated("a", "b"));
        assertEquals(new ConfigUpdateNotices.Notice(false, "plugin.config_keys_retired",
                        List.of("count", 2, "keys", "x, y")),
                ConfigUpdateNotices.retired(List.of("x", "y")));
        assertEquals(new ConfigUpdateNotices.Notice(false, "plugin.config_keys_added",
                        List.of("file", "config.yml", "count", 4)),
                ConfigUpdateNotices.added("config.yml", 4));
    }
}
