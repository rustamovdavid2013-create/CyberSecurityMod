package com.david.cybermod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BillDetectorItem extends Item {

    public BillDetectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Возьмите купюру во вторую руку и нажмите ПКМ для проверки.").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack detector = player.getItemInHand(hand);
        InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack targetItem = player.getItemInHand(otherHand);

        if (!level.isClientSide) {
            if (targetItem.isEmpty()) {
                player.sendSystemMessage(Component.literal("[Детектор] Возьмите купюру во вторую руку!").withStyle(ChatFormatting.YELLOW));
                return InteractionResultHolder.success(detector);
            }

            if (isDirtyBill(targetItem)) {
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0f, 0.5f);
                player.sendSystemMessage(Component.literal("[Детектор] ТРЕВОГА! Обнаружена ГРЯЗНАЯ купюра: " + targetItem.getHoverName().getString()).withStyle(ChatFormatting.RED));
            } else if (isCleanBill(targetItem)) {
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);
                player.sendSystemMessage(Component.literal("[Детектор] ПРОВЕРЕНО: Чистая подлинная купюра (" + targetItem.getHoverName().getString() + ").").withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.literal("[Детектор] Это не купюра.").withStyle(ChatFormatting.GRAY));
            }
        }
        return InteractionResultHolder.sidedSuccess(detector, level.isClientSide);
    }

    public static boolean isDirtyBill(ItemStack stack) {
        return stack.is(ModItems.DIRTY_BILL_10.get())
                || stack.is(ModItems.DIRTY_BILL_50.get())
                || stack.is(ModItems.DIRTY_BILL_100.get());
    }

    public static boolean isCleanBill(ItemStack stack) {
        return stack.is(ModItems.CLEAN_BILL_10.get())
                || stack.is(ModItems.CLEAN_BILL_50.get())
                || stack.is(ModItems.CLEAN_BILL_100.get());
    }
}
