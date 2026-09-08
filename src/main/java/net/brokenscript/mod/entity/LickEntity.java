package net.brokenscript.mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Lick - Green Steve's digger. Stage 3 aggressor that also terraforms:
 * wherever it lingers it carves the "Cursed Cross" - two orthogonal trenches
 * backfilled with netherrack and obsidian (see {@code CursedCrossBuilder}).
 */
public class LickEntity extends AbstractHorrorEntity {
    private int digCooldown = 600;

    public LickEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, false));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel && --digCooldown <= 0) {
            digCooldown = 1200 + this.random.nextInt(1200);
            CursedCrossBuilder.carve(serverLevel, this.blockPosition(), this.random);
        }
    }

    /**
     * Carves cursed crosses: orthogonal trenches lined with netherrack with
     * obsidian at the intersection. Public so the HorrorDirector can also
     * summon crosses near players at stage 3+ without spawning a Lick.
     */
    public static final class CursedCrossBuilder {
        public static void carve(ServerLevel level, BlockPos center, net.minecraft.util.RandomSource random) {
            int armLength = 5 + random.nextInt(4);
            for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                for (int i = 0; i <= armLength; i++) {
                    BlockPos top = center.relative(direction, i);
                    carveColumn(level, top, i == 0, random);
                }
            }
        }

        private static void carveColumn(ServerLevel level, BlockPos pos, boolean isCenter,
                                        net.minecraft.util.RandomSource random) {
            // Find local surface (within a few blocks of the entity's Y).
            BlockPos surface = pos;
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos probe = pos.offset(0, dy, 0);
                if (!level.getBlockState(probe).isAir() && level.getBlockState(probe.above()).isAir()) {
                    surface = probe;
                    break;
                }
            }
            // Trench: 2 deep, floor of netherrack, obsidian heart at the center.
            level.removeBlock(surface.above(), false);
            level.setBlockAndUpdate(surface, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(surface.below(),
                    isCenter ? Blocks.OBSIDIAN.defaultBlockState()
                             : (random.nextFloat() < 0.85F
                                     ? Blocks.NETHERRACK.defaultBlockState()
                                     : Blocks.OBSIDIAN.defaultBlockState()));
        }

        private CursedCrossBuilder() {}
    }
}
