package net.brokenscript.mod.entity;

import net.brokenscript.mod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * EntityHerobrine - "The Puppeteer".
 *
 * <p>Never direct-attacks at first. His entire kit is manipulation:</p>
 * <ul>
 *   <li>Teleports behind the player the moment they look away.</li>
 *   <li>Swaps hotbar items for corrupted variants (named after broken files).</li>
 *   <li>Plants redstone torches in the player's wake.</li>
 *   <li>Triggers fake explosions - full sound and particles, zero damage.</li>
 * </ul>
 */
public class HerobrineEntity extends AbstractHorrorEntity {
    private static final double STALK_RANGE = 64.0D;
    private static final double BEHIND_DISTANCE = 6.0D;

    private int abilityCooldown = 200;
    private int staredAtTicks = 0;

    public HerobrineEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, STALK_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, (float) STALK_RANGE));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerPlayer victim = nearestPlayer(STALK_RANGE);
        if (victim == null) {
            return;
        }

        // Being stared at for too long is intolerable. He leaves - for now.
        if (isLookedAtBy(victim) && victim.hasLineOfSight(this)) {
            if (++staredAtTicks > 100) {
                vanish();
            }
            return;
        }
        staredAtTicks = 0;

        if (--abilityCooldown > 0) {
            return;
        }
        abilityCooldown = 200 + this.random.nextInt(400);

        switch (this.random.nextInt(4)) {
            case 0 -> teleportBehind(serverLevel, victim);
            case 1 -> corruptHotbarItem(victim);
            case 2 -> placeRedstoneTorchNear(serverLevel, victim);
            default -> fakeExplosion(serverLevel, victim);
        }
    }

    /** Blinks to a point ~6 blocks directly behind the player's back. */
    private void teleportBehind(ServerLevel level, ServerPlayer victim) {
        Vec3 behind = victim.position().subtract(victim.getViewVector(1.0F).scale(BEHIND_DISTANCE));
        BlockPos target = BlockPos.containing(behind);
        // Find solid footing within a couple of blocks vertically.
        for (int dy = 2; dy >= -2; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            if (level.getBlockState(candidate.below()).isSolidRender()
                    && level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()) {
                this.snapTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D,
                        victim.getYRot() + 180.0F, 0.0F);
                this.getLookControl().setLookAt(victim);
                level.playSound(null, candidate, ModSounds.WHISPER.get(), SoundSource.HOSTILE, 0.4F, 0.7F);
                return;
            }
        }
    }

    /** Swaps a random non-empty hotbar stack with a corrupted stand-in. */
    private void corruptHotbarItem(ServerPlayer victim) {
        Inventory inventory = victim.getInventory();
        int slot = this.random.nextInt(9);
        ItemStack current = inventory.getItem(slot);
        if (current.isEmpty() || current.getCount() > 1) {
            return; // only lone items get "edited" - stacks feel too noisy
        }
        ItemStack corrupted = new ItemStack(switch (this.random.nextInt(3)) {
            case 0 -> Items.POISONOUS_POTATO;
            case 1 -> Items.DEAD_BUSH;
            default -> Items.REDSTONE_TORCH;
        });
        corrupted.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal(current.getHoverName().getString())
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.OBFUSCATED));
        inventory.setItem(slot, corrupted);
    }

    /** The classic calling card, left just outside the player's field of view. */
    private void placeRedstoneTorchNear(ServerLevel level, ServerPlayer victim) {
        for (int attempts = 0; attempts < 12; attempts++) {
            BlockPos pos = randomOffset(victim.blockPosition(), this.random, 6, 2);
            Vec3 toPos = Vec3.atCenterOf(pos).subtract(victim.getEyePosition()).normalize();
            boolean behindPlayer = victim.getViewVector(1.0F).dot(toPos) < 0.0D;
            if (behindPlayer && level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.below()).isSolidRender()) {
                level.setBlockAndUpdate(pos, Blocks.REDSTONE_TORCH.defaultBlockState());
                return;
            }
        }
    }

    /** All the drama of TNT with none of the block damage: sound + particles only. */
    private void fakeExplosion(ServerLevel level, ServerPlayer victim) {
        Vec3 at = victim.position().add(this.random.nextGaussian() * 4.0D, 0.0D, this.random.nextGaussian() * 4.0D);
        level.playSound(null, BlockPos.containing(at), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 3.0F, (1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F) * 0.7F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                at.x, at.y + 0.5D, at.z, 24, 1.2D, 0.8D, 1.2D, 0.05D);
    }
}
