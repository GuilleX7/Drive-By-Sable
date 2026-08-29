package edn.lakeopossmc.drivebysable.blocks;

import com.mojang.serialization.MapCodec;
import edn.lakeopossmc.drivebysable.CableBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

// --- NETWORK ANCHOR --- //
// * A creative only counterpart to the Backup Drive
public class NetworkAnchorBlock extends BaseEntityBlock {

    public static final BooleanProperty STORED = BooleanProperty.create("stored");

    public static final MapCodec<NetworkAnchorBlock> CODEC = simpleCodec(NetworkAnchorBlock::new);

    public NetworkAnchorBlock(final Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(STORED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STORED);
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new NetworkAnchorBlockEntity(pos, state);
    }

    //#region // --- INDESTRUCTIBLE OUTSIDE CREATIVE --- //
    @Override
    protected float getDestroyProgress(
            final BlockState state,
            final Player player,
            final BlockGetter level,
            final BlockPos pos
    ) {
        return player.isCreative() ? super.getDestroyProgress(state, player, level, pos) : 0.0F;
    }

    @Override
    public float getExplosionResistance(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final Explosion explosion
    ) {
        return Float.MAX_VALUE;
    }

    @Override
    public PushReaction getPistonPushReaction(final BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return new ItemStack(this);
    }
    //#endregion

    @Override
    protected void onPlace(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final BlockState oldState,
            final boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level.isClientSide || oldState.is(this)) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof final NetworkAnchorBlockEntity anchor) {
            anchor.onPlaced();
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level,
            final BlockState state,
            final BlockEntityType<T> type
    ) {
        return createTickerHelper(
                type,
                CableBlockEntities.NETWORK_ANCHOR.get(),
                (tickLevel, pos, tickState, anchor) -> anchor.tick()
        );
    }

    //#region // --- CAPTURE ON USE --- //
    @Override
    protected InteractionResult useWithoutItem(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof final NetworkAnchorBlockEntity anchor)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (!anchor.hasStoredSnapshot()) {
                return InteractionResult.CONSUME;
            }

            if (player instanceof final ServerPlayer serverPlayer) {
                anchor.showClearReport(serverPlayer);
            }

            anchor.clearSnapshot();
            return InteractionResult.CONSUME;
        }

        anchor.capture();

        if (player instanceof final ServerPlayer serverPlayer) {
            anchor.showCaptureReport(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }
    //#endregion
}