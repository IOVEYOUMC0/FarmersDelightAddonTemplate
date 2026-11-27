package com.example.fdaddon.util;

import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.momirealms.craftengine.libraries.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The recovery archive has to be a standard compressed NBT file an operator can open. */
class DropRecoveryArchiveTest {

    @TempDir
    Path temp;

    @Test
    void theArchiveRoundTripsTheContainerPayload() throws IOException {
        CompoundTag data = new CompoundTag();
        CompoundTag slot = new CompoundTag();
        slot.putInt("count", 3);
        slot.putString("id", "minecraft:stone");
        data.put("slot_0", slot);

        Path file = DropRecoveryArchive.write(temp, "world-uuid", "world", new BlockPos(12, 70, -34), 4321, data);

        assertTrue(Files.size(file) > 0);
        try (var raw = Files.newInputStream(file);
             var in = new DataInputStream(new GZIPInputStream(raw))) {
            assertEquals(Tag.TAG_COMPOUND_ID, in.readByte());
            assertEquals("", in.readUTF());
            CompoundTag root = (CompoundTag) new CompoundTag().getType().read(in, 0);
            assertEquals("world-uuid", root.getString("world_uuid"));
            assertEquals("world", root.getString("world_name"));
            assertEquals(12, root.getInt("x"));
            assertEquals(70, root.getInt("y"));
            assertEquals(-34, root.getInt("z"));
            assertEquals(4321, root.getInt("DataVersion"));
            assertEquals("fdaddon-container", root.getString("format"));
            assertEquals(data, root.getCompound("data"));
        }
    }
}
