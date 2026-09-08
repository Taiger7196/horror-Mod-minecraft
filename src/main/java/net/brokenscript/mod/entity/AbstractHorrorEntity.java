package net.brokenscript.mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Shared base for all Broken Script entities.
 *
 * <p>Horror rules baked in here:</p>
 * <ul>
 *   <li>No despawn while a player is being stalked - they leave on their own terms.</li>
 *   <li>Zero XP; killing them is never "worth it".</li>
 *   <li>Helper for the "vanish when approached" spectator behavior.</li>
 * </ul>
 */
public abstract class AbstractHorrorEntity extends PathfinderMob {

    /**
     * Playtest fix: since {@link #removeWhenFarAway} is false, these entities
     * would otherwise accumulate forever. Every haunting is a visit: after
     * this many ticks (5 min) the entity quietly vanishes on its own terms.
     * The Reaper overrides this with a longer stay.
     */
    protected int maxLifetimeTicks = 20 * 60 * 5;
    private int lifetime = 0;

    protected AbstractHorrorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && ++lifetime > maxLifetimeTicks) {
            vanish();
        }
    }

    /** Nearest surviving player within range, or null. */
    protected ServerPlayer nearestPlayer(double range) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Player player = serverLevel.getNearestPlayer(this, range);
        return player instanceof ServerPlayer serverPlayer && !serverPlayer.isSpectator()
                ? serverPlayer : null;
    }

    /**
     * True when {@code player} is looking roughly at this entity
     * (dot product of view vector against direction-to-entity).
     */
    protected boolean isLookedAtBy(Player player) {
        var toEntity = this.position().subtract(player.getEyePosition()).normalize();
        return player.getViewVector(1.0F).dot(toEntity) > 0.5D;
    }

    /** Discreet exit: smoke, no death animation, no sound. It was never there. */
    protected void vanish() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 12, 0.3D, 0.6D, 0.3D, 0.01D);
        }
        this.discard();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** Immune to environmental noise; only players and the void matter. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() == null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    protected static BlockPos randomOffset(BlockPos origin, net.minecraft.util.RandomSource random,
                                           int horizontal, int vertical) {
        return origin.offset(
                random.nextInt(horizontal * 2 + 1) - horizontal,
                random.nextInt(vertical * 2 + 1) - vertical,
                random.nextInt(horizontal * 2 + 1) - horizontal);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // These things only ever hunt players.
        return target instanceof Player && super.canAttack(target);
    }
}
