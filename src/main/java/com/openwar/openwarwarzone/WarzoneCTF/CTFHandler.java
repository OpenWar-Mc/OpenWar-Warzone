package com.openwar.openwarwarzone.WarzoneCTF;

import com.openwar.openwarfaction.factions.Faction;
import com.openwar.openwarfaction.factions.FactionManager;
import com.openwar.openwarwarzone.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class CTFHandler implements Listener {

    private final Zone zone;
    private final FactionCaptureManager manager;
    private final FactionManager factionManager;
    private final Main main;
    private final Set<Player> playersInZone = new HashSet<>();

    private static final int TICK_INTERVAL = 20;

    public CTFHandler(Zone zone, FactionCaptureManager manager, FactionManager factionManager, Main main) {
        this.zone = zone;
        this.manager = manager;
        this.factionManager = factionManager;
        this.main = main;

        startCaptureTask();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        boolean isInZone = zone.isPlayerInRegion(player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                2774D, 56D, 3085D, 2759D, 46D, 3115D);

        //if (isInZone && !playersInZone.contains(player)) {
        //    playersInZone.add(player);
        //    manager.handlePlayerEnter(player, factionManager);
        //    sendActionBar(player, "§aYou are on the capture zone !");
        //} else if (!isInZone && playersInZone.contains(player)) {
        //    playersInZone.remove(player);
        //    manager.handlePlayerExit(player, factionManager);
        //    sendActionBar(player, "§cYou are leaving the capture zone !");
        //}
    }

    private void startCaptureTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (playersInZone.isEmpty()) {
                    zone.resetCapture();
                    return;
                }
                if (!canStartCapture()) {
                    zone.resetCapture();
                    return;
                }
                manager.handleCaptureTick();
                broadcastCaptureProgress();
            }
        }.runTaskTimer(main, 0, TICK_INTERVAL);
    }

    private boolean canStartCapture() {
        if (Bukkit.getOnlinePlayers().size() < 3) return false;

        long factionCount = playersInZone.stream()
                .map(player -> factionManager.getFactionByPlayer(player.getUniqueId()))
                .filter(faction -> faction != null)
                .map(Faction::getName)
                .distinct()
                .count();

        return factionCount >= 2;
    }

    private void broadcastCaptureProgress() {
        String currentFaction = zone.getCurrentFaction();
        int progress = zone.getProgress();

        String message;
        if (currentFaction == null) {
            message = "§8» §7This zone is neutral. Progression : §b" + progress + "§7%";
        } else {
            message = "§8» §bLeading Faction : §c" + currentFaction + " §7| Progression : §b" + progress + "§7%";
        }

        for (Player player : playersInZone) {
            sendActionBar(player, message);
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
