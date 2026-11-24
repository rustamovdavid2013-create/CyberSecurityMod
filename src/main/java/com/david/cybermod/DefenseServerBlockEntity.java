package com.david.cybermod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefenseServerBlockEntity extends BlockEntity {

    // 2 слота под модули: 0 = Верхний слот, 1 = Нижний слот
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public DefenseServerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEFENSE_SERVER_BE.get(), pos, state);
    }

    public ItemStack getSlot(int index) {
        return itemHandler.getStackInSlot(index);
    }

    public boolean insertModuleToSlot(int slot, ItemStack heldItem) {
        if (slot < 0 || slot >= 2) return false;
        if (itemHandler.getStackInSlot(slot).isEmpty()) {
            itemHandler.setStackInSlot(slot, heldItem.split(1));
            return true;
        }
        return false;
    }

    public ItemStack extractModuleFromSlot(int slot) {
        if (slot < 0 || slot >= 2) return ItemStack.EMPTY;
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    // Метод для DDoS-атаки из ноутбука (выбивает первый найденный модуль)
    public ItemStack extractModule() {
        for (int i = 1; i >= 0; i--) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }
}
