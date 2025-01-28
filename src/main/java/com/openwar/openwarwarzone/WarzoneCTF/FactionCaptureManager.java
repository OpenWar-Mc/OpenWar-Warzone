package com.openwar.openwarwarzone.WarzoneCTF;

import com.openwar.openwarfaction.factions.Faction;
import com.openwar.openwarfaction.factions.FactionManager;
import org.bukkit.entity.Player;

public class FactionCaptureManager {
    private Zone zone;

    public FactionCaptureManager(Zone zone) {
        this.zone = zone;
    }

    public void handlePlayerEnter(Player player, FactionManager fm) {
        Faction faction = fm.getFactionByPlayer(player.getUniqueId());
        if (faction == null) return;
        zone.updatePresence(faction.getName(), 1);
        System.out.println("ENTRY " + player.getName());
    }

    public void handlePlayerExit(Player player, FactionManager fm) {
        Faction faction = fm.getFactionByPlayer(player.getUniqueId());
        if (faction == null) return;
        zone.updatePresence(faction.getName(), -1);
        System.out.println("EXIT " + player.getName());
    }

    public void handleCaptureTick() {
        String leadingFaction = zone.getLeadingFaction();
        if (leadingFaction != null) {
            zone.progressCapture(leadingFaction);
        }
    }
}