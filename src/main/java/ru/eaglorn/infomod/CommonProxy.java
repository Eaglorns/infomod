package ru.eaglorn.infomod;

import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
    }

    public void init(FMLInitializationEvent event) {
    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    }

    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    }

    public void serverStarting(FMLServerStartingEvent event) {
    }

    public void serverStop(FMLServerStoppingEvent event) {
    }
}
