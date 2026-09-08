package net.brokenscript.mod.entity;

import net.brokenscript.mod.block.PurificationAltarBlock;
import net.brokenscript.mod.util.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Reaper - "The Void Executioner". Stage 4 apex threat.
 *
 * <ul>
 *   <li>Slow, inevitable drift toward the nearest player; ignores pathfinding
 *       and passes through soft (non-obstructing, breakable) blocks.</li>
 *   <li>Damage uses the custom {@code brokenscript:reaper} damage type which is
 *       tagged {@code bypasses_armor} + {@code bypasses_shield} - standard
 *       armor calculations simply do not apply.</li>
 *   <li>Cannot be hurt by players. The only counterplay is Holy Light: within
 *       {@link PurificationAltarBlock#HOLY_RADIUS} of an altar it is repelled
 *       and burned.</li>
 * </ul>
 */
public class TheReaperEntity extends AbstractHorrorEntity {
    private static final double DRIFT_SPEED = 0.06D;
    private static final float TOUCH_DAMAGE = 4.0F;

    public TheReaperEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = false; // toggled per-tick based on surrounding blocks
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D) // it does not walk
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 96.0D);
    }

    @Override
    protected void registerGoals() {
        // No goals - movement is handled manually for the phasing drift.
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer victim = nearestPlayer(96.0D);
        if (victim == null) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // Holy Light check: altars push it back hard and scorch it.
        if (PurificationAltarBlock.isNearAltar(serverLevel, this.blockPosition(),
                PurificationAltarBlock.HOLY_RADIUS)) {
            Vec3 away = this.position().subtract(victim.position()).normalize().scale(0.6D).add(0.0D, 0.2D, 0.0D);
            this.setDeltaMovement(away);
            this.hurtServer(serverLevel, serverLevel.damageSources().inFire(), 5.0F);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 1.2D, this.getZ(), 10, 0.4D, 0.6D, 0.4D, 0.05D);
            return;
        }

        // Phasing: no-clip whenever the surrounding blocks are "soft"
        // (anything a player could break instantly or walk through).
        this.noPhysics = isInsideSoftBlocks(serverLevel);

        Vec3 drift = victim.getEyePosition().subtract(this.position().add(0.0D, 1.2D, 0.0D))
                .normalize().scale(DRIFT_SPEED);
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9D).add(drift));
        this.getLookControl().setLookAt(victim);

        // Touch of the void: armor-bypassing damage on contact.
        if (this.getBoundingBox().inflate(0.4D).intersects(victim.getBoundingBox())) {
            victim.hurtServer(serverLevel, ModDamageTypes.reaper(serverLevel, this), TOUCH_DAMAGE);
        }
    }

    private boolean isInsideSoftBlocks(ServerLevel level) {
        BlockPos pos = this.blockPosition();
        for (BlockPos probe : BlockPos.betweenClosed(pos, pos.above())) {
            var state = level.getBlockState(probe);
            if (!state.isAir() && state.getDestroySpeed(level, probe) >= 0.0F
                    && state.getDestroySpeed(level, probe) <= 1.5F) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Players cannot fight the void; only holy fire (and creative/void) works.
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            level.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.5F);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
