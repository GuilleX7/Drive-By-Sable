package edn.lakeopossmc.drivebysable.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// --- CLIENT SIDE OF THE LOAD HIGHLIGHT PACKET --- //
public final class BackupDriveHighlightClient {

    private BackupDriveHighlightClient() {
    }

    public static void show(final BlockPos drivePos, final List<BlockPos> sources, final List<String> modules) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        final Map<BlockPos, Set<String>> restored = new LinkedHashMap<>();
        final int count = Math.min(sources.size(), modules.size());

        for (int index = 0; index < count; index++) {
            restored.computeIfAbsent(sources.get(index), ignored -> new LinkedHashSet<>())
                    .add(modules.get(index));
        }

        BackupDriveLoadHighlight.show(minecraft.level, drivePos, restored);
    }
}