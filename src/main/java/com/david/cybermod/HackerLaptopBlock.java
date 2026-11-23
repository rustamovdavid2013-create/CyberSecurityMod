package com.david.cybermod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class HackerLaptopBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 2, 15, 1.5, 14),
            Block.box(1, 1.5, 12.5, 15, 11, 14)
    );

    public HackerLaptopBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HackerLaptopBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.HACKER_LAPTOP_BE.get(), HackerLaptopBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HackerLaptopBlockEntity laptop) {
            ItemStack heldItem = player.getItemInHand(hand);
            ItemStack currentDisk = laptop.getDisk();

            if (currentDisk.isEmpty() && isHackDisk(heldItem)) {
                laptop.setDisk(heldItem.split(1));
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.BLOCKS, 1.0f, 1.2f);
                player.sendSystemMessage(Component.literal("[CyberOS] Диск загружен: " + laptop.getDisk().getHoverName().getString()).withStyle(ChatFormatting.GREEN));
            } else if (player.isShiftKeyDown() && !currentDisk.isEmpty()) {
                player.getInventory().placeItemBackInInventory(currentDisk.copy());
                laptop.setDisk(ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.BLOCKS, 1.0f, 0.8f);
                player.sendSystemMessage(Component.literal("[CyberOS] Диск извлечен.").withStyle(ChatFormatting.YELLOW));
            } else {
                String diskName = currentDisk.isEmpty() ? "Нет" : currentDisk.getHoverName().getString();
                player.sendSystemMessage(Component.literal("[CyberOS] Заряд: " + laptop.getEnergy() + " / " + laptop.getMaxEnergy() + " RF | Диск: " + diskName).withStyle(ChatFormatting.AQUA));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean isHackDisk(ItemStack stack) {
        return stack.is(ModItems.SNIFFER_DISK.get())
                || stack.is(ModItems.TROJAN_DISK.get())
                || stack.is(ModItems.DDOS_DISK.get())
                || stack.is(ModItems.FORKBOMB_DISK.get())
                || stack.is(ModItems.BLACKOUT_DISK.get());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof HackerLaptopBlockEntity laptop) {
            if (!laptop.getDisk().isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), laptop.getDisk());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
