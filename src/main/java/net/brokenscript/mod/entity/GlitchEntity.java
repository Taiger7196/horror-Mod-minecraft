package net.brokenscript.mod.entity;

import net.brokenscript.mod.network.clientbound.GlitchEffectPayload;
import net.brokenscript.mod.registry.ModEntities;
import net.brokenscript.mod.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Entity 303 / Entity 505 - "The Glitch".
 *
 * <p>One class, two registered types:</p>
 * <ul>
 *   <li>{@code brokenscript:entity_505} behaves as a spectator - it lingers at
 *       extreme render distance and {@linkplain #vanish() vanishes} on approach.</li>
 *   <li>{@code brokenscript:entity_303} closes in and fires
 *       {@link GlitchEffectPayload}s at its victim: UI scrambling, temporary
 *       mouse-sensitivity randomization, and (rarely) a fake crash overlay.</li>
 * </ul>
 */
public class GlitchEntity extends AbstractHorrorEntity {
    private static final double SPECTATOR_FLEE_DISTANCE = 40.0D;

    private int glitchCooldown = 100;

    public GlitchEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    public boolean isSpectatorVariant() {
        return this.getType() == ModEntities.ENTITY_505.get();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 128.0F));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerPlayer victim = nearestPlayer(128.0D);
        if (victim == null) {
            return;
        }

        if (isSpectatorVariant()) {
            // Entity 505: pure spectator. Get close and it was never there.
            if (victim.distanceTo(this) < SPECTATOR_FLEE_DISTANCE) {
                serverLevel.playSound(null, this.blockPosition(), ModSounds.STATIC.get(),
                        SoundSource.HOSTILE, 0.6F, 0.5F);
                vanish();
            }
            return;
        }

        // Entity 303: active glitcher.
        if (--glitchCooldown <= 0 && victim.distanceTo(this) < 32.0D) {
            glitchCooldown = 160 + this.random.nextInt(200);
            GlitchEffectPayload.Kind kind = switch (this.random.nextInt(10)) {
                case 0 -> GlitchEffectPayload.Kind.FAKE_CRASH;       // rare, the big one
                case 1, 2, 3 -> GlitchEffectPayload.Kind.MOUSE_SCRAMBLE;
                default -> GlitchEffectPayload.Kind.UI_SCRAMBLE;
            };
            int durationTicks = 60 + this.random.nextInt(100);
            PacketDistributor.sendToPlayer(victim, new GlitchEffectPayload(kind, durationTicks));
            serverLevel.playSound(null, victim.blockPosition(), ModSounds.STATIC.get(),
                    SoundSource.HOSTILE, 0.8F, 1.0F);
        }
    }
}
