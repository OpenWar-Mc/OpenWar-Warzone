package com.openwar.openwarwarzone.WarzoneCTF;

import com.openwar.openwarfaction.factions.Faction;
import com.openwar.openwarfaction.factions.FactionManager;
import com.openwar.openwarwarzone.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
    private BossBar bossBar = Bukkit.createBossBar("§7Building Neutral", BarColor.WHITE, BarStyle.SOLID);

    private static final int TICK_INTERVAL = 20;

    public CTFHandler(Zone zone, FactionCaptureManager manager, FactionManager factionManager, Main main) {
        this.zone = zone;
        this.manager = manager;
        this.factionManager = factionManager;
        this.main = main;
        loopBossBar();

        startCaptureTask();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equals("warzone")) {
            boolean isInZone = zone.isPlayerInRegion(player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ(),
                    2774D, 56D, 3085D, 2759D, 46D, 3115D);

            if (isInZone && !playersInZone.contains(player)) {
                playersInZone.add(player);
                manager.handlePlayerEnter(player, factionManager);
                sendActionBar(player, "§8» §aYou are on the capture zone !");
            } else if (!isInZone && playersInZone.contains(player)) {
                playersInZone.remove(player);
                manager.handlePlayerExit(player, factionManager);
                sendActionBar(player, "§8» §cYou are leaving the capture zone !");
            }
        }
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
                    zone.pauseCapture();
                    return;
                }

                manager.handleCaptureTick();
                broadcastCaptureProgress();
            }
        }.runTaskTimer(main, 0, TICK_INTERVAL);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (zone.getCurrentFaction() != null) {
                    boolean factionMemberPresent = playersInZone.stream()
                            .anyMatch(player -> {
                                Faction faction = factionManager.getFactionByPlayer(player.getUniqueId());
                                return faction != null && faction.getName().equals(zone.getCurrentFaction());
                            });

                    if (!factionMemberPresent) {
                        zone.resetCapture();
                        Bukkit.broadcastMessage("§8» §4Warzone §8« §cZone has been neutralized!");
                    }
                }
            }
        }.runTaskTimer(main, 0, 1200);
    }

    private boolean canStartCapture() {
        if (playersInZone.size() < 3) return false;

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

        String message = "";
        if (currentFaction == null && zone.getProgress() == 0) {
            message = "§8» §7This zone is neutral.";
            bossBarManager(2, progress);
        }
        if (currentFaction == null && zone.getProgress() != 0) {
            bossBarManager(3, progress);
            message = "§8» §7This zone is neutral. Progression : §b" + progress + "§7%";
        }
        if (currentFaction != null && zone.getProgress() < 0) {
            bossBarManager(1, progress);
            message = "§8» §bLeading Faction : §c" + currentFaction + " §7| Progression : §b" + progress + "§7%";
        }

        for (Player player : playersInZone) {
            sendActionBar(player, message);
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private void bossBarManager(int type, double progress) {
        switch (type) {
            case 1:
                bossBar.setTitle("§bBuilding Captured by §f"+zone.getCurrentFaction());
                bossBar.setColor(BarColor.BLUE);
                bossBar.setStyle(BarStyle.SOLID);
                break;
            case 2:
                bossBar.setTitle("§7Building Neutral");
                bossBar.setColor(BarColor.WHITE);
                bossBar.setStyle(BarStyle.SOLID);
                break;
            case 3:
                bossBar.setTitle("§8"+zone.getLeadingFaction()+" §cis Capturing the Building");
                bossBar.setColor(BarColor.RED);
                bossBar.setStyle(BarStyle.SOLID);
                bossBar.setProgress(progress);
                break;
        }
    }
    private void loopBossBar() {
        Bukkit.getScheduler().runTaskTimer(main, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().getName().equals("warzone")) {
                    if (!bossBar.getPlayers().contains(player)) {
                        bossBar.addPlayer(player);
                    }
                } else {
                    bossBar.removePlayer(player);
                }
            }
        }, 0L, 20L);
    }
}