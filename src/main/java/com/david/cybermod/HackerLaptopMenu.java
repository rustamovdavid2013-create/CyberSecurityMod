package com.david.cybermod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class HackerLaptopMenu extends AbstractContainerMenu {
    public final HackerLaptopBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Клиентский конструктор (вызывается из ModMenuTypes через IContainerFactory)
    public HackerLaptopMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // Серверный конструктор (вызывается из HackerLaptopBlock.use)
    public HackerLaptopMenu(int containerId, Inventory inv, BlockEntity entity) {
        this(containerId, inv, entity, ((HackerLaptopBlockEntity) entity).data);
    }

    public HackerLaptopMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.HACKER_LAPTOP_MENU.get(), containerId);
        this.blockEntity = (HackerLaptopBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        // Автоматическая синхронизация энергии и кулдаунов каждый тик
        addDataSlots(data);

        // Слот для диска атаки (координаты совпадают с рамкой x + 80, y + 22 в HackerLaptopScreen)
        this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(), 0, 80, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof HackDiskItem;
            }
        });

        // Инвентарь игрока (3 ряда по 9 слотов)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }

        // Хотбар игрока (9 слотов)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 152));
        }
    }

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1) > 0 ? this.data.get(1) : 10000;
    }

    public int getCooldownTicks() {
        return this.data.get(2);
    }

    public int getBlackoutTicks() {
        return this.data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index == 0) {
            if (!moveItemStackTo(sourceStack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() instanceof HackDiskItem) {
            if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.HACKER_LAPTOP.get());
    }
}
