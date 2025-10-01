package ru.eaglorn.infomod;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTracker {
    private DiscordApi discordApi;
    private TextChannel statusChannel;

    private final String BOT_TOKEN = "";
    private final String CHANNEL_ID = "1115846300026548326";

    private final Map<String, Long> loginTimes = new ConcurrentHashMap<>();
    private Map<String, Long> logoutTimes = new ConcurrentHashMap<>();
    private File statsFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        loadStats();
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        startDiscordBot();
        sendOnlineStatus();
    }

    @Mod.EventHandler
    public void serverStop(FMLServerStoppingEvent event) {
        List<String> onlinePlayers = getOnlinePlayers();
        for (String playerName : onlinePlayers) {
            handlePlayerLogout(playerName);
        }

        sendOnlineStatus(true);

        saveStats();
        stopDiscordBot();
    }

    private void startDiscordBot() {
        try {
            discordApi = new DiscordApiBuilder()
                .setToken(BOT_TOKEN)
                .setAllNonPrivilegedIntents()
                .login()
                .join();

            statusChannel = discordApi.getTextChannelById(CHANNEL_ID).orElse(null);
            if (statusChannel != null) {
                System.out.println("Discord бот подключен к каналу: " + statusChannel.getId());
            }

        } catch (Exception e) {
            System.err.println("Ошибка запуска Discord бота: " + e.getMessage());
        }
    }

    private void stopDiscordBot() {
        if (discordApi != null) {
            discordApi.disconnect();
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        String playerName = event.player.getCommandSenderName();
        long loginTime = System.currentTimeMillis();
        loginTimes.put(playerName, loginTime);
        logoutTimes.remove(playerName);

        sendOnlineStatus();
        saveStats();
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String playerName = event.player.getCommandSenderName();
        handlePlayerLogout(playerName);

        sendOnlineStatus();
        saveStats();
    }

    private void handlePlayerLogout(String playerName) {
        if (loginTimes.containsKey(playerName)) {
            long logoutTime = System.currentTimeMillis();
            logoutTimes.put(playerName, logoutTime);
            loginTimes.remove(playerName);
        }
    }

    private void sendOnlineStatus(boolean isServerStopping) {
        List<String> onlinePlayers = getOnlinePlayers();
        List<String> offlinePlayers = getOfflinePlayers();

        StringBuilder message = new StringBuilder();

        if (isServerStopping) {
            message.append("🔴 **Сервер выключается**\n\n");
        } else {
            message.append("🟢 **Сервер включен**\n\n");
        }

        if (!onlinePlayers.isEmpty()) {
            message.append("🟢 **Онлайн (").append(onlinePlayers.size()).append("):**\n");
            for (String playerName : onlinePlayers) {
                long loginTime = loginTimes.get(playerName);
                message.append("• ").append(playerName)
                    .append(" - зашёл в ").append(formatDateTime(loginTime)).append("\n");
            }
            message.append("\n");
        }

        if (!offlinePlayers.isEmpty()) {
            message.append("⚫ **Оффлайн:**\n");
            for (String playerName : offlinePlayers) {
                Long logoutTime = logoutTimes.get(playerName);
                if (logoutTime != null) {
                    message.append("• ").append(playerName)
                        .append(" - вышел в ").append(formatDateTime(logoutTime)).append("\n");
                } else {
                    message.append("• ").append(playerName)
                        .append(" - никогда не заходил\n");
                }
            }
        }

        sendToDiscord(message.toString());
    }

    private void sendOnlineStatus() {
        sendOnlineStatus(false);
    }

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        try {
            List<EntityPlayerMP> playerList = MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList;

            for (EntityPlayerMP player : playerList) {
                String playerName = player.getCommandSenderName();
                players.add(playerName);
                if (!loginTimes.containsKey(playerName)) {
                    loginTimes.put(playerName, System.currentTimeMillis());
                }
            }

            Collections.sort(players);

        } catch (Exception e) {
            System.err.println("Ошибка получения списка игроков: " + e.getMessage());
        }
        return players;
    }

    private List<String> getOfflinePlayers() {

        List<String> offlinePlayers = new ArrayList<>(logoutTimes.keySet());

        Collections.sort(offlinePlayers);

        return offlinePlayers;
    }

    private void sendToDiscord(String message) {
        if (statusChannel != null) {
            try {
                statusChannel.sendMessage(message);
            } catch (Exception e) {
                System.err.println("Ошибка отправки сообщения в Discord: " + e.getMessage());
            }
        }
    }

    private String formatDateTime(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    private void saveStats() {
        try {
            if (statsFile == null) {
                statsFile = new File("world/infomod.dat");
            }

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(statsFile));
            oos.writeObject(new HashMap<>(logoutTimes));
            oos.close();

        } catch (IOException e) {
            System.err.println("Ошибка сохранения статистики: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadStats() {
        try {
            statsFile = new File("world/infomod.dat");
            if (statsFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(statsFile));
                logoutTimes = new ConcurrentHashMap<>((Map<String, Long>) ois.readObject());
                ois.close();
                System.out.println("Загружена статистика для " + logoutTimes.size() + " игроков");
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки статистики: " + e.getMessage());
            logoutTimes = new ConcurrentHashMap<>();
        }
    }
}
