package com.david.cybermod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LaunchHackC2SPacket {
    private final BlockPos laptopPos;
    private final int targetX;
    private final int targetY;
    private final int targetZ;

    public LaunchHackC2SPacket(BlockPos laptopPos, int targetX, int targetY, int targetZ) {
        this.laptopPos = laptopPos;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public LaunchHackC2SPacket(FriendlyByteBuf buf) {
        this.laptopPos = buf.readBlockPos();
        this.targetX = buf.readInt();
        this.targetY = buf.readInt();
        this.targetZ = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(laptopPos);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();

            if (level.getBlockEntity(laptopPos) instanceof HackerLaptopBlockEntity laptop) {
                ItemStack disk = laptop.getDisk();
                if (!disk.isEmpty()) {
                    CompoundTag tag = disk.getOrCreateTag();
                    tag.putInt("targetX", targetX);
                    tag.putInt("targetY", targetY);
                    tag.putInt("targetZ", targetZ);
                }
                laptop.executeHack(player);
            }
        });
        return true;
    }
}
