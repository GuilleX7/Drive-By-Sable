package edn.lakeopossmc.drivebysable.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

// --- LOSING STORED NETWORK DATA --- //
// * Shared by the Backup Drive and the Network Anchor
// * Sounds that are played when breaking a block with saved data
public final class CableDataLostSound {

    private static final float VOLUME = 0.7F;

    private CableDataLostSound() {
    }

    public static void play(final Level level, final BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, VOLUME, 1.0F);

        level.playSound(
                null,
                pos,
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.BLOCKS,
                VOLUME,
                0.5F
        );
    }
}