package com.openwar.openwarwarzone.WarzoneCTF;

import com.openwar.openwarfaction.factions.FactionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Zone {
    private String name;
    private String currentFaction = null;
    private int progress = 0;
    private Map<String, Integer> factionPresence = new HashMap<>();
    private static final int CAPTURE_PROGRESS_MAX = 100;
    private boolean isPaused = false;

    public Zone(String name) {
        this.name = name;
    }

    public void updatePresence(String factionName, int count) {
        factionPresence.put(factionName, factionPresence.getOrDefault(factionName, 0) + count);
        factionPresence.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    public String getLeadingFaction() {
        return factionPresence.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey).orElse(null);
    }

    public void progressCapture(String factionName) {
        if (isPaused) return;

        if (currentFaction == null || !currentFaction.equals(factionName)) {
            progress++;
            if (progress >= CAPTURE_PROGRESS_MAX) {
                currentFaction = factionName;
                progress = 0;
                Bukkit.broadcastMessage("§8» §4Warzone §8« §c" + currentFaction + " §7has captured the zone!");
            }
        }
    }

    public void pauseCapture() {
        isPaused = true;
    }

    public void resetCapture() {
        currentFaction = null;
        progress = 0;
        factionPresence.clear();
        isPaused = false;
    }

    public String getCurrentFaction() {
        return currentFaction;
    }

    public boolean isPlayerInRegion(double playerX, double playerY, double playerZ,
                                    double x1, double y1, double z1,
                                    double x2, double y2, double z2) {
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2);
        double maxY = Math.max(y1, y2);
        double minZ = Math.min(z1, z2);
        double maxZ = Math.max(z1, z2);
        return (playerX >= minX && playerX <= maxX) &&
                (playerY >= minY && playerY <= maxY) &&
                (playerZ >= minZ && playerZ <= maxZ);
    }

    public int getProgress() {
        return progress;
    }
}