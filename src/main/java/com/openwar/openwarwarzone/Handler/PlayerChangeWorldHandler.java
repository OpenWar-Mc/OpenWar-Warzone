package com.openwar.openwarwarzone.Handler;

import org.bukkit.boss.BossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerChangeWorldHandler implements Listener {
    BossBar bossBar;


    public PlayerChangeWorldHandler(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        String worldFrom = event.getFrom().getName();
        String worldNow = event.getPlayer().getWorld().getName();
        if (!worldNow.equals("warzone")) {
            bossBar.removePlayer(event.getPlayer());
        } else {
            bossBar.addPlayer(event.getPlayer());
        }
    }
}
