package com.david.cybermod;

import net.minecraft.core.BlockPos;
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
    private final boolean success;

    public LaunchHackC2SPacket(BlockPos laptopPos, int targetX, int targetY, int targetZ, boolean success) {
        this.laptopPos = laptopPos;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.success = success;
    }

    public LaunchHackC2SPacket(FriendlyByteBuf buf) {
        this.laptopPos = buf.readBlockPos();
        this.targetX = buf.readInt();
        this.targetY = buf.readInt();
        this.targetZ = buf.readInt();
        this.success = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(laptopPos);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
        buf.writeBoolean(success);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();

            if (level.getBlockEntity(laptopPos) instanceof HackerLaptopBlockEntity laptop) {
                if (!success) {
                    laptop.failHack(player);
                    return;
                }

                ItemStack disk = laptop.getDisk();
                if (!disk.isEmpty() && disk.getItem() instanceof HackDiskItem) {
                    HackDiskItem.setTargetPos(disk, new BlockPos(targetX, targetY, targetZ));
                }
                laptop.executeHack(player);
            }
        });
        return true;
    }
}
