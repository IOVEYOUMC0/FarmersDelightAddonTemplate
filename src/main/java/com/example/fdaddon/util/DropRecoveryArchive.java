package com.example.fdaddon.util;

import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.momirealms.craftengine.libraries.nbt.Tag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPOutputStream;

/**
 * Writes content that could not be dropped as standard compressed NBT, so an operator can recover it after the
 * block is gone. This is the removal path that cannot veto itself: an explosion or another plugin replacing the
 * block fires no break event the addon could cancel.
 */
public final class DropRecoveryArchive {

    private DropRecoveryArchive() {
    }

    public static Path write(Path directory, String worldId, String worldName, BlockPos pos, int dataVersion,
                             CompoundTag data) throws IOException {
        final CompoundTag root = new CompoundTag();
        root.putString("world_uuid", worldId);
        root.putString("world_name", worldName);
        root.putInt("x", pos.x());
        root.putInt("y", pos.y());
        root.putInt("z", pos.z());
        root.putInt("DataVersion", dataVersion);
        root.putString("format", "fdaddon-container");
        root.put("data", data);

        Files.createDirectories(directory);
        Path pending = Files.createTempFile(directory, "container-", ".tmp");
        String fileName = pending.getFileName().toString();
        Path target = pending.resolveSibling(fileName.substring(0, fileName.length() - 4) + ".nbt");
        try {
            try (var file = Files.newOutputStream(pending);
                 var out = new DataOutputStream(new GZIPOutputStream(file))) {
                out.writeByte(Tag.TAG_COMPOUND_ID);
                out.writeUTF("");
                root.write(out);
            }
            try {
                return Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                return Files.move(pending, target);
            }
        } catch (IOException | RuntimeException | LinkageError failure) {
            try {
                Files.deleteIfExists(pending);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }
}
