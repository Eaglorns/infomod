package ru.eaglorn.infomod;

import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;

@Mod(modid = InfoMod.MODID, version = Tags.VERSION, name = "InfoMod", acceptedMinecraftVersions = "[1.7.10]", acceptableRemoteVersions = "*")
public class InfoMod {

    public static final String MODID = "infomod";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(serverSide = "com.eaglorn.infomod.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);

    }

    @Mod.EventHandler
    public void serverStop(FMLServerStoppingEvent event) {
        proxy.serverStop(event);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        proxy.onPlayerLogin(event);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        proxy.onPlayerLogout(event);
    }
}
