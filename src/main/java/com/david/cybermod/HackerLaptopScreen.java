package com.david.cybermod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class HackerLaptopScreen extends AbstractContainerScreen<HackerLaptopMenu> {
    private EditBox xInput;
    private EditBox yInput;
    private EditBox zInput;
    private Button hackButton;

    // Переменные мини-игры на тайминг
    private boolean minigameActive = false;
    private float cursorPos = 0.0f;
    private int cursorDir = 1;
    private float cursorSpeed = 0.02f;
    private float targetStart = 0.35f;
    private float targetWidth = 0.22f;
    private int currentStage = 0;
    private int stagesRequired = 3;

    private int pendingTx;
    private int pendingTy;
    private int pendingTz;

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

        this.xInput = new EditBox(this.font, x + 16, y + 46, 44, 14, Component.literal("X"));
        this.yInput = new EditBox(this.font, x + 66, y + 46, 44, 14, Component.literal("Y"));
        this.zInput = new EditBox(this.font, x + 116, y + 46, 44, 14, Component.literal("Z"));

        this.xInput.setMaxLength(7);
        this.yInput.setMaxLength(5);
        this.zInput.setMaxLength(7);

        // Если на диске уже записаны координаты через Shift+ПКМ — подставляем их в поля
        ItemStack disk = this.menu.blockEntity.getDisk();
        if (!disk.isEmpty() && disk.getItem() instanceof HackDiskItem) {
            BlockPos savedTarget = HackDiskItem.getTargetPos(disk);
            if (savedTarget != null) {
                this.xInput.setValue(String.valueOf(savedTarget.getX()));
                this.yInput.setValue(String.valueOf(savedTarget.getY()));
                this.zInput.setValue(String.valueOf(savedTarget.getZ()));
            }
        }

        this.addRenderableWidget(this.xInput);
        this.addRenderableWidget(this.yInput);
        this.addRenderableWidget(this.zInput);

        this.hackButton = this.addRenderableWidget(Button.builder(Component.literal("ВЗЛОМАТЬ"), button -> {
            if (this.minigameActive) {
                processMinigameHit();
            } else {
                startMinigame();
            }
        }).bounds(x + 48, y + 64, 80, 16).build());
    }

    private void startMinigame() {
        ItemStack disk = this.menu.getSlot(0).getItem();
        if (disk.isEmpty() || !(disk.getItem() instanceof HackDiskItem)) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.literal("[CyberOS] Вставьте диск атаки в слот!"), true);
            }
            return;
        }

        if (this.menu.getEnergy() < 2000) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.literal("[CyberOS] Недостаточно энергии (нужно 2000 RF)!"), true);
            }
            return;
        }

        if (this.menu.getBlackoutTicks() > 0 || this.menu.getCooldownTicks() > 0) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.literal("[CyberOS] Ноутбук на перезарядке или вырублен!"), true);
            }
            return;
        }

        this.pendingTx = parseCoord(this.xInput.getValue(), this.menu.blockEntity.getBlockPos().getX());
        this.pendingTy = parseCoord(this.yInput.getValue(), this.menu.blockEntity.getBlockPos().getY());
        this.pendingTz = parseCoord(this.zInput.getValue(), this.menu.blockEntity.getBlockPos().getZ());

        // Настройка сложности мини-игры в зависимости от диска
        if (disk.is(ModItems.SNIFFER_DISK.get())) {
            this.cursorSpeed = 0.022f;
            this.targetWidth = 0.26f;
            this.stagesRequired = 3;
        } else if (disk.is(ModItems.DDOS_DISK.get()) || disk.is(ModItems.FORKBOMB_DISK.get())) {
            this.cursorSpeed = 0.030f;
            this.targetWidth = 0.20f;
            this.stagesRequired = 4;
        } else if (disk.is(ModItems.TROJAN_DISK.get())) {
            this.cursorSpeed = 0.038f;
            this.targetWidth = 0.16f;
            this.stagesRequired = 4;
        } else if (disk.is(ModItems.BLACKOUT_DISK.get())) {
            this.cursorSpeed = 0.046f;
            this.targetWidth = 0.12f;
            this.stagesRequired = 5;
        } else {
            this.cursorSpeed = 0.025f;
            this.targetWidth = 0.22f;
            this.stagesRequired = 3;
        }

        this.minigameActive = true;
        this.currentStage = 0;
        this.cursorPos = 0.0f;
        this.cursorDir = 1;
        randomizeTargetZone();

        // Снимаем фокус с полей ввода, чтобы Пробел сразу срабатывал для мини-игры
        this.xInput.setFocused(false);
        this.yInput.setFocused(false);
        this.zInput.setFocused(false);
        this.hackButton.setMessage(Component.literal("ПЕРЕХВАТ [SPACE]"));
    }

    private void randomizeTargetZone() {
        this.targetStart = (float) (Math.random() * (1.0f - this.targetWidth));
    }

    private void processMinigameHit() {
        if (!this.minigameActive) return;

        if (this.cursorPos >= this.targetStart && this.cursorPos <= (this.targetStart + this.targetWidth)) {
            this.currentStage++;
            if (this.currentStage >= this.stagesRequired) {
                // Победа! Отправляем пакет успешного взлома
                this.minigameActive = false;
                ModMessages.sendToServer(new LaunchHackC2SPacket(
                        this.menu.blockEntity.getBlockPos(), pendingTx, pendingTy, pendingTz, true
                ));
                this.onClose();
            } else {
                randomizeTargetZone();
            }
        } else {
            // Промах! Отправляем пакет провала (сервер повесит кулдаун 20 минут)
            this.minigameActive = false;
            ModMessages.sendToServer(new LaunchHackC2SPacket(
                    this.menu.blockEntity.getBlockPos(), pendingTx, pendingTy, pendingTz, false
            ));
            this.onClose();
        }
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

        // Основное окно терминала
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1A1D24);
        graphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF00FF66);

        // Рамка слота диска
        graphics.fill(x + 79, y + 21, x + 97, y + 39, 0xFF0D0F12);
        graphics.renderOutline(x + 79, y + 21, 18, 18, 0xFF00FF66);

        // Подписи координат
        graphics.drawString(this.font, "X:", x + 8, y + 49, 0x00FF66, false);
        graphics.drawString(this.font, "Y:", x + 58, y + 49, 0x00FF66, false);
        graphics.drawString(this.font, "Z:", x + 108, y + 49, 0x00FF66, false);

        // Живой счётчик энергии из синхронизированного ContainerData
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        graphics.drawString(this.font, energy + " / " + maxEnergy + " RF", x + 8, y + 26, 0x55FFFF, false);

        // Визуальная полоска энергии вверху справа
        int barW = 60;
        int filledW = (int) ((energy / (float) maxEnergy) * barW);
        graphics.fill(x + 108, y + 26, x + 108 + barW, y + 34, 0xFF0D0F12);
        graphics.fill(x + 108, y + 26, x + 108 + filledW, y + 34, 0xFF00CCFF);
        graphics.renderOutline(x + 108, y + 26, barW, 8, 0xFF00FF66);

        // Отрисовка мини-игры поверх окна, когда она активна
        if (this.minigameActive) {
            float delta = this.minecraft != null ? this.minecraft.getDeltaFrameTime() : 0.5f;
            this.cursorPos += this.cursorDir * this.cursorSpeed * delta;
            if (this.cursorPos >= 1.0f) {
                this.cursorPos = 1.0f;
                this.cursorDir = -1;
            } else if (this.cursorPos <= 0.0f) {
                this.cursorPos = 0.0f;
                this.cursorDir = 1;
            }

            int mgX = x + 18;
            int mgY = y + 36;
            int mgW = 140;
            int mgH = 12;

            // Затемнённая подложка мини-игры
            graphics.fill(x + 10, y + 18, x + 166, y + 60, 0xF00A0C10);
            graphics.renderOutline(x + 10, y + 18, 156, 42, 0xFF00FF66);

            graphics.drawString(this.font, "ВЗЛОМ УЗЛА: " + this.currentStage + "/" + this.stagesRequired + " [SPACE]", mgX, y + 23, 0x00FF66, false);

            // Шкала
            graphics.fill(mgX, mgY, mgX + mgW, mgY + mgH, 0xFF1F242D);
            int zoneX1 = mgX + (int) (this.targetStart * mgW);
            int zoneX2 = mgX + (int) ((this.targetStart + this.targetWidth) * mgW);
            graphics.fill(zoneX1, mgY + 1, zoneX2, mgY + mgH - 1, 0xFF00FF66);

            // Бегунок
            int curX = mgX + Mth.clamp((int) (this.cursorPos * mgW), 0, mgW - 2);
            graphics.fill(curX, mgY - 2, curX + 2, mgY + mgH + 2, 0xFFFFFFFF);
        }

        // Отрисовка слотов инвентаря игрока
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
        if (this.minigameActive && keyCode == 32) { // Пробел (GLFW_KEY_SPACE = 32)
            processMinigameHit();
            return true;
        }
        if (this.xInput.isFocused() || this.yInput.isFocused() || this.zInput.isFocused()) {
            if (keyCode == 256) {
                this.minecraft.player.closeContainer();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
