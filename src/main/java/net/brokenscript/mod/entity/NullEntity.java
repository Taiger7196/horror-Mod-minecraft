package net.brokenscript.mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * EntityNull - "The Shadow".
 *
 * <ul>
 *   <li><b>Blindness aura:</b> every player within 15 blocks is blinded.</li>
 *   <li><b>Light dowsing:</b> torches, lanterns and campfires within its radius
 *       are snuffed out dynamically, a few per second, nearest first.</li>
 *   <li><b>Stealth AI:</b> raycasts against the player's view frustum each
 *       repositioning attempt and only moves to points the player cannot see.</li>
 * </ul>
 */
public class NullEntity extends AbstractHorrorEntity {
    public static final double AURA_RADIUS = 15.0D;

    private int dowseCooldown = 0;
    private int repositionCooldown = 0;
    private int exposedTicks = 0;

    public NullEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 66.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        // Intentionally no goals: Null does not walk. Null is simply *there*.
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        applyBlindnessAura(serverLevel);

        if (--dowseCooldown <= 0) {
            dowseNearbyLights(serverLevel);
            dowseCooldown = 15;
        }

        ServerPlayer victim = nearestPlayer(48.0D);
        if (victim == null) {
            return;
        }

        // Stealth: if the player can currently see us, count exposure and relocate.
        if (isLookedAtBy(victim) && hasCleanLineOfSight(victim)) {
            if (++exposedTicks > 30 && --repositionCooldown <= 0) {
                if (relocateOutOfSight(serverLevel, victim)) {
                    exposedTicks = 0;
                }
                repositionCooldown = 40;
            }
        } else {
            exposedTicks = 0;
        }
    }

    private void applyBlindnessAura(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceTo(this) <= AURA_RADIUS) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false));
            }
        }
    }

    /** Kills a handful of nearby player-placed light sources per pulse. */
    private void dowseNearbyLights(ServerLevel level) {
        int radius = (int) AURA_RADIUS;
        int budget = 3;
        BlockPos origin = this.blockPosition();
        for (BlockPos pos : BlockPos.withinManhattan(origin, radius, radius, radius)) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                    || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)) {
                level.removeBlock(pos, false);
            } else if (state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) {
                level.destroyBlock(pos, true); // lanterns drop; someone hung them with care
            } else if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                    && (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))) {
                level.setBlockAndUpdate(pos, state.setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false));
            } else {
                continue;
            }
            if (--budget <= 0) {
                return;
            }
        }
    }

    /** Raycast from the player's eyes to our chest - true when nothing blocks it. */
    private boolean hasCleanLineOfSight(ServerPlayer player) {
        Vec3 from = player.getEyePosition();
        Vec3 to = this.position().add(0.0D, this.getBbHeight() * 0.6D, 0.0D);
        HitResult hit = this.level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    /**
     * Samples candidate positions around the player and teleports to the first
     * one that fails the player's line-of-sight raycast (i.e. is hidden).
     */
    private boolean relocateOutOfSight(ServerLevel level, ServerPlayer victim) {
        for (int attempts = 0; attempts < 16; attempts++) {
            BlockPos candidate = randomOffset(victim.blockPosition(), this.random, 20, 4);
            if (!level.getBlockState(candidate).isAir()
                    || !level.getBlockState(candidate.above()).isAir()
                    || !level.getBlockState(candidate.below()).isSolidRender()) {
                continue;
            }
            Vec3 target = Vec3.atBottomCenterOf(candidate);
            Vec3 eye = target.add(0.0D, this.getBbHeight() * 0.6D, 0.0D);
            HitResult hit = level.clip(new ClipContext(victim.getEyePosition(), eye,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, victim));
            boolean hidden = hit.getType() != HitResult.Type.MISS;
            Vec3 toTarget = target.subtract(victim.getEyePosition()).normalize();
            boolean outsideView = victim.getViewVector(1.0F).dot(toTarget) < 0.25D;
            if (hidden || outsideView) {
                this.snapTo(target.x, target.y, target.z, this.random.nextFloat() * 360.0F, 0.0F);
                return true;
            }
        }
        return false;
    }
}
