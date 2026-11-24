package com.david.cybermod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class HackerLaptopScreen extends AbstractContainerScreen<HackerLaptopMenu> {
    private EditBox xInput;
    private EditBox yInput;
    private EditBox zInput;

    public HackerLaptopScreen(HackerLaptopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = 82;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Поля ввода координат X, Y, Z
        this.xInput = new EditBox(this.font, x + 16, y + 46, 44, 14, Component.literal("X"));
        this.yInput = new EditBox(this.font, x + 66, y + 46, 44, 14, Component.literal("Y"));
        this.zInput = new EditBox(this.font, x + 116, y + 46, 44, 14, Component.literal("Z"));

        this.xInput.setMaxLength(7);
        this.yInput.setMaxLength(5);
        this.zInput.setMaxLength(7);

        // Если на диске уже записаны координаты — подставляем их в поля
        ItemStack disk = this.menu.blockEntity.getDisk();
        if (!disk.isEmpty() && disk.hasTag() && disk.getTag().contains("targetX")) {
            this.xInput.setValue(String.valueOf(disk.getTag().getInt("targetX")));
            this.yInput.setValue(String.valueOf(disk.getTag().getInt("targetY")));
            this.zInput.setValue(String.valueOf(disk.getTag().getInt("targetZ")));
        }

        this.addRenderableWidget(this.xInput);
        this.addRenderableWidget(this.yInput);
        this.addRenderableWidget(this.zInput);

        // Кнопка запуска атаки
        this.addRenderableWidget(Button.builder(Component.literal("ВЗЛОМАТЬ"), button -> {
            int tx = parseCoord(this.xInput.getValue(), this.menu.blockEntity.getBlockPos().getX());
            int ty = parseCoord(this.yInput.getValue(), this.menu.blockEntity.getBlockPos().getY());
            int tz = parseCoord(this.zInput.getValue(), this.menu.blockEntity.getBlockPos().getZ());

            ModMessages.sendToServer(new LaunchHackC2SPacket(
                    this.menu.blockEntity.getBlockPos(), tx, ty, tz
            ));
            this.onClose();
        }).bounds(x + 48, y + 64, 80, 16).build());
    }

    private int parseCoord(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Тёмный корпус окна и зелёная рамка терминала
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1A1D24);
        graphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF00FF66);

        // Рамка слота под диск (80, 22)
        graphics.fill(x + 79, y + 21, x + 97, y + 39, 0xFF0D0F12);
        graphics.renderOutline(x + 79, y + 21, 18, 18, 0xFF00FF66);

        // Подписи координат
        graphics.drawString(this.font, "X:", x + 8, y + 49, 0x00FF66, false);
        graphics.drawString(this.font, "Y:", x + 58, y + 49, 0x00FF66, false);
        graphics.drawString(this.font, "Z:", x + 108, y + 49, 0x00FF66, false);

        // Отображение энергии RF
        int energy = this.menu.blockEntity.getEnergy();
        int maxEnergy = this.menu.blockEntity.getMaxEnergy();
        graphics.drawString(this.font, energy + " / " + maxEnergy + " RF", x + 8, y + 26, 0x55FFFF, false);

        // Слоты инвентаря игрока
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 7 + col * 18;
                int sy = y + 93 + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF2B303B);
                graphics.renderOutline(sx, sy, 18, 18, 0xFF4A5262);
            }
        }
        for (int col = 0; col < 9; col++) {
            int sx = x + 7 + col * 18;
            int sy = y + 151;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF2B303B);
            graphics.renderOutline(sx, sy, 18, 18, 0xFF4A5262);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Чтобы нажатие 'E' при вводе текста не закрывало окно
        if (this.xInput.isFocused() || this.yInput.isFocused() || this.zInput.isFocused()) {
            if (keyCode == 256) { // Esc
                this.minecraft.player.closeContainer();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
