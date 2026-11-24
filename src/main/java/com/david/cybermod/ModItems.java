package com.david.cybermod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CyberMod.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CyberMod.MODID);

    // Диски атак (с записью координат)
    public static final RegistryObject<Item> SNIFFER_DISK = ITEMS.register("sniffer_disk", () -> new HackDiskItem(new Item.Properties(), "Сниффер (Шпионаж)"));
    public static final RegistryObject<Item> TROJAN_DISK = ITEMS.register("trojan_disk", () -> new HackDiskItem(new Item.Properties(), "Троян (Кража)"));
    public static final RegistryObject<Item> DDOS_DISK = ITEMS.register("ddos_disk", () -> new HackDiskItem(new Item.Properties(), "DDoS (Блокировка)"));
    public static final RegistryObject<Item> FORKBOMB_DISK = ITEMS.register("forkbomb_disk", () -> new HackDiskItem(new Item.Properties(), "Форк-бомба (Мусор)"));
    public static final RegistryObject<Item> BLACKOUT_DISK = ITEMS.register("blackout_disk", () -> new HackDiskItem(new Item.Properties(), "Блэкаут-Протокол"));

    // Материалы и компоненты
    public static final RegistryObject<Item> CALIBRATED_EYE = ITEMS.register("calibrated_eye", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PAPER_LETTER = ITEMS.register("paper_letter", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CONFETTI = ITEMS.register("confetti", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_BLADE = ITEMS.register("diamond_blade", () -> new Item(new Item.Properties()));

    // Устройства и дроны
    public static final RegistryObject<Item> KILLER_DRONE = ITEMS.register("killer_drone", () -> new KillerDroneItem(new Item.Properties().stacksTo(1)));

    // Модули Сервера защиты
    public static final RegistryObject<Item> ANTIVIRUS_MODULE = ITEMS.register("antivirus_module", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FIREWALL_MODULE = ITEMS.register("firewall_module", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HONEYPOT_MODULE = ITEMS.register("honeypot_module", () -> new Item(new Item.Properties().stacksTo(1)));

    // Экономика: Детектор и Купюры (10$, 50$, 100$)
    public static final RegistryObject<Item> BILL_DETECTOR = ITEMS.register("bill_detector", () -> new BillDetectorItem(new Item.Properties()));
    public static final RegistryObject<Item> DIRTY_BILL_10 = ITEMS.register("dirty_bill_10", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DIRTY_BILL_50 = ITEMS.register("dirty_bill_50", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DIRTY_BILL_100 = ITEMS.register("dirty_bill_100", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLEAN_BILL_10 = ITEMS.register("clean_bill_10", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLEAN_BILL_50 = ITEMS.register("clean_bill_50", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLEAN_BILL_100 = ITEMS.register("clean_bill_100", () -> new Item(new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> CYBER_TAB = CREATIVE_MODE_TABS.register("cyber_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(SNIFFER_DISK.get()))
                    .title(Component.translatable("creativetab.cybermod.cyber_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.HACKER_LAPTOP.get());
                        output.accept(ModBlocks.DEFENSE_SERVER.get());
                        output.accept(ModBlocks.JAMMER_BLOCK.get());
                        output.accept(SNIFFER_DISK.get());
                        output.accept(TROJAN_DISK.get());
                        output.accept(DDOS_DISK.get());
                        output.accept(FORKBOMB_DISK.get());
                        output.accept(BLACKOUT_DISK.get());
                        output.accept(ANTIVIRUS_MODULE.get());
                        output.accept(FIREWALL_MODULE.get());
                        output.accept(HONEYPOT_MODULE.get());
                        output.accept(CALIBRATED_EYE.get());
                        output.accept(PAPER_LETTER.get());
                        output.accept(CONFETTI.get());
                        output.accept(DIAMOND_BLADE.get());
                        output.accept(KILLER_DRONE.get());
                        output.accept(BILL_DETECTOR.get());
                        output.accept(DIRTY_BILL_10.get());
                        output.accept(DIRTY_BILL_50.get());
                        output.accept(DIRTY_BILL_100.get());
                        output.accept(CLEAN_BILL_10.get());
                        output.accept(CLEAN_BILL_50.get());
                        output.accept(CLEAN_BILL_100.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
