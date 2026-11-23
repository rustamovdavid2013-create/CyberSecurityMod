package com.david.cybermod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HackDiskItem extends Item {
    private final String attackType;

    public HackDiskItem(Properties properties, String attackType) {
        super(properties.stacksTo(1));
        this.attackType = attackType;
    }

    public String getAttackType() {
        return attackType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Тип: " + attackType).withStyle(ChatFormatting.LIGHT_PURPLE));
        BlockPos target = getTargetPos(stack);
        if (target != null) {
            tooltip.add(Component.literal("Цель: X=" + target.getX() + ", Y=" + target.getY() + ", Z=" + target.getZ()).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("Цель не задана (Shift+ПКМ по блоку в радиусе 5 м)").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    // Shift + ПКМ по блоку в радиусе 5 блоков записывает координаты сети в диск
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 25.0) {
                    player.sendSystemMessage(Component.literal("[Диск] Ошибка: радиус захвата координат — не более 5 блоков!").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }

                setTargetPos(context.getItemInHand(), pos);
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.5f);
                player.sendSystemMessage(Component.literal("[Диск] Координаты сети захвачены: X=" + pos.getX() + ", Y=" + pos.getY() + ", Z=" + pos.getZ()).withStyle(ChatFormatting.GREEN));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }

    public static void setTargetPos(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("TargetX", pos.getX());
        tag.putInt("TargetY", pos.getY());
        tag.putInt("TargetZ", pos.getZ());
        tag.putBoolean("HasTarget", true);
    }

    @Nullable
    public static BlockPos getTargetPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean("HasTarget")) {
            return new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        }
        return null;
    }
}
