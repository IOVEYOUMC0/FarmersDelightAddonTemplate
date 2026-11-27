package com.example.fdaddon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the container example's wiring: a break that cannot drop must be vetoed, an unavoidable removal must
 * archive the rest, and the block definition / registration has to exist. The drop rule itself is tested in
 * StoredContentsTest; this test covers the parts that only run inside a live server.
 */
class ExampleContainerWiringTest {

    @Test
    void theBreakListenerCancelsWhenAControllerKeepsAStack() throws IOException {
        String listener = read("src/main/java/com/example/fdaddon/listener/ExampleContainerBreakListener.java");
        int drop = listener.indexOf("!container.dropContents()");
        int cancel = listener.indexOf("event.setCancelled(true)");
        assertTrue(drop >= 0, "the listener has to ask the controller whether everything dropped");
        assertTrue(cancel > drop, "and cancel the break when it did not");
    }

    @Test
    void anUnavoidableRemovalArchivesTheUndeliveredStacks() throws IOException {
        String controller = read("src/main/java/com/example/fdaddon/ExampleContainerController.java");
        int remove = controller.indexOf("public void onRemove()");
        assertTrue(remove >= 0, "the controller overrides onRemove for explosions and block replacements");
        String body = controller.substring(remove, Math.min(controller.length(), remove + 700));
        assertTrue(body.contains("dropContents()"), "onRemove drops first");
        assertTrue(body.contains("archiveUndelivered()"), "and archives whatever stayed behind");
        assertTrue(controller.contains("DropRecoveryArchive.write("), "the archive writer is used");
        assertFalse(controller.contains("dropItemNaturally("), "drops go through the verified helper");
    }

    @Test
    void persistenceUsesTheCraftEngineItemCodec() throws IOException {
        String controller = read("src/main/java/com/example/fdaddon/ExampleContainerController.java");
        assertTrue(controller.contains("ItemStackUtils.saveBukkitItemAsTag("),
                "saveCustomData serializes through CraftEngine's item codec");
        assertTrue(controller.contains("ItemStackUtils.parseBukkitItem(itemTag, VersionHelper.WORLD_VERSION)"),
                "loadCustomData runs the data fixer with the server's data version");
    }

    @Test
    void theHandBackUsesTheVerifiedDeliveryPath() throws IOException {
        String behavior = read("src/main/java/com/example/fdaddon/ExampleContainerBehavior.java");
        assertTrue(behavior.contains("ItemDelivery.giveOrDrop(player, taken[0])"),
                "taking an item out uses FD's verified overflow delivery");
    }

    @Test
    void theBlockDefinitionAndRegistrationExist() throws IOException {
        String blocks = read("src/main/resources/craftengine/fdaddon/configuration/blocks.yml");
        assertTrue(blocks.contains("fdaddon:example_container:"), "the block definition is shipped");
        assertTrue(blocks.contains("- type: fdaddon:example_container"), "the block references its behavior id");

        String plugin = read("src/main/java/com/example/fdaddon/FDAddonTemplate.java");
        assertTrue(plugin.contains("ExampleContainerBehavior.FACTORY"), "onLoad registers the behavior");
        assertTrue(plugin.contains("new ExampleContainerBreakListener()"), "onEnable registers the break listener");
        assertTrue(plugin.contains("CONTAINER_FLAG"), "the container has its own protection flag");
    }

    private static String read(String relative) throws IOException {
        Path path = Path.of("").toAbsolutePath().resolve(relative);
        assertTrue(Files.isRegularFile(path), relative + " has to exist under " + Path.of("").toAbsolutePath());
        return Files.readString(path);
    }
}
