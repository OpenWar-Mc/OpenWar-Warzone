package com.openwar.openwarwarzone.Handler;


import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;


public class AllowedCommands implements Listener {

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {return;}
        String command = event.getMessage().toLowerCase();
        String worldName = player.getWorld().getName();
        if (worldName.equalsIgnoreCase("warzone")) {
            if (command.contains("f chat") || command.contains("r") || command.contains("msg") || command.contains("money")) {
                return;
            } else {
                event.setCancelled(true);
                player.sendMessage("§8» §cYou can only use §7/f chat, /r, /msg, §cand §7/money §cin the warzone.");
            }
        }
    }
}