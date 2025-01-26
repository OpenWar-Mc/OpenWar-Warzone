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
import java.util.Map;
import java.util.Set;

public class CTFHandler implements Listener {

    private final Zone zone;
    private final FactionCaptureManager manager;
    private final FactionManager factionManager;
    private final Main main;
    private final Set<Player> playersInZone = new HashSet<>();
    private final BossBar bossBar;

    private static final int TICK_INTERVAL = 20;
    private static final int NEUTRALIZATION_CHECK_INTERVAL = 1200; // 1 minute

    public CTFHandler(Zone zone, FactionCaptureManager manager, FactionManager factionManager, Main main) {
        this.zone = zone;
        this.manager = manager;
        this.factionManager = factionManager;
        this.main = main;
        this.bossBar = Bukkit.createBossBar("§7Building Neutral", BarColor.WHITE, BarStyle.SOLID);

        startCaptureTask();
        startNeutralizationCheckTask();
        loopBossBar();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals("warzone")) return;

        boolean isInZone = zone.isPlayerInRegion(player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                2774D, 56D, 3085D, 2759D, 46D, 3115D);
        Faction faction = factionManager.getFactionByPlayer(player.getUniqueId());
        if (isInZone) {
            if (faction == null) return;
            playersInZone.add(player);
            manager.handlePlayerEnter(player, factionManager);
            sendActionBar(player, "§8» §aYou are on the capture zone!");
        } else {
            if (faction == null) return;
            playersInZone.remove(player);
            manager.handlePlayerExit(player, factionManager);
            sendActionBar(player, "§8» §cYou are leaving the capture zone!");
        }
    }

    private void startCaptureTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!canStartCapture()) {
                    zone.resetCapture();
                    updateBossBar("§7Building Neutral", BarColor.WHITE, 0);
                    return;
                }
                manager.handleCaptureTick();
                updateBossBarBasedOnProgress();
            }
        }.runTaskTimer(main, 0, TICK_INTERVAL);
    }

    private void startNeutralizationCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (zone.isCaptured() && !zone.isFactionPresentInZone(playersInZone, factionManager)) {
                    zone.resetCapture();
                }
            }
        }.runTaskTimer(main, NEUTRALIZATION_CHECK_INTERVAL, NEUTRALIZATION_CHECK_INTERVAL);
    }

    private boolean canStartCapture() {
        return true;
//        if (playersInZone.size() < 3) return false;
//
//        long factionCount = playersInZone.stream()
//                .map(player -> factionManager.getFactionByPlayer(player.getUniqueId()))
//                .filter(faction -> faction != null)
//                .map(Faction::getName)
//                .distinct()
//                .count();
//
//        return factionCount >= 2;
    }

    private void updateBossBarBasedOnProgress() {
        String leadingFaction = zone.getLeadingFaction();
        int progress = zone.getProgress();

        if (leadingFaction == null) {
            updateBossBar("§7Building Neutral", BarColor.WHITE, 100);
        } else {
            if (progress == 0) {
                updateBossBar("§bCapturing Faction: §f" + leadingFaction, BarColor.BLUE, 100.0);
            } else {
                updateBossBar("§f"+leadingFaction+" §bis Capturing the Building", BarColor.BLUE, progress / 100.0);
            }
        }
    }

    private void updateBossBar(String title, BarColor color, double progress) {
        bossBar.setTitle(title);
        bossBar.setColor(color);
        bossBar.setProgress(progress);
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

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
