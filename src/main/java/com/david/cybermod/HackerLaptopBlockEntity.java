package com.david.cybermod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
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
            }
            return received;
        }
    };

    private int cooldownTicks = 0; // Кулдаун ноутбука (смена диска не сбрасывает!)
    private int blackoutTicks = 0; // Отключение от Блэкаута
    private int activeDrainTicks = 0; // Оставшиеся тики активного потребления 200 RF/t при взломе

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    // Синхронизация данных с GUI (обновляется каждый тик)
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                case 2 -> cooldownTicks;
                case 3 -> blackoutTicks;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

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

        // Расход 200 RF/t во время активной фазы взлома
        if (entity.activeDrainTicks > 0 && entity.blackoutTicks == 0) {
            if (entity.energyStorage.getEnergyStored() >= 200) {
                entity.energyStorage.extractEnergy(200, false);
                entity.activeDrainTicks--;
                entity.setChanged();
            } else {
                entity.activeDrainTicks = 0;
            }
        }
    }

    // Вызывается при провале мини-игры
    public void failHack(Player hacker) {
        if (level == null || level.isClientSide) return;
        if (blackoutTicks > 0 || cooldownTicks > 0) return;

        energyStorage.extractEnergy(500, false);
        cooldownTicks = 20 * 60 * 20; // 20 минут
        setChanged();

        level.playSound(null, worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 0.6f);
        hacker.sendSystemMessage(Component.literal("[CyberOS] СБОЙ СИНХРОНИЗАЦИИ! Взлом провален. Кулдаун: 20 мин.")
                .withStyle(ChatFormatting.RED));
    }

    // Главный метод запуска взлома после успешной мини-игры!
    public void executeHack(Player hacker) {
        if (level == null || level.isClientSide) return;

        if (blackoutTicks > 0) {
            hacker.sendSystemMessage(Component.literal("[CyberOS] ОШИБКА: Ноутбук вырублен Блэкаут-Протоколом! Осталось: " + (blackoutTicks / 20) + " сек.")
                    .withStyle(ChatFormatting.DARK_RED));
            return;
        }

        if (cooldownTicks > 0) {
            int mins = (cooldownTicks / 20) / 60;
            int secs = (cooldownTicks / 20) % 60;
            hacker.sendSystemMessage(Component.literal("[CyberOS] Система на кулдауне! Осталось: " + mins + " мин. " + secs + " сек.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        ItemStack disk = getDisk();
        if (disk.isEmpty() || !(disk.getItem() instanceof HackDiskItem)) {
            hacker.sendSystemMessage(Component.literal("[CyberOS] Вставьте диск атаки в ноутбук!")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        BlockPos target = HackDiskItem.getTargetPos(disk);
        if (target == null) {
            hacker.sendSystemMessage(Component.literal("[CyberOS] На диске не заданы координаты! Зажмите Shift+ПКМ диском по цели.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        if (energyStorage.getEnergyStored() < 2000) {
            hacker.sendSystemMessage(Component.literal("[CyberOS] Недостаточно энергии! Нужно минимум 2000 RF (сейчас: " + energyStorage.getEnergyStored() + " RF).")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Списываем 2000 RF за атаку и запускаем 10 тиков пиковой нагрузки (200 RF/t)
        energyStorage.extractEnergy(2000, false);
        this.activeDrainTicks = 10;
        setChanged();

        // 1. Проверка Заглушек сети в радиусе 20 блоков от цели
        for (BlockPos scanPos : BlockPos.betweenClosed(target.offset(-20, -10, -20), target.offset(20, 15, 20))) {
            BlockState scanState = level.getBlockState(scanPos);
            if (scanState.is(ModBlocks.JAMMER_BLOCK.get()) && scanState.getValue(JammerBlock.ACTIVE) && JammerBlock.canOperate(level, scanPos)) {
                level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
                hacker.sendSystemMessage(Component.literal("[CyberOS] ВЗЛОМ ПРОВАЛЕН: Сигнал перехвачен Заглушкой сети и ушел в пустоту! Кулдаун: 20 мин.")
                        .withStyle(ChatFormatting.RED));
                cooldownTicks = 20 * 60 * 20;
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
            hacker.sendSystemMessage(Component.literal("[CyberOS] КРИТИЧЕСКАЯ ТРЕВОГА! Вы взломали приманку Honeypot! Ваши координаты раскрыты цели!")
                    .withStyle(ChatFormatting.DARK_RED));
            List<ServerPlayer> nearbyVictims = level.getEntitiesOfClass(ServerPlayer.class, new AABB(target).inflate(30));
            for (ServerPlayer victim : nearbyVictims) {
                victim.sendSystemMessage(Component.literal("[Служба безопасности] ВНИМАНИЕ! Обнаружена попытка взлома приманки! Координаты хакера: X="
                        + worldPosition.getX() + ", Y=" + worldPosition.getY() + ", Z=" + worldPosition.getZ())
                        .withStyle(ChatFormatting.RED));
            }
            cooldownTicks = 20 * 60 * 20;
            return;
        }

        // 4. Если атака НЕ DDoS и НЕ Блэкаут — проверяем Антивирус
        if (!disk.is(ModItems.DDOS_DISK.get()) && !disk.is(ModItems.BLACKOUT_DISK.get()) && hasAntivirus) {
            level.playSound(null, worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 0.7f);
            hacker.sendSystemMessage(Component.literal("[CyberOS] Атака заблокирована Антивирусом цели! Доступ закрыт на 10 минут.")
                    .withStyle(ChatFormatting.RED));
            cooldownTicks = 10 * 60 * 20;
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
            hacker.sendSystemMessage(Component.literal("[Сниффер] Отчет получен! В радиусе 5 блоков от цели обнаружено ресурсов: " + foundItems + " шт.")
                    .withStyle(ChatFormatting.GREEN));

        } else if (disk.is(ModItems.TROJAN_DISK.get())) {
            if (hasFirewall) {
                hacker.sendSystemMessage(Component.literal("[Троян] ОШИБКА: Файрвол цели заблокировал утечку средств через энергосеть! Кулдаун: 10 мин.")
                        .withStyle(ChatFormatting.RED));
                cooldownTicks = 10 * 60 * 20;
                return;
            }

            int stolen10 = 0;
            int stolen50 = 0;
            int stolen100 = 0;

            // Сканируем все хранилища (сундуки и модовые инвентари) в радиусе 5 блоков от цели
            for (BlockPos scanPos : BlockPos.betweenClosed(target.offset(-5, -5, -5), target.offset(5, 5, 5))) {
                if (scanPos.equals(worldPosition)) continue;
                BlockEntity be = level.getBlockEntity(scanPos);
                if (be == null) continue;

                LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
                if (cap.isPresent()) {
                    IItemHandler handler = cap.orElseThrow(IllegalStateException::new);
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        ItemStack stack = handler.getStackInSlot(slot);
                        if (stack.isEmpty()) continue;

                        if (stack.is(ModItems.CLEAN_BILL_10.get())) {
                            ItemStack extracted = handler.extractItem(slot, stack.getCount(), false);
                            stolen10 += extracted.getCount();
                        } else if (stack.is(ModItems.CLEAN_BILL_50.get())) {
                            ItemStack extracted = handler.extractItem(slot, stack.getCount(), false);
                            stolen50 += extracted.getCount();
                        } else if (stack.is(ModItems.CLEAN_BILL_100.get())) {
                            ItemStack extracted = handler.extractItem(slot, stack.getCount(), false);
                            stolen100 += extracted.getCount();
                        }
                    }
                } else if (be instanceof Container container) {
                    for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        ItemStack stack = container.getItem(slot);
                        if (stack.isEmpty()) continue;

                        if (stack.is(ModItems.CLEAN_BILL_10.get())) {
                            stolen10 += stack.getCount();
                            container.setItem(slot, ItemStack.EMPTY);
                        } else if (stack.is(ModItems.CLEAN_BILL_50.get())) {
                            stolen50 += stack.getCount();
                            container.setItem(slot, ItemStack.EMPTY);
                        } else if (stack.is(ModItems.CLEAN_BILL_100.get())) {
                            stolen100 += stack.getCount();
                            container.setItem(slot, ItemStack.EMPTY);
                        }
                    }
                    container.setChanged();
                }
            }

            int totalStolen = stolen10 + stolen50 + stolen100;
            if (totalStolen == 0) {
                hacker.sendSystemMessage(Component.literal("[Троян] Взлом выполнен, но в хранилищах цели (радиус 5 блоков) не найдено чистых купюр.")
                        .withStyle(ChatFormatting.YELLOW));
                return;
            }

            // Половина остаётся чистой, половина портится (становится грязной)
            distributeStolenBills(hacker, stolen10, ModItems.CLEAN_BILL_10.get(), ModItems.DIRTY_BILL_10.get());
            distributeStolenBills(hacker, stolen50, ModItems.CLEAN_BILL_50.get(), ModItems.DIRTY_BILL_50.get());
            distributeStolenBills(hacker, stolen100, ModItems.CLEAN_BILL_100.get(), ModItems.DIRTY_BILL_100.get());

            int totalValue = stolen10 * 10 + stolen50 * 50 + stolen100 * 100;
            hacker.sendSystemMessage(Component.literal("[Троян] Успешная кража! Изъято купюр: " + totalStolen + " шт. (на сумму $" + totalValue + "). Половина повреждена при передаче!")
                    .withStyle(ChatFormatting.GREEN));

        } else if (disk.is(ModItems.DDOS_DISK.get())) {
            if (activeServer != null) {
                for (int i = 0; i < 2; i++) {
                    ItemStack mod = activeServer.getSlot(i);
                    if (mod.is(ModItems.ANTIVIRUS_MODULE.get())) {
                        activeServer.extractModule();
                    }
                }
                hacker.sendSystemMessage(Component.literal("[DDoS] Сервер защиты цели перегружен! Антивирус отключен!")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                hacker.sendSystemMessage(Component.literal("[DDoS] Удар нанесен, но активных Серверов защиты рядом с целью не найдено.")
                        .withStyle(ChatFormatting.YELLOW));
            }

        } else if (disk.is(ModItems.FORKBOMB_DISK.get())) {
            List<ServerPlayer> victims = level.getEntitiesOfClass(ServerPlayer.class, new AABB(target).inflate(8));
            for (ServerPlayer victim : victims) {
                for (int i = 0; i < 9; i++) {
                    ItemStack spam = new ItemStack(ModItems.CONFETTI.get(), 64);
                    spam.setHoverName(Component.literal("FORK_BOMB_PROCESS_0x99").withStyle(ChatFormatting.OBFUSCATED));
                    victim.getInventory().add(spam);
                }
                victim.sendSystemMessage(Component.literal("[ВНИМАНИЕ] Ваша система атакована Форк-бомбой!")
                        .withStyle(ChatFormatting.RED));
            }
            hacker.sendSystemMessage(Component.literal("[Форк-бомба] Инвентарь целей в зоне поражения забит спам-процессами!")
                    .withStyle(ChatFormatting.GREEN));

        } else if (disk.is(ModItems.BLACKOUT_DISK.get())) {
            for (BlockPos scanPos : BlockPos.betweenClosed(target.offset(-15, -10, -15), target.offset(15, 10, 15))) {
                if (level.getBlockEntity(scanPos) instanceof HackerLaptopBlockEntity victimLaptop && !scanPos.equals(worldPosition)) {
                    victimLaptop.setBlackout(20 * 150);
                }
            }
            hacker.sendSystemMessage(Component.literal("[БЛЭКАУТ-ПРОТОКОЛ] Все ПК и системы в радиусе 15 блоков вырублены на 2.5 минуты! Кулдаун: 1 игровой день.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            cooldownTicks = 24000;
        }
    }

    private void distributeStolenBills(Player hacker, int totalCount, Item cleanBill, Item dirtyBill) {
        if (totalCount <= 0) return;
        int cleanCount = totalCount / 2;
        int dirtyCount = totalCount - cleanCount;

        if (cleanCount > 0) {
            hacker.getInventory().placeItemBackInInventory(new ItemStack(cleanBill, cleanCount));
        }
        if (dirtyCount > 0) {
            hacker.getInventory().placeItemBackInInventory(new ItemStack(dirtyBill, dirtyCount));
        }
    }

    public void setBlackout(int ticks) {
        this.blackoutTicks = ticks;
        setChanged();
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }
    public ItemStack getDisk() { return itemHandler.getStackInSlot(0); }
    public void setDisk(ItemStack stack) { itemHandler.setStackInSlot(0, stack); }
    public int getEnergy() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergy() { return energyStorage.getMaxEnergyStored(); }
    public void extractEnergyDirect(int amount) { energyStorage.extractEnergy(amount, false); setChanged(); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("cooldown", cooldownTicks);
        tag.putInt("blackout", blackoutTicks);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        if (tag.contains("energy")) {
            int savedEnergy = tag.getInt("energy");
            while (energyStorage.getEnergyStored() < savedEnergy) {
                int added = energyStorage.receiveEnergy(savedEnergy - energyStorage.getEnergyStored(), false);
                if (added <= 0) break;
            }
        }
        cooldownTicks = tag.getInt("cooldown");
        blackoutTicks = tag.getInt("blackout");
    }
}
