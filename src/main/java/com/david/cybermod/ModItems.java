package com.david.cybermod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CyberMod.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CyberMod.MODID);

    // === 5 ДИСКОВ АТАК (по 1 в стаке) ===
    public static final RegistryObject<Item> SNIFFER_DISK = ITEMS.register("sniffer_disk",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> TROJAN_DISK = ITEMS.register("trojan_disk",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> DDOS_DISK = ITEMS.register("ddos_disk",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> FORKBOMB_DISK = ITEMS.register("forkbomb_disk",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> BLACKOUT_DISK = ITEMS.register("blackout_disk",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // === КОМПОНЕНТЫ ДЛЯ КРАФТОВ ===
    public static final RegistryObject<Item> CALIBRATED_EYE = ITEMS.register("calibrated_eye",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PAPER_LETTER = ITEMS.register("paper_letter",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CONFETTI = ITEMS.register("confetti",
            () -> new Item(new Item.Properties()));

    // === ВКЛАДКА В КРЕАТИВЕ ===
    public static final RegistryObject<CreativeModeTab> CYBER_TAB = CREATIVE_TABS.register("cyber_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.cybermod.cyber_tab"))
                    .icon(() -> new ItemStack(BLACKOUT_DISK.get()))
                    .displayItems((params, output) -> {
                        output.accept(SNIFFER_DISK.get());
                        output.accept(TROJAN_DISK.get());
                        output.accept(DDOS_DISK.get());
                        output.accept(FORKBOMB_DISK.get());
                        output.accept(BLACKOUT_DISK.get());
                        output.accept(CALIBRATED_EYE.get());
                        output.accept(PAPER_LETTER.get());
                        output.accept(CONFETTI.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_TABS.register(eventBus);
    }
}
