package ru.eaglorn.infomod;

import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;

import java.util.List;

@Mod(modid = InfoMod.MODID, version = Tags.VERSION, name = "InfoMod", acceptedMinecraftVersions = "[1.7.10]", acceptableRemoteVersions = "*")
public class InfoMod {

    public static final String MODID = "infomod";
    public static final Logger LOG = LogManager.getLogger(MODID);

    private PlayerTracker playerTracker;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        InfoMod.LOG.info(Config.greeting);
        InfoMod.LOG.info("I am InfoMod at version " + Tags.VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        playerTracker = new PlayerTracker();
        playerTracker.loadStats();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        playerTracker.startDiscordBot();
        playerTracker.sendOnlineStatus();
    }

    @Mod.EventHandler
    public void serverStop(FMLServerStoppingEvent event) {
        List<String> onlinePlayers = playerTracker.getOnlinePlayers();
        for (String playerName : onlinePlayers) {
            playerTracker.handlePlayerLogout(playerName);
        }
        playerTracker.sendOnlineStatus(true);
        playerTracker.saveStats();
        playerTracker.stopDiscordBot();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        String playerName = event.player.getCommandSenderName();
        long loginTime = System.currentTimeMillis();
        playerTracker.loginTimes.put(playerName, loginTime);
        playerTracker.logoutTimes.remove(playerName);
        playerTracker.sendOnlineStatus();
        playerTracker.saveStats();
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String playerName = event.player.getCommandSenderName();
        playerTracker.handlePlayerLogout(playerName);
        playerTracker.sendOnlineStatus();
        playerTracker.saveStats();
    }
}
