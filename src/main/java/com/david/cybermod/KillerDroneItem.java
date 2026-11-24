package com.david.cybermod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KillerDroneItem extends Item {
    public static final int MAX_ENERGY = 10000;

    public KillerDroneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrCreateTag().getInt("DroneEnergy");
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.getOrCreateTag().putInt("DroneEnergy", Math.min(MAX_ENERGY, Math.max(0, energy)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getEnergy(stack) / MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FFFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Заряд: " + getEnergy(stack) + " / " + MAX_ENERGY + " RF").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Shift+ПКМ по Хакерскому ноутбуку — зарядить.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("ПКМ в воздух — запуск (7.5 бл/сек, взрывает Заглушки).").withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player != null && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof HackerLaptopBlockEntity laptop) {
            if (!level.isClientSide) {
                int current = getEnergy(stack);
                int needed = MAX_ENERGY - current;
                if (needed <= 0) {
                    player.sendSystemMessage(Component.literal("[Дрон] Батарея уже заряжена на 100%!").withStyle(ChatFormatting.GREEN));
                    return InteractionResult.SUCCESS;
                }
                int available = laptop.getEnergy();
                int toTransfer = Math.min(needed, available);
                if (toTransfer > 0) {
                    laptop.extractEnergyDirect(toTransfer);
                    setEnergy(stack, current + toTransfer);
                    level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                    player.sendSystemMessage(Component.literal("[Дрон] Заряжено: " + getEnergy(stack) + " / " + MAX_ENERGY + " RF").withStyle(ChatFormatting.AQUA));
                } else {
                    player.sendSystemMessage(Component.literal("[Дрон] В ноутбуке нет энергии!").withStyle(ChatFormatting.RED));
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (!player.isCreative() && getEnergy(stack) < MAX_ENERGY) {
                player.sendSystemMessage(Component.literal("[Дрон] Недостаточно заряда! Нужно 10 000 RF (сейчас: " + getEnergy(stack) + " RF).").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }

            KillerDroneEntity drone = new KillerDroneEntity(ModEntities.KILLER_DRONE.get(), level);
            drone.setPos(player.getX(), player.getEyeY() + 0.5, player.getZ());
            drone.findTarget(player.blockPosition());
            level.addFreshEntity(drone);

            level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.2f);
            player.sendSystemMessage(Component.literal("[Дрон-убийца] Запущен! Скорость 7.5 бл/сек, поиск заглушек...").withStyle(ChatFormatting.GREEN));

            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
