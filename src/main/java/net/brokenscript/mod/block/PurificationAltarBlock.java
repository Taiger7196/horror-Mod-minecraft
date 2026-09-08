package net.brokenscript.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Purification Altar - a source of "Holy Light". It is the only thing
 * that can repel The Reaper (see {@code TheReaperEntity#hurtServer} and its
 * altar-repulsion tick) and it slowly drains the world infection level while
 * players stand near it (see {@code HorrorDirector}).
 */
public class PurificationAltarBlock extends Block {
    /** Radius in which the altar's holy light protects and purifies. */
    public static final int HOLY_RADIUS = 10;

    public PurificationAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Gentle rising motes of light, client-side only.
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.0D + random.nextDouble() * 0.4D,
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.03D + random.nextDouble() * 0.02D, 0.0D);
        }
    }

    /** True if a purification altar exists within {@code radius} of {@code center}. */
    public static boolean isNearAltar(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).getBlock() instanceof PurificationAltarBlock) {
                return true;
            }
        }
        return false;
    }
}
