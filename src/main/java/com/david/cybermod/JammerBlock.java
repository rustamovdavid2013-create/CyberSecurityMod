package com.david.cybermod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class JammerBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final int MIN_SURFACE_Y = 62; // Минимальная высота работы (уровень моря)

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 4, 13),
            Block.box(7, 4, 7, 9, 16, 9)
    );

    public JammerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    // Проверка: стоит ли заглушка достаточно высоко и под открытым небом
    public static boolean canOperate(Level level, BlockPos pos) {
        return pos.getY() >= MIN_SURFACE_Y && level.canSeeSky(pos.above());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean isCurrentlyActive = state.getValue(ACTIVE);

            if (!isCurrentlyActive) {
                if (pos.getY() < MIN_SURFACE_Y) {
                    level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 0.8f);
                    player.sendSystemMessage(Component.literal("[Заглушка] Ошибка: слишком низко (Y=" + pos.getY() + ")! Установите на поверхности (выше Y=" + MIN_SURFACE_Y + ").").withStyle(ChatFormatting.RED));
                    return InteractionResult.SUCCESS;
                }
                if (!level.canSeeSky(pos.above())) {
                    level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 0.8f);
                    player.sendSystemMessage(Component.literal("[Заглушка] Ошибка: антенна перекрыта крышей! Нужен открытый выход в небо.").withStyle(ChatFormatting.RED));
                    return InteractionResult.SUCCESS;
                }

                level.setBlock(pos, state.setValue(ACTIVE, true), 3);
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.4f);
                player.sendSystemMessage(Component.literal("[Заглушка] Включена! Все атаки в радиусе 20 блоков перенаправляются в пустоту.").withStyle(ChatFormatting.GREEN));
            } else {
                level.setBlock(pos, state.setValue(ACTIVE, false), 3);
                level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8f, 0.8f);
                player.sendSystemMessage(Component.literal("[Заглушка] Отключена.").withStyle(ChatFormatting.YELLOW));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && canOperate(level, pos) && random.nextInt(3) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 1.05;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0, 0.05, 0.0);
        }
    }
}
