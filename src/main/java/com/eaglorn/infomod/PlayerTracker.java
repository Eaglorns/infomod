package com.eaglorn.infomod;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;

import java.util.ArrayList;
import java.util.List;

public class PlayerTracker {
    private DiscordApi discordApi;
    private TextChannel statusChannel;
    private String lastStatusMessage = "";

    private final String BOT_TOKEN = "YOUR_DISCORD_BOT_TOKEN";
    private final String CHANNEL_ID = "YOUR_CHANNEL_ID";

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        startDiscordBot();
        sendStatusUpdate("🟢 **Сервер запущен**");
    }

    @Mod.EventHandler
    public void serverStop(FMLServerStoppingEvent event) {
        sendStatusUpdate("🔴 **Сервер остановлен**");
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
                updateOnlineStatus();
            } else {
                System.err.println("Не удалось найти канал с ID: " + CHANNEL_ID);
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

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        try {
            List<EntityPlayerMP> playerList = MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList;

            for (EntityPlayerMP player : playerList) {
                players.add(player.getCommandSenderName());
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения списка игроков: " + e.getMessage());
        }
        return players;
    }

    private void updateOnlineStatus() {
        List<String> onlinePlayers = getOnlinePlayers();
        String message;

        if (onlinePlayers.isEmpty()) {
            message = "🔴 **На сервере никто не играет**";
        } else {
            message = "🟢 **Игроки онлайн (" + onlinePlayers.size() + "):**\n" +
                "```" + String.join("\n", onlinePlayers) + "```";
        }

        if (!message.equals(lastStatusMessage)) {
            sendStatusUpdate(message);
            lastStatusMessage = message;
        }
    }

    private void sendStatusUpdate(String message) {
        if (statusChannel != null) {
            try {
                statusChannel.sendMessage(message);
            } catch (Exception e) {
                System.err.println("Ошибка отправки сообщения в Discord: " + e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        String playerName = event.player.getCommandSenderName();
        sendStatusUpdate("🟢 **" + playerName + " зашел на сервер**");

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                updateOnlineStatus();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String playerName = event.player.getCommandSenderName();
        sendStatusUpdate("🔴 **" + playerName + " вышел с сервера**");

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                updateOnlineStatus();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
