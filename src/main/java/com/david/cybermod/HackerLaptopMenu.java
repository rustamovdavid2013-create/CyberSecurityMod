package com.david.cybermod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HackerLaptopBlockEntity extends BlockEntity {

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(10000, 1000, 2000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
            return received;
        }
    };

    private int cooldownTicks = 0; // Кулдаун ноутбука (смена диска не сбрасывает!)
    private int blackoutTicks = 0; // Отключение от Блэкаута

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    public HackerLaptopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HACKER_LAPTOP_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HackerLaptopBlockEntity entity) {
        if (level.isClientSide) return;

        if (entity.cooldownTicks > 0) {
            entity.cooldownTicks--;
            entity.setChanged();
        }
        if (entity.blackoutTicks > 0) {
            entity.blackoutTicks--;
            entity.setChanged();
        }

        ItemStack disk = entity.itemHandler.getStackInSlot(0);
        if (!disk.isEmpty() && entity.blackoutTicks == 0 && entity.energyStorage.getEnergyStored() >= 200) {
            entity.energyStorage.extractEnergy(200, false);
            entity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    // Главный метод запуска взлома!
    public void executeHack(Player hacker) {
        if (level == null || level.isClientSide) return;

        if (blackoutTicks > 0) {
            hacker.sendSystemMessage(Component.literal("[CyberOS] ОШИБКА: Ноутбук вырублен Блэкаут-Протоколом! Осталось: " + (blackoutTicks / 20) + " сек.").withStyle(ChatFormatting.DARK_RED));
            return;
        }

        if (cooldownTicks > 0) {
            int mins = (cooldownTicks / 20) / 60;
            int secs = (cooldownTicks / 20) % 60;
            hacker.sendSystemMessage(Component.literal("[CyberOS] Система на кулдауне! Осталось: " + mins + " мин. " + secs + " сек.").withStyle(ChatFormatting.RED));
            return;
        }

        ItemStack disk = getDisk();
        if (disk.isEmpty() || !(disk.getItem() instanceof HackDiskItem)) {
            hacker.sendSystemMessage(Component.literal("[CyberOS] Вставьте диск атаки в ноутбук!").withStyle(ChatFormatting.YELLOW));
            return;
        }

        BlockPos target = HackDiskItem.getTargetPos(disk);
        if (target == null) {
            hacker.sendSystemMessage(Component.literal("[CyberOS] На диске не заданы координаты! Зажмите Shift+ПКМ диском по цели.").withStyle(ChatFormatting.YELLOW));
            return;
        }

        if (energyStorage.getEnergyStored() < 2000) {
            hacker.sendSystemMessage(Component.literal("[CyberOS] Недостаточно энергии! Нужно минимум 2000 RF (сейчас: " + energyStorage.getEnergyStored() + " RF).").withStyle(ChatFormatting.RED));
            return;
        }

        // Списываем 2000 RF за запуск атаки
        energyStorage.extractEnergy(2000, false);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

        // 1. Проверка Заглушек сети в радиусе 20 блоков от цели
        for (BlockPos scanPos : BlockPos.betweenClosed(target.offset(-20, -10, -20), target.offset(20, 15, 20))) {
            BlockState scanState = level.getBlockState(scanPos);
            if (scanState.is(ModBlocks.JAMMER_BLOCK.get()) && scanState.getValue(JammerBlock.ACTIVE) && JammerBlock.canOperate(level, scanPos)) {
                level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
                hacker.sendSystemMessage(Component.literal("[CyberOS] ВЗЛОМ ПРОВАЛЕН: Сигнал перехвачен Заглушкой сети и ушел в пустоту! Кулдаун: 20 мин.").withStyle(ChatFormatting.RED));
                cooldownTicks = 20 * 60 * 20; // 20 минут
                return;
            }
        }

        // 2. Проверка Сервера защиты в радиусе 10 блоков от цели
        boolean hasAntivirus = false;
        boolean hasFirewall = false;
        boolean hasHoneypot = false;
        DefenseServerBlockEntity activeServer = null;

        for (BlockPos scanPos : BlockPos.betweenClosed(target.offset(-10, -5, -10), target.offset(10, 5, 10))) {
            if (level.getBlockEntity(scanPos) instanceof DefenseServerBlockEntity server) {
                activeServer = server;
                for (int i = 0; i < 2; i++) {
                    ItemStack mod = server.getSlot(i);
                    if (mod.is(ModItems.ANTIVIRUS_MODULE.get())) hasAntivirus = true;
                    if (mod.is(ModItems.FIREWALL_MODULE.get())) hasFirewall = true;
                    if (mod.is(ModItems.HONEYPOT_MODULE.get())) hasHoneypot = true;
                }
            }
        }

        // 3. Проверка приманки Honeypot
        if (hasHoneypot) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0f, 0.5f);
            hacker.sendSystemMessage(Component.literal("[CyberOS] КРИТИЧЕСКАЯ ТРЕВОГА! Вы взломали приманку Honeypot! Ваши координаты раскрыты цели!").withStyle(ChatFormatting.DARK_RED));
            List<ServerPlayer> nearbyVictims = level.getEntitiesOfClass(ServerPlayer.class, new AABB(target).inflate(30));
            for (ServerPlayer victim : nearbyVictims) {
                victim.sendSystemMessage(Component.literal("[Служба безопасности] ВНИМАНИЕ! Обнаружена попытка взлома приманки! Координаты хакера: X=" + worldPosition.getX() + ", Y=" + worldPosition.getY() + ", Z=" + worldPosition.getZ()).withStyle(ChatFormatting.RED));
            }
            cooldownTicks = 20 * 60 * 20; // 20 минут
            return;
        }

        // 4. Если атака НЕ DDoS и НЕ Блэкаут — проверяем Антивирус
        if (!disk.is(ModItems.DDOS_DISK.get()) && !disk.is(ModItems.BLACKOUT_DISK.get()) && hasAntivirus) {
            level.playSound(null, worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 0.7f);
            hacker.sendSystemMessage(Component.literal("[CyberOS] Атака заблокирована Антивирусом цели! Доступ закрыт на 10 минут.").withStyle(ChatFormatting.RED));
            cooldownTicks = 10 * 60 * 20; // 10 минут
            return;
        }

        // 5. ВЫПОЛНЕНИЕ УСПЕШНОЙ АТАКИ ПО ТИПУ ДИСКА
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.4f);

        if (disk.is(ModItems.SNIFFER_DISK.get())) {
            int foundItems = 0;
            for (BlockPos scanPos : BlockPos.betweenClosed(target.offset(-5, -5, -5), target.offset(5, 5, 5))) {
                if (level.getBlockEntity(scanPos) instanceof Container container) {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        foundItems += container.getItem(i).getCount();
                    }
                }
            }
            hacker.sendSystemMessage(Component.literal("[Сниффер] Отчет получен! В радиусе 5 блоков от цели обнаружено ресурсов: " + foundItems + " шт.").withStyle(ChatFormatting.GREEN));

        } else if (disk.is(ModItems.TROJAN_DISK.get())) {
            if (hasFirewall) {
                hacker.sendSystemMessage(Component.literal("[Троян] ОШИБКА: Файрвол цели заблокировал утечку средств через энергосеть! Кулдаун: 10 мин.").withStyle(ChatFormatting.RED));
                cooldownTicks = 10 * 60 * 20;
                return;
            }
            // Генерируем грязные купюры в зависимости от удачи
            int roll = level.random.nextInt(10);
            ItemStack stolenMoney;
            if (roll < 6) stolenMoney = new ItemStack(ModItems.DIRTY_BILL_10.get(), 1 + level.random.nextInt(3));
            else if (roll < 9) stolenMoney = new ItemStack(ModItems.DIRTY_BILL_50.get(), 1 + level.random.nextInt(2));
            else stolenMoney = new ItemStack(ModItems.DIRTY_BILL_100.get(), 1);

            hacker.getInventory().placeItemBackInInventory(stolenMoney);
