package com.openwar.openwarwarzone.WarzoneCTF;

import com.openwar.openwarfaction.factions.Faction;
import com.openwar.openwarfaction.factions.FactionManager;
import com.openwar.openwarwarzone.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class BuildingCapture implements Listener {

    private final Cuboid captureZone = new Cuboid(
            new Location(Bukkit.getWorld("warzone"), 2774, 60, 3085),
            new Location(Bukkit.getWorld("warzone"), 2759, 46, 3115)
    );

    private final Set<Player> playersInZone = new HashSet<>();
    private Faction currentOwner;
    private Faction capturingFaction;
    private int captureProgress = 0;
    private BukkitTask captureTask;
    private BukkitTask resetTask;
    private BossBar bossBar;
    private Main main;
    private FactionManager fm;
    private BukkitTask resetProgressTask;


    public BuildingCapture(Main main, FactionManager fm, BossBar bossBar) {
        this.fm = fm;
        this.main = main;
        this.bossBar = bossBar;
        onStart();
    }

    public void onStart() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateCaptureState();
            }
        }.runTaskTimer(main, 0L, 20L);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentOwner != null) {
                    Location loc = new Location(Bukkit.getWorld("warzone"), 2767 ,57 ,3098);
                    if (loc.getBlock().getType().equals(Material.AIR)) {
                        Bukkit.broadcastMessage("§8» §4Warzone §8« §cAirdrop called at Building by §4" + currentOwner.getName() + " §c!");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "event airdrop 2767 57 3098 13");
                    }
                }
            }
        }.runTaskTimer(main, 0L, 20L * 60 * 10);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        boolean nowInZone = captureZone.contains(event.getTo());
        boolean wasInZone = playersInZone.contains(player);

        if (nowInZone && !wasInZone) {
            handlePlayerEnter(player);
        } else if (!nowInZone && wasInZone) {
            handlePlayerExit(player);
        }
    }

    private void handlePlayerEnter(Player player) {
        playersInZone.add(player);
        sendActionBar(player, "§8» §aYou are on the capture zone !");
        updateBossBar();
    }

    private void handlePlayerExit(Player player) {
        playersInZone.remove(player);
        sendActionBar(player, "§8» §cYou left the capture zone !");
        updateBossBar();
        checkCaptureInterruption();
    }

    private void checkCaptureInterruption() {
        if (capturingFaction == null && currentOwner == null) return;

        Faction factionToCheck = capturingFaction != null ? capturingFaction : currentOwner;

        boolean hasMembersInZone = playersInZone.stream()
                .anyMatch(p -> factionToCheck.equals(getFaction(p)));

        if (!hasMembersInZone) {
            startResetCountdown();
        } else {
            if (resetProgressTask != null) {
                resetProgressTask.cancel();
                resetProgressTask = null;
            }
        }
    }

    private void startResetCountdown() {
        if (resetProgressTask != null) resetProgressTask.cancel();

        resetProgressTask = Bukkit.getScheduler().runTaskLater(main, () -> {
            Faction factionToCheck = capturingFaction != null ? capturingFaction : currentOwner;

            boolean stillNoMembers = playersInZone.stream()
                    .noneMatch(p -> factionToCheck.equals(getFaction(p)));

            if (stillNoMembers) {
                currentOwner = null;
                resetCapture();
                updateBossBar();
            }
        }, 20 * 60L);
    }
    private void updateCaptureState() {
        if (!canStartCapture()) {
            resetCapture();
            return;
        }

        Faction dominant = getDominantFaction();
        if (dominant == null) {
            resetCapture();
            return;
        }

        if (capturingFaction == null || !capturingFaction.equals(dominant)) {
            startNewCapture(dominant);
        }
    }
    private void startNewCapture(Faction faction) {
        capturingFaction = faction;
        captureProgress = 0;

        if (captureTask != null) captureTask.cancel();

        captureTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (capturingFaction.equals(getDominantFaction())) {
                    advanceCapture();
                } else {
                    interruptCapture();
                }
            }
        }.runTaskTimer(main, 0L, 20L);
    }

    private void advanceCapture() {
        captureProgress += 2;
        updateBossBar();

        if (captureProgress >= 100) {
            completeCapture();
        }
    }

    private void completeCapture() {
        if (currentOwner == null) {
            Bukkit.broadcastMessage("§8» §4Warzone §8« §cBuilding as been Captured by §4"+capturingFaction.getName());
        }
        currentOwner = capturingFaction;

        checkCaptureInterruption();
    }



    private void interruptCapture() {
        resetCapture();
    }

    private void resetCapture() {
        captureProgress = 0;
        capturingFaction = null;
        if (captureTask != null) captureTask.cancel();
        updateBossBar();
    }

    private void updateBossBar() {
        bossBar.setVisible(true);

        if (currentOwner != null) {
            bossBar.setTitle("Building Captured By §b" + currentOwner.getName());
            bossBar.setColor(BarColor.BLUE);
            bossBar.setProgress(1.0);
        } else if (capturingFaction != null) {
            bossBar.setTitle("Building Getting Captured by §6" + capturingFaction.getName());
            bossBar.setColor(BarColor.YELLOW);
            bossBar.setProgress(captureProgress / 100.0);
        } else {
            bossBar.setTitle("Building Neutral");
            bossBar.setColor(BarColor.WHITE);
            bossBar.setProgress(1.0);
        }

        playersInZone.forEach(p -> {
            if (!bossBar.getPlayers().contains(p)) {
                bossBar.addPlayer(p);
            }
        });
    }

    private boolean canStartCapture() {
        return true;
//        if (playersInZone.size() < 3) return false;
//        long factionCount = playersInZone.stream()
//                .map(player -> fm.getFactionByPlayer(player.getUniqueId()))
//                .filter(faction -> faction != null)
//                .map(Faction::getName)
//                .distinct()
//                .count();
//        return factionCount >= 2;
    }


    private Faction getDominantFaction() {
        Map<Faction, Integer> counts = new HashMap<>();
        playersInZone.forEach(p ->
                counts.merge(getFaction(p), 1, Integer::sum)
        );

        Optional<Map.Entry<Faction, Integer>> maxEntry = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (!maxEntry.isPresent()) {
            return null;
        }

        int maxCount = maxEntry.get().getValue();
        long numberOfMax = counts.values().stream()
                .filter(v -> v == maxCount)
                .count();

        if (numberOfMax > 1) {
            return null;
        }

        return maxEntry.get().getKey();
    }

    private Faction getFaction(Player player) {
        return fm.getFactionByPlayer(player.getUniqueId());
    }


    private static class Cuboid {
        private final int x1, y1, z1, x2, y2, z2;
        private final World world;

        public Cuboid(Location loc1, Location loc2) {
            this.world = loc1.getWorld();
            this.x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
            this.y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
            this.z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
            this.x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
            this.y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
            this.z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        }

        public boolean contains(Location loc) {
            return loc.getWorld().equals(world) &&
                    loc.getBlockX() >= x1 && loc.getBlockX() <= x2 &&
                    loc.getBlockY() >= y1 && loc.getBlockY() <= y2 &&
                    loc.getBlockZ() >= z1 && loc.getBlockZ() <= z2;
        }
    }
    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}