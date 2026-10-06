package com.example.fdaddon;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The template is the shape an addon copies, so its example controller has to show the tick channel CraftEngine's
 * world layer drives — the synchronous {@code createBlockEntityTicker} — and must never declare the asynchronous
 * one, whose thread model is not established on a regionised server.
 */
class ExampleBlockEntityControllerTest {

    private static final Path CONTROLLER =
            Path.of("src", "main", "java", "com", "example", "fdaddon", "ExampleBlockEntityController.java");

    @Test
    void theExampleControllerDeclaresASynchronousTicker() throws Exception {
        assertTrue(Files.isRegularFile(CONTROLLER), CONTROLLER + " has to exist under "
                + Path.of("").toAbsolutePath() + " (user.dir=" + System.getProperty("user.dir") + ")");
        String source = Files.readString(CONTROLLER);
        assertTrue(source.contains("createBlockEntityTicker("),
                "the example has to declare the synchronous ticker CraftEngine's world tick calls");
        assertTrue(source.contains("createTickerHelper("),
                "and build it through createTickerHelper, the shape CraftEngine's own behaviours use");
        assertFalse(source.contains("createAsyncBlockEntityTicker"),
                "the example must never opt into the async channel");
    }
}
