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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTracker {
    private DiscordApi discordApi;
    private TextChannel statusChannel;

    private final String BOT_TOKEN = "MTQyMjc1MzE4MDM5NTExMDY0NA.GRrU_0.mYExed8l4jvcT4mLVD6ycGXktEoWIUZCO442hY";
    private final String CHANNEL_ID = "1115846300026548326";

    private Map<String, Long> playerPlayTime = new ConcurrentHashMap<>();
    private Map<String, Long> loginTimes = new ConcurrentHashMap<>();
    private File statsFile;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        loadStats();
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        startDiscordBot();
    }

    @Mod.EventHandler
    public void serverStop(FMLServerStoppingEvent event) {
        for (String playerName : loginTimes.keySet()) {
            updatePlayTime(playerName);
        }
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
                sendOnlineStatus();
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
        loginTimes.put(playerName, System.currentTimeMillis());

        if (!playerPlayTime.containsKey(playerName)) {
            playerPlayTime.put(playerName, 0L);
        }

        sendOnlineStatus();
        saveStats();
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String playerName = event.player.getCommandSenderName();
        updatePlayTime(playerName);

        sendOnlineStatus();
        saveStats();
    }

    private void updatePlayTime(String playerName) {
        if (loginTimes.containsKey(playerName)) {
            long sessionTime = System.currentTimeMillis() - loginTimes.get(playerName);
            long totalTime = playerPlayTime.getOrDefault(playerName, 0L) + sessionTime;
            playerPlayTime.put(playerName, totalTime);
            loginTimes.remove(playerName);
        }
    }

    private void sendOnlineStatus() {
        List<String> onlinePlayers = getOnlinePlayers();
        List<String> offlinePlayers = getOfflinePlayers();

        StringBuilder message = new StringBuilder();

        if (onlinePlayers.isEmpty()) {
            message.append("🔴 **Сервер пуст**\n\n");
        } else {
            message.append("🟢 **Онлайн (").append(onlinePlayers.size()).append("):**\n");
            for (String playerName : onlinePlayers) {
                long totalTime = playerPlayTime.getOrDefault(playerName, 0L);
                long sessionTime = System.currentTimeMillis() - loginTimes.get(playerName);
                message.append("• ").append(playerName)
                    .append(" - ").append(formatTime(totalTime))
                    .append(" (").append(formatTime(sessionTime)).append(" сессия)\n");
            }
            message.append("\n");
        }

        if (!offlinePlayers.isEmpty()) {
            message.append("⚫ **Оффлайн:**\n");
            for (String playerName : offlinePlayers) {
                long totalTime = playerPlayTime.getOrDefault(playerName, 0L);
                message.append("• ").append(playerName).append(" - ").append(formatTime(totalTime)).append("\n");
            }
        }

        sendToDiscord(message.toString());
    }

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        try {
            List<EntityPlayerMP> playerList = MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList;

            for (EntityPlayerMP player : playerList) {
                players.add(player.getCommandSenderName());
            }

            Collections.sort(players);

        } catch (Exception e) {
            System.err.println("Ошибка получения списка игроков: " + e.getMessage());
        }
        return players;
    }

    private List<String> getOfflinePlayers() {
        List<String> offlinePlayers = new ArrayList<>();

        for (String playerName : playerPlayTime.keySet()) {
            if (!loginTimes.containsKey(playerName)) {
                offlinePlayers.add(playerName);
            }
        }

        // Сортируем по алфавиту
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

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%dч %dм", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dм", minutes);
        } else {
            return String.format("%dс", seconds);
        }
    }

    // Сохранение и загрузка статистики
    private void saveStats() {
        try {
            if (statsFile == null) {
                statsFile = new File("world/infomod.dat");
            }

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(statsFile));
            oos.writeObject(new HashMap<>(playerPlayTime));
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
                playerPlayTime = new ConcurrentHashMap<>((Map<String, Long>) ois.readObject());
                ois.close();
                System.out.println("Загружена статистика для " + playerPlayTime.size() + " игроков");
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки статистики: " + e.getMessage());
            playerPlayTime = new ConcurrentHashMap<>();
        }
    }
}
