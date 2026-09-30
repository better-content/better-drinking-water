package com.bettercontent.betterdrinkingwater;

import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fml.ModList;

public final class RainCollectorBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    public static final int CAPACITY = 4;
    public static final int FILL_INTERVAL_TICKS = 30 * 20;

    private static final VoxelShape SHAPE = Shapes.or(
            box(1, 0, 1, 15, 3, 15),
            box(1, 3, 1, 3, 13, 15),
            box(13, 3, 1, 15, 13, 15),
            box(3, 3, 1, 13, 13, 3),
            box(3, 3, 13, 13, 13, 15));

    public RainCollectorBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, FILL_INTERVAL_TICKS);
        }
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        int stored = state.getValue(LEVEL);
        final BlockPos collectionPos = pos.above();
        if (stored < CAPACITY && level.canSeeSky(collectionPos)
                && (level.isRainingAt(collectionPos) || Weather2RainCompat.isPrecipitatingAt(level, collectionPos))) {
            level.setBlock(pos, state.setValue(LEVEL, stored + 1), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, FILL_INTERVAL_TICKS);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
                                 final InteractionHand hand, final BlockHitResult hit) {
        if (state.getValue(LEVEL) <= 0 || !ModList.get().isLoaded("thirst")) {
            return InteractionResult.PASS;
        }

        final ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            final boolean[] drank = {false};
            player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(thirst -> {
                if (thirst.getThirst() < 20 || thirst.getQuenched() < 20) {
                    thirst.drink(player, 3, 2);
                    WaterPurity.givePurityEffects(player, WaterPurity.getBlockPurity(level, pos));
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                            && WaterPurity.getBlockPurity(level, pos) == WaterPurity.MAX_PURITY) {
                        WaterSafetyEpisodes.purifiedDrunk(serverPlayer);
                    }
                    drank[0] = true;
                }
            });
            if (!drank[0]) return InteractionResult.PASS;
            consumeCharge(level, pos, state);
            level.playSound(null, pos, SoundEvents.GENERIC_DRINK, SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.CONSUME;
        }

        final ItemStack filled = WaterPurity.getFilledContainer(held, false);
        if (filled.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        WaterPurity.addPurity(filled, pos, level);
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
        consumeCharge(level, pos, state);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    private static void consumeCharge(final Level level, final BlockPos pos, final BlockState state) {
        level.setBlock(pos, state.setValue(LEVEL, state.getValue(LEVEL) - 1), Block.UPDATE_ALL);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }
}
