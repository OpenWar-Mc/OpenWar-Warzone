package com.openwar.openwarwarzone;

import com.openwar.openwarfaction.factions.FactionManager;
import com.openwar.openwarlevels.manager.PlayerManager;
import com.openwar.openwarwarzone.EventCrate.CrateFaction;
import com.openwar.openwarwarzone.Handler.AllowedCommands;
import com.openwar.openwarwarzone.Handler.LootCrate;
import com.openwar.openwarwarzone.Handler.PlayerChangeWorldHandler;
import com.openwar.openwarwarzone.WarzoneCTF.BuildingCapture;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.openwar.openwarwarzone.Utils.DiscordWebhookEmbed.sendWebhook;

public final class Main extends JavaPlugin {
    private PlayerManager pl;
    private FactionManager fm;
    private Economy economy = null;

    private boolean setupDepend() {
        RegisteredServiceProvider<PlayerManager> levelProvider = getServer().getServicesManager().getRegistration(PlayerManager.class);
        RegisteredServiceProvider<FactionManager> factionDataProvider = getServer().getServicesManager().getRegistration(FactionManager.class);
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (    levelProvider == null || factionDataProvider == null || rsp == null) {
            System.out.println("ERROR !!!!!!!!!!!!!!!!!!!!");
            return false;
        }
        pl = levelProvider.getProvider();
        fm = factionDataProvider.getProvider();
        economy = rsp.getProvider();
        return true;
    }

    @Override
    public void onEnable() {
        System.out.println("====================================");
        System.out.println(" ");
        System.out.println(" OpenWar - Warzone loading...");
        if (!setupDepend()) {return;}
        BossBar bossBar = Bukkit.createBossBar("Building Neutral", BarColor.WHITE, BarStyle.SOLID);
        getServer().getPluginManager().registerEvents(new BuildingCapture(this, fm, bossBar), this);
        getServer().getPluginManager().registerEvents(new PlayerChangeWorldHandler(bossBar), this);
        getServer().getPluginManager().registerEvents(new LootCrate(pl, this), this);
        getServer().getPluginManager().registerEvents(new CrateFaction(pl, this), this);
        getServer().getPluginManager().registerEvents(new AllowedCommands(), this);
        eventYeay();
    }


    // -- this stay here.
    private void eventYeay() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Player> players = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
                if (players.size() > 2) {
                    Random random = new Random();
                    int x = random.nextInt(8001) - 4000;
                    int z = random.nextInt(8001) - 4000;
                    Bukkit.broadcastMessage("§8» §4Event §8« §cSupply Drop at: §7x: §8"+x+" §7z: §8"+z);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "event airdrop "+x+" 200 "+z+" 6");
                    String webhookUrl = "https://discord.com/api/webhooks/1395408967081136179/YsHYq8L8FfsAV5kID0372yQH13limGjUDw5rJLpDiNG_gRtEvTA-8ySbz-FTe_9AjGky";

                    try {
                        sendWebhook(webhookUrl, x, z);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    for (Player player : players) {
                        getServer().dispatchCommand(Bukkit.getConsoleSender(), "hud https://media.discordapp.net/attachments/1263073609287729192/1395406402268430377/aaaaa.png?ex=687a54fb&is=6879037b&hm=bb563831a7d7bdb9c4820c269d735bcc1ee17b281bae528d939477b1aed91d6f&=&format=png&quality=lossless&width=985&height=479 75 30 up [&cAirdrop &f- &4Faction World] 1 down "+player.getDisplayName()+" false false");
                    }
                }

            }
        }.runTaskTimer(this, 360000, 360000);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
