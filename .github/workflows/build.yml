package com.david.cybermod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = CyberMod.MODID, name = CyberMod.NAME, version = CyberMod.VERSION)
public class CyberMod {
    public static final String MODID = "cybermod";
    public static final String NAME = "Cyber Security Mod";
    public static final String VERSION = "1.0.0";

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Cyber Security Mod успешно загружен!");
    }
}
