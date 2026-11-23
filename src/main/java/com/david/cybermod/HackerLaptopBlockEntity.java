package com.david.cybermod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HackerLaptopBlockEntity extends BlockEntity {

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(10000, 1000, 200) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) setChanged();
            return received;
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    public HackerLaptopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HACKER_LAPTOP_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HackerLaptopBlockEntity entity) {
        if (level.isClientSide) return;

        ItemStack disk = entity.itemHandler.getStackInSlot(0);
        if (!disk.isEmpty() && entity.energyStorage.getEnergyStored() >= 200) {
            entity.energyStorage.extractEnergy(200, false);
            entity.setChanged();
        }
    }

    public ItemStack getDisk() {
        return itemHandler.getStackInSlot(0);
    }

    public void setDisk(ItemStack stack) {
        itemHandler.setStackInSlot(0, stack);
    }

    public int getEnergy() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }
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
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        if (tag.contains("energy")) {
            energyStorage.receiveEnergy(tag.getInt("energy"), false);
        }
    }
}
