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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class DefenseServerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public DefenseServerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        return new DefenseServerBlockEntity(pos, state);
    }

    public static boolean isServerModule(ItemStack stack) {
        return stack.is(ModItems.ANTIVIRUS_MODULE.get())
                || stack.is(ModItems.FIREWALL_MODULE.get())
                || stack.is(ModItems.HONEYPOT_MODULE.get());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DefenseServerBlockEntity server) {
            ItemStack heldItem = player.getItemInHand(hand);
            boolean isShift = player.isShiftKeyDown();

            // Обычный ПКМ -> Верхний слот (0), Shift + ПКМ -> Нижний слот (1)
            int targetSlot = isShift ? 1 : 0;
            String slotLabel = isShift ? "Нижний слот" : "Верхний слот";

            // 1. Если рука пустая — пытаемся достать модуль из выбранного слота
            if (heldItem.isEmpty()) {
                ItemStack extracted = server.extractModuleFromSlot(targetSlot);
                if (!extracted.isEmpty()) {
                    player.setItemInHand(hand, extracted);
                    level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.BLOCKS, 1.0f, 0.8f);
                    player.sendSystemMessage(Component.literal("[Сервер защиты] Извлечён модуль (" + slotLabel + "): " + extracted.getHoverName().getString()).withStyle(ChatFormatting.YELLOW));
                } else {
                    String s1 = server.getSlot(0).isEmpty() ? "Пусто" : server.getSlot(0).getHoverName().getString();
                    String s2 = server.getSlot(1).isEmpty() ? "Пусто" : server.getSlot(1).getHoverName().getString();
                    player.sendSystemMessage(Component.literal("[Сервер защиты] " + slotLabel + " пуст. | Верх: " + s1 + " | Низ: " + s2).withStyle(ChatFormatting.AQUA));
                }
                return InteractionResult.SUCCESS;
            }

            // 2. Если в руке предмет — проверяем, модуль ли это, и вставляем в выбранный слот
            if (!isServerModule(heldItem)) {
                player.sendSystemMessage(Component.literal("[Сервер защиты] Ошибка: можно вставить только модули защиты (Антивирус, Файрвол, Honeypot)!").withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }

            if (server.insertModuleToSlot(targetSlot, heldItem)) {
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.BLOCKS, 1.0f, 1.2f);
                player.sendSystemMessage(Component.literal("[Сервер защиты] Модуль установлен в: " + slotLabel).withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.literal("[Сервер защиты] " + slotLabel + " уже занят! Сначала освободите его пустой рукой.").withStyle(ChatFormatting.RED));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DefenseServerBlockEntity server) {
            for (int i = 0; i < 2; i++) {
                if (!server.getSlot(i).isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), server.getSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
