package net.brokenscript.mod.block;

import com.mojang.serialization.MapCodec;
import net.brokenscript.mod.network.clientbound.OpenTerminalPayload;
import net.brokenscript.mod.registry.ModItems;
import net.brokenscript.mod.util.TerminalProtocol;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Terminal Block - the end-game quest anchor. Right click with 8 Source
 * Code Fragments in your inventory to boot it; afterwards it opens the CLI
 * screen on the client via {@link OpenTerminalPayload}.
 *
 * <p>All state lives in {@link TerminalBlockEntity}; the block itself is
 * stateless so it survives being moved by fake pistons during stage 4.</p>
 */
public class TerminalBlock extends BaseEntityBlock {
    public static final MapCodec<TerminalBlock> CODEC = simpleCodec(TerminalBlock::new);

    public TerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<TerminalBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TerminalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof TerminalBlockEntity terminal)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // Server side from here on - all world interaction stays on the server thread.
        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (!terminal.isBooted()) {
            int inserted = terminal.tryInsertFragments(serverPlayer);
            if (terminal.getStoredFragments() >= TerminalProtocol.REQUIRED_FRAGMENTS) {
                terminal.boot();
                serverPlayer.sendSystemMessage(Component.literal("> TERMINAL ONLINE. THE SCRIPT AWAITS REPAIR.")
                        .withStyle(ChatFormatting.GREEN));
            } else if (inserted > 0) {
                serverPlayer.sendSystemMessage(Component.literal(
                                "> " + terminal.getStoredFragments() + "/" + TerminalProtocol.REQUIRED_FRAGMENTS
                                        + " SOURCE FRAGMENTS ACCEPTED.")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                serverPlayer.sendSystemMessage(Component.literal(
                                "> INSUFFICIENT DATA. BRING ME "
                                        + (TerminalProtocol.REQUIRED_FRAGMENTS - terminal.getStoredFragments())
                                        + " MORE FRAGMENTS OF " + ModItems.SOURCE_FRAGMENT.get()
                                        .getDefaultInstance().getHoverName().getString().toUpperCase() + ".")
                        .withStyle(ChatFormatting.DARK_RED));
            }
            return InteractionResult.CONSUME;
        }

        // Booted: open the CLI on this player's client.
        PacketDistributor.sendToPlayer(serverPlayer, new OpenTerminalPayload(pos, terminal.getRepairStep()));
        terminal.beginSession(serverPlayer);
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, net.brokenscript.mod.registry.ModBlockEntities.TERMINAL.get(),
                        TerminalBlockEntity::serverTick);
    }
}
