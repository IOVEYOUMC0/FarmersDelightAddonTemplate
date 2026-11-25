package com.example.fdaddon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The template must probe the runtime API before touching compat-ce, and declare CE load BEFORE. */
class TemplateApiProbeTest {

    @Test
    void onLoadProbesBeforeTheDirectCompatCeReference() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/example/fdaddon/FDAddonTemplate.java"));
        int probe = source.indexOf("TemplateApiProbe.supportsCompatCe()");
        int direct = source.indexOf("ContentRegistration.isRegistered");
        assertTrue(probe >= 0, "onLoad must probe the api");
        assertTrue(direct > probe, "the probe must run before the direct compat-ce call");
        assertFalse(Files.exists(Path.of("src/main/resources/plugin.yml")),
                "the template must use paper-plugin.yml only");
    }

    @Test
    void craftEngineIsDeclaredLoadBefore() throws IOException {
        String[] lines = Files.readString(Path.of("src/main/resources/paper-plugin.yml")).split("\\R");
        int craftEngine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals("CraftEngine:")) {
                craftEngine = i;
                break;
            }
        }
        assertTrue(craftEngine >= 0, "CraftEngine dependency must exist");
        boolean before = false;
        for (int i = craftEngine + 1; i < Math.min(lines.length, craftEngine + 8); i++) {
            if (lines[i].trim().equals("load: BEFORE")) {
                before = true;
                break;
            }
        }
        assertTrue(before, "CraftEngine must be declared load: BEFORE");
    }
}
