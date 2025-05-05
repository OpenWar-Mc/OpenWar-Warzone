package com.openwar.openwarwarzone.Handler;

import com.openwar.openwarcore.Utils.LevelSaveAndLoadBDD;
import com.openwar.openwarlevels.level.PlayerLevel;
import com.openwar.openwarwarzone.Main;
import com.openwar.openwarwarzone.Utils.Tuple;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.AbstractMap.SimpleEntry;

import java.util.*;

public class LootCrate implements Listener {
    private final LevelSaveAndLoadBDD pl;
    private final Map<Location, Inventory> crateInventory;
    private Map<Location, Long> crateTimers = new HashMap<>();
    private Map<Player, Integer> playerProgress = new HashMap<>();
    private Map<Player, BukkitTask> activeTasks = new HashMap<>();
    private Set<Player> playersWithOpenInventory = new HashSet<>();
    private JavaPlugin main;

    double exp;
    PlayerLevel xp;

    List<Tuple<String, Integer, Integer>> crates = new ArrayList<>();

    public LootCrate(LevelSaveAndLoadBDD pl, Main main) {
        this.pl = pl;
        this.crateInventory = new HashMap<>();
        this.main = main;
        loadCrates();
    }

    private void loadCrates() {
        crates.add(new Tuple<>("MWC_FRIDGE_CLOSED", 15, 9));
        crates.add(new Tuple<>("MWC_FRIDGE_OPEN", 15, 9));
        crates.add(new Tuple<>("MWC_FILINGCABINET_OPENED", 20, 9));
        crates.add(new Tuple<>("MWC_FILINGCABINET", 20, 9));
        crates.add(new Tuple<>("MWC_DUMPSTER", 10, 9));
        crates.add(new Tuple<>("MWC_WOODEN_CRATE_OPENED", 27, 18));
        crates.add(new Tuple<>("CFM_COUNTER_DRAWER", 18, 9));
        crates.add(new Tuple<>("CFM_BEDSIDE_CABINET_OAK", 18, 9));
        crates.add(new Tuple<>("CFM_DESK_CABINET_OAK", 18, 9));
        crates.add(new Tuple<>("MWC_RUSSIAN_WEAPONS_CASE", 25, 18));
        crates.add(new Tuple<>("MWC_WEAPONS_CASE", 35, 18));
        crates.add(new Tuple<>("MWC_AMMO_BOX", 15, 9));
        crates.add(new Tuple<>("MWC_WEAPONS_CASE_SMALL", 23, 9));
        crates.add(new Tuple<>("MWC_WEAPONS_LOCKER", 30, 18));
        crates.add(new Tuple<>("MWC_MEDICAL_CRATE", 18, 9));
        crates.add(new Tuple<>("MWC_TRASH_BIN", 12, 9));
        crates.add(new Tuple<>("MWC_VENDING_MACHINE", 18, 9));
        crates.add(new Tuple<>("MWC_SUPPLY_DROP", 35, 27));
        crates.add(new Tuple<>("MWC_SCP_LOCKER", 24, 9));
        crates.add(new Tuple<>("MWC_LOCKER", 17, 9));
        crates.add(new Tuple<>("MWC_ELECTRIC_BOX_OPENED", 10, 9));
        crates.add(new Tuple<>("MWC_ELECTRIC_BOX", 10, 9));
        crates.add(new Tuple<>("HBM_RADIOREC", 12, 9));
        crates.add(new Tuple<>("HBM_SAFE", 45, 27));
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onLoot(PlayerInteractEvent event) {
        if (event.getPlayer().isOp()) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(false);
                return;
            }
        }
        if (event.getPlayer().getWorld().getName().equals("warzone")) {
            Block block = event.getClickedBlock();
            if (block != null) {
                Location crateLoc = block.getLocation();
                Optional<Tuple<String, Integer, Integer>> found = crates.stream()
                        .filter(tuple -> tuple.getFirst().equals(block.getType().name()))
                        .findFirst();


                if (found.isPresent()) {
                    event.setCancelled(true);
                    Tuple<String, Integer, Integer> TriplesCouilles = found.get();
                    long cooldownTime = TriplesCouilles.getSecond() * 60 * 1000L;
                    long currentTime = System.currentTimeMillis();


                    if (crateTimers.containsKey(crateLoc)) {
                        long lastOpenTime = crateTimers.get(crateLoc);
                        long timeSinceLastOpen = currentTime - lastOpenTime;


                        if (timeSinceLastOpen >= cooldownTime) {
                            boolean isSafe = false;
                            if (block.getType().name().equals("HBM_SAFE")) {
                                isSafe = true;
                                Bukkit.broadcastMessage("§8» §4Warzone §8« §f"+event.getPlayer().getName()+" §cis looting the Safe");
                            }
                            regenerateCrate(event, crateLoc, TriplesCouilles, isSafe);

                        } else {
                            if (crateInventory.containsKey(crateLoc)) {
                                Inventory inv = crateInventory.get(crateLoc);
                                ItemStack[] contents = inv.getContents();
                                boolean isEmpty = true;
                                for (ItemStack item : contents) {
                                    if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                                        isEmpty = false;
                                        break;
                                    }
                                }

                                if (isEmpty) {
                                    event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cX §7Empty"));
                                } else {
                                    event.getPlayer().openInventory(inv);
                                }
                            }
                        }



                    } else {
                        boolean isSafe = false;
                        if (block.getType().name().equals("HBM_SAFE")) {
                            isSafe = true;
                            Bukkit.broadcastMessage("§8» §4Warzone §8« §f"+event.getPlayer().getName()+" §cis looting the Safe");
                        }
                        regenerateCrate(event, crateLoc, TriplesCouilles, isSafe);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void invEvent(InventoryCloseEvent event) {
        if (event.getPlayer().getWorld().getName().equals("warzone")) {
            if (event.getPlayer().getOpenInventory().getTitle().equals("§8§lSupply")) {
                Inventory inventory = event.getInventory();
                Location crateLoc = null;
                Block targetBlock = event.getPlayer().getTargetBlock(null, 5);
                if (targetBlock != null) {
                    crateLoc = targetBlock.getLocation();
                }
                Location loc = new Location(targetBlock.getWorld(),2768, 57 ,3100);
                double distance = loc.distance(crateLoc);
                if (distance < 20) {
                    if (crateLoc != null && crateInventory.containsKey(crateLoc) && isInventoryEmpty(inventory)) {
                        crateLoc.getBlock().setType(Material.AIR);
                        crateInventory.remove(crateLoc);
                        crateTimers.remove(crateLoc);
                    }
                }
            }
        }
    }
    private boolean isInventoryEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private void regenerateCrate(PlayerInteractEvent event, Location crateLoc, Tuple<String, Integer, Integer> TriplesCouilles, boolean isSafe) {
        Location loc = new Location(crateLoc.getWorld(),2768, 57 ,3100);
        double distance = loc.distance(crateLoc);
        List<SimpleEntry<ItemStack, Integer>> loot;
        if (distance < 20) {
            System.out.println(distance);
             loot = createLoot(TriplesCouilles, true);
        } else {
            System.out.println(distance);
            loot = createLoot(TriplesCouilles, false);
        }
        Inventory inv = createGUI(loot, TriplesCouilles, event.getPlayer(), isSafe);
        crateInventory.put(crateLoc, inv);
        crateTimers.put(crateLoc, System.currentTimeMillis());
        event.getPlayer().openInventory(inv);
    }

    private List<SimpleEntry<ItemStack, Integer>> createLoot(Tuple<String, Integer, Integer> tuple, boolean coin) {
        String type = tuple.getFirst();
        List<Tuple<String, Integer, Integer>> items = new ArrayList<>();
        List<SimpleEntry<ItemStack, Integer>> finalItem;
        switch (type) {
            case "MWC_MEDICAL_CRATE":
                items.add(new Tuple<>("HBM_SYRINGE_METAL_STIMPAK", 2, 60));
                items.add(new Tuple<>("HBM_SYRINGE_METAL_SUPER", 2, 25));
                items.add(new Tuple<>("HBM_MED BAG", 2, 10));
                items.add(new Tuple<>("HBM_RADAWAY", 2, 45));
                items.add(new Tuple<>("HBM_RADAWAY_STRONG", 2, 25));
                items.add(new Tuple<>("HBM_RADAWAY_FLUSH", 2, 10));
                items.add(new Tuple<>("HBM_RADX", 2, 25));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_WEAPONS_LOCKER":
                items.add(new Tuple<>("MWC_SOCOM_MAG", 2, 35));
                items.add(new Tuple<>("MWC_SV98MAG_2", 2, 20));
                items.add(new Tuple<>("MWC_M38MAG_2", 2, 30));
                items.add(new Tuple<>("MWC_M4A1MAG_2", 2, 40));
                items.add(new Tuple<>("MWC_M38_DMR", 1, 30));
                items.add(new Tuple<>("MWC_M4A1", 1, 40));
                items.add(new Tuple<>("MWC_SV98", 1, 10));
                items.add(new Tuple<>("MWC_ACOG", 1, 45));
                items.add(new Tuple<>("MWC_MICROREFLEX", 1, 50));
                items.add(new Tuple<>("MWC_SPECTER", 1, 50));
                items.add(new Tuple<>("MWC_HOLOGRAPHIC2", 1, 45));
                finalItem = generateLoot(items, 3);
                return finalItem;
            case "MWC_FRIDGE_CLOSED":
                items.add(new Tuple<>("HARVESTCRAFT_GUMMYBEARSITEM", 3, 60));
                items.add(new Tuple<>("HARVESTCRAFT_FRUITPUNCHITEM", 1, 50));
                items.add(new Tuple<>("HARVESTCRAFT_PERSIMMONYOGURTITEM", 1, 60));
                items.add(new Tuple<>("HARVESTCRAFT_FOOTLONGITEM", 1, 40));
                items.add(new Tuple<>("HARVESTCRAFT_GLISTENINGSALADITEM", 1, 50));
                items.add(new Tuple<>("HARVESTCRAFT_PADTHAIITEM", 1, 50));
                items.add(new Tuple<>("HARVESTCRAFT_PORKRINDSITEM", 1, 40));
                items.add(new Tuple<>("HARVESTCRAFT_GLISTENINGSALADITEM", 1, 50));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 1, 30));
                items.add(new Tuple<>("MWC_M17", 1, 5));
                finalItem = generateLoot(items, 3);
                return finalItem;
            case "MWC_FRIDGE_OPEN":
                items.add(new Tuple<>("HARVESTCRAFT_GUMMYBEARSITEM", 3, 60));
                items.add(new Tuple<>("HARVESTCRAFT_FRUITPUNCHITEM", 1, 50));
                items.add(new Tuple<>("HARVESTCRAFT_PERSIMMONYOGURTITEM", 1, 60));
                items.add(new Tuple<>("HARVESTCRAFT_FOOTLONGITEM", 1, 40));
                items.add(new Tuple<>("HARVESTCRAFT_GLISTENINGSALADITEM", 1, 50));
                items.add(new Tuple<>("HARVESTCRAFT_PADTHAIITEM", 1, 50));
                items.add(new Tuple<>("HARVESTCRAFT_PORKRINDSITEM", 1, 40));
                items.add(new Tuple<>("HARVESTCRAFT_GLISTENINGSALADITEM", 1, 50));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 1, 30));
                items.add(new Tuple<>("MWC_M17", 1, 5));
                finalItem = generateLoot(items, 3);
                return finalItem;
            case "MWC_FILINGCABINET":
                items.add(new Tuple<>("MONEY", 1, 20));
                items.add(new Tuple<>("MWC_BULLET44", 16, 10));
                items.add(new Tuple<>("MWC_BULLET45ACP", 18, 20));
                items.add(new Tuple<>("MWC_BULLET9X18MM", 12, 50));
                items.add(new Tuple<>("MWC_BULLET9X19MM", 14, 60));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 1, 50));
                items.add(new Tuple<>("MWC_M17", 1, 5));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_FILINGCABINET_OPENED":
                items.add(new Tuple<>("MONEY", 1, 20));
                items.add(new Tuple<>("MWC_BULLET44", 16, 10));
                items.add(new Tuple<>("MWC_BULLET45ACP", 18, 20));
                items.add(new Tuple<>("MWC_BULLET9X18MM", 12, 50));
                items.add(new Tuple<>("MWC_BULLET9X19MM", 14, 60));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 1, 50));
                items.add(new Tuple<>("MWC_M17", 1, 5));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_DUMPSTER":
                items.add(new Tuple<>("ROTTEN_FLESH", 2, 60));
                items.add(new Tuple<>("POISONOUS_POTATO", 2, 50));
                items.add(new Tuple<>("HARVESTCRAFT_SWEETPICKLEITEM", 3, 40));
                items.add(new Tuple<>("HBM_CANNED_TOMATO", 1, 30));
                items.add(new Tuple<>("HARVESTCRAFT_ZOMBIEJERKYITEM", 1, 20));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 2, 10));
                items.add(new Tuple<>("HBM_WIRE_COPPER", 6, 10));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_WOODEN_CRATE_OPENED":
                items.add(new Tuple<>("MWC_BULLET9X19MM", 40, 60));
                items.add(new Tuple<>("MWC_BULLET9X18MM", 38, 60));
                items.add(new Tuple<>("MWC_BULLET45ACP", 35, 55));
                items.add(new Tuple<>("MWC_BULLET762X39", 30, 45));
                items.add(new Tuple<>("MWC_BULLET762X54", 38, 45));
                items.add(new Tuple<>("MWC_BULLET556X45", 35, 50));
                items.add(new Tuple<>("MWC_BULLET545X39", 32, 50));
                items.add(new Tuple<>("MWC_SV98MAG_2", 2, 20));
                items.add(new Tuple<>("MWC_SOCOM_MAG", 2, 30));
                items.add(new Tuple<>("MWC_M38MAG_2", 2, 30));
                items.add(new Tuple<>("MWC_M4A1MAG_2", 2, 30));
                items.add(new Tuple<>("MWC_AK74MAG", 2, 30));
                items.add(new Tuple<>("MWC_AK47MAG", 2, 30));
                items.add(new Tuple<>("MWC_AK47PMAGTAN", 2, 30));
                items.add(new Tuple<>("MWC_AK15MAG_2", 2, 30));
                items.add(new Tuple<>("MWC_AK74", 1, 5));
                items.add(new Tuple<>("MWC_AK47", 1, 5));
                items.add(new Tuple<>("MWC_MAC10", 1, 15));
                items.add(new Tuple<>("MWC_MAC10MAG", 3, 25));
                finalItem = generateLoot(items, 3);
                return finalItem;
            case "MWC_WEAPONS_CASE":
                items.add(new Tuple<>("MWC_SOCOM_MAG", 2, 35));
                items.add(new Tuple<>("MWC_SV98MAG_2", 2, 40));
                items.add(new Tuple<>("MWC_M38MAG_2", 2, 35));
                items.add(new Tuple<>("MWC_M4A1MAG_2", 2, 40));
                items.add(new Tuple<>("MWC_M38_DMR", 1, 30));
                items.add(new Tuple<>("MWC_M4A1", 1, 40));
                items.add(new Tuple<>("MWC_SV98", 1, 10));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_AMMO_BOX":
                items.add(new Tuple<>("MWC_BULLET9X19MM", 40, 60));
                items.add(new Tuple<>("MWC_BULLET9X18MM", 38, 60));
                items.add(new Tuple<>("MWC_BULLET45ACP", 35, 55));
                items.add(new Tuple<>("MWC_BULLET762X39", 30, 45));
                items.add(new Tuple<>("MWC_BULLET762X54", 38, 45));
                items.add(new Tuple<>("MWC_BULLET556X45", 35, 50));
                items.add(new Tuple<>("MWC_BULLET545X39", 32, 50));
                items.add(new Tuple<>("MWC_SV98MAG_2", 2, 20));
                items.add(new Tuple<>("MWC_SOCOM_MAG", 2, 30));
                items.add(new Tuple<>("MWC_M38MAG_2", 2, 30));
                items.add(new Tuple<>("MWC_M4A1MAG_2", 2, 30));
                items.add(new Tuple<>("MWC_AK74MAG", 2, 30));
                items.add(new Tuple<>("MWC_AK47MAG", 2, 30));
                items.add(new Tuple<>("MWC_AK47PMAGTAN", 2, 30));
                items.add(new Tuple<>("MWC_AK15MAG_2", 2, 30));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_VENDING_MACHINE":
                items.add(new Tuple<>("HARVESTCRAFT_SNICKERSBARITEM", 2, 40));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 2, 40));
                items.add(new Tuple<>("HARVESTCRAFT_CHOCOLATEMILKITEM", 2, 30));
                items.add(new Tuple<>("HBM_CANNED_TOMATO", 1, 40));
                items.add(new Tuple<>("HARVESTCRAFT_CRISPYRICEPUFFBARSITEM", 1, 45));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 2, 56));
                items.add(new Tuple<>("HARVESTCRAFT_BBQPOTATOCHIPSITEM", 4, 35));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_TRASH_BIN":
                items.add(new Tuple<>("ROTTEN_FLESH", 2, 60));
                items.add(new Tuple<>("POISONOUS_POTATO", 2, 50));
                items.add(new Tuple<>("HARVESTCRAFT_SWEETPICKLEITEM", 3, 40));
                items.add(new Tuple<>("HBM_CANNED_TOMATO", 1, 30));
                items.add(new Tuple<>("HARVESTCRAFT_ZOMBIEJERKYITEM", 1, 20));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 2, 10));
                items.add(new Tuple<>("HBM_WIRE_COPPER", 6, 10));
                items.add(new Tuple<>("HBM_NUCLEAR_WASTE", 1, 1));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_WEAPONS_CASE_SMALL":
                items.add(new Tuple<>("MWC_APSMAG_2", 2, 30));
                items.add(new Tuple<>("MWC_MAKAROVMAG", 2, 30));
                items.add(new Tuple<>("MWC_GLOCKMAG13", 1, 35));
                items.add(new Tuple<>("MWC_MAKAROV_PM", 1, 30));
                items.add(new Tuple<>("MWC_APS", 1, 20));
                items.add(new Tuple<>("MWC_GLOCK_18C", 1, 20));
                items.add(new Tuple<>("MWC_SILENCER9MM", 1, 10));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "CFM_COUNTER_DRAWER":
                items.add(new Tuple<>("MONEY", 1, 20));
                items.add(new Tuple<>("MWC_BULLET44", 16, 10));
                items.add(new Tuple<>("MWC_BULLET45ACP", 18, 20));
                items.add(new Tuple<>("MWC_BULLET9X18MM", 12, 50));
                items.add(new Tuple<>("MWC_BULLET9X19MM", 14, 60));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 1, 50));
                items.add(new Tuple<>("MWC_M17", 1, 5));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "CFM_BEDSIDE_CABINET_OAK":
                items.add(new Tuple<>("MONEY", 1, 20));
                items.add(new Tuple<>("MWC_BULLET44", 16, 10));
                items.add(new Tuple<>("MWC_BULLET45ACP", 18, 20));
                items.add(new Tuple<>("MWC_BULLET9X18MM", 12, 50));
                items.add(new Tuple<>("MWC_BULLET9X19MM", 14, 60));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 1, 50));
                items.add(new Tuple<>("MWC_M17", 1, 5));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "CFM_DESK_CABINET_OAK":
                items.add(new Tuple<>("MONEY", 1, 20));
                items.add(new Tuple<>("MWC_BULLET44", 16, 10));
                items.add(new Tuple<>("MWC_BULLET45ACP", 18, 20));
                items.add(new Tuple<>("MWC_BULLET9X18MM", 12, 50));
                items.add(new Tuple<>("MWC_BULLET9X19MM", 14, 60));
                items.add(new Tuple<>("HARVESTCRAFT_ENERGYDRINKITEM", 1, 50));
                items.add(new Tuple<>("MWC_M17", 1, 5));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_RUSSIAN_WEAPONS_CASE":
                items.add(new Tuple<>("MWC_AK74MAG", 2, 30));
                items.add(new Tuple<>("MWC_AK47MAG", 2, 30));
                items.add(new Tuple<>("MWC_AK47PMAGTAN", 2, 35));
                items.add(new Tuple<>("MWC_AK15MAG_2", 2, 35));
                items.add(new Tuple<>("MWC_AK74", 1, 35));
                items.add(new Tuple<>("MWC_AK47", 1, 45));
                items.add(new Tuple<>("MWC_MAC10", 1, 55));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_SUPPLY_DROP":
                if (coin) {
                    items.add(new Tuple<>("OPENWARDRUGZ_WARZONE_COIN", 2, 100));
                    items.add(new Tuple<>("MONEYSAFE", 1, 100));
                    items.add(new Tuple<>("MONEYSAFE", 1, 100));
                    items.add(new Tuple<>("MONEYSAFE", 1, 100));
                    items.add(new Tuple<>("GOLDMC", 2, 40));
                } else {
                    items.add(new Tuple<>("MWC_ACOG", 1, 45));
                    items.add(new Tuple<>("MWC_MICROREFLEX", 1, 50));
                    items.add(new Tuple<>("MWC_SPECTER", 1, 50));
                    items.add(new Tuple<>("MWC_HOLOGRAPHIC2", 1, 45));
                    items.add(new Tuple<>("MCHELI_FIM92", 1, 35));
                    items.add(new Tuple<>("MCHELI_FGM148", 1, 35));
                    items.add(new Tuple<>("MWC_SOCOM_MAG", 3, 40));
                    items.add(new Tuple<>("MWC_SV98MAG_2", 3, 35));
                    items.add(new Tuple<>("MWC_M38MAG_2", 3, 40));
                    items.add(new Tuple<>("MWC_M4A1MAG_2", 3, 30));
                    items.add(new Tuple<>("MWC_M38_DMR", 1, 30));
                    items.add(new Tuple<>("MWC_M4A1", 1, 40));
                    items.add(new Tuple<>("MWC_SV98", 1, 20));
                }
                finalItem = generateLoot(items, 4);
                return finalItem;
            case "MWC_SCP_LOCKER":
                items.add(new Tuple<>("MWC_MOLLE_BLACK", 1, 40));
                items.add(new Tuple<>("MWC_MOLLE_GREEN", 1, 40));
                items.add(new Tuple<>("MWC_MOLLE_URBAN", 1, 40));
                items.add(new Tuple<>("MWC_SWAT_VEST", 1, 10));
                items.add(new Tuple<>("MWC_FLYYE_FIELD_COMPACT_PLATE_CARRIER", 1, 40));
                items.add(new Tuple<>("MWC_M43A_CHEST_HARNESS", 1, 60));
                items.add(new Tuple<>("MWC_DUFFLE_BAG", 1, 10));
                items.add(new Tuple<>("MWC_TRU_SPEC_CORDURA_BACKPACK_FOREST", 1, 30));
                items.add(new Tuple<>("MWC_TRU_SPEC_CORDURA_BACKPACK_BLACK", 1, 30));
                items.add(new Tuple<>("MWC_ASSAULT_BACKPACK_FOREST", 1, 20));
                items.add(new Tuple<>("MWC_ASSAULT_BACKPACK_BLACK", 1, 20));
                items.add(new Tuple<>("MWC_COMBAT_SUSTAINMENT_BACKPACK_FOREST", 1, 30));
                items.add(new Tuple<>("MWC_COMBAT_SUSTAINMENT_BACKPACK_BLACK", 1, 30));
                items.add(new Tuple<>("MWC_ASSAULT_BACKPACK_TAN", 1, 20));
                items.add(new Tuple<>("MWC_TRU_SPEC_CORDURA_BACKPACK_TAN", 1, 15));
                items.add(new Tuple<>("MWC_F5_SWITCHBLADE_BACKPACK", 1, 20));
                items.add(new Tuple<>("MWC_COMBAT_SUSTAINMENT_BACKPACK_TAN", 1, 30));
                finalItem = generateLoot(items, 1);
                return finalItem;
            case "MWC_LOCKER":
                items.add(new Tuple<>("MWC_SPEC_OPS_BOOTS", 1, 30));
                items.add(new Tuple<>("MWC_SPEC_OPS_CHEST", 1, 30));
                items.add(new Tuple<>("MWC_SPEC_OPS_HELMET", 1, 30));
                items.add(new Tuple<>("MWC_MARINE_BOOTS", 1, 30));
                items.add(new Tuple<>("MWC_MARINE_CHEST", 1, 30));
                items.add(new Tuple<>("MWC_MARINE_HELMET", 1, 30));
                items.add(new Tuple<>("MWC_SPETZNAZ_HELMET", 1, 30));
                items.add(new Tuple<>("MWC_SPETZNAZ_CHEST", 1, 30));
                items.add(new Tuple<>("MWC_SPETZNAZ_BOOTS", 1, 30));
                items.add(new Tuple<>("MWC_URBAN_HELMET", 1, 30));
                items.add(new Tuple<>("MWC_URBAN_CHEST", 1, 30));
                items.add(new Tuple<>("MWC_URBAN_BOOTS", 1, 30));
                items.add(new Tuple<>("MWC_BLACKCAMO_CHEST", 1, 30));
                items.add(new Tuple<>("MWC_FOREST_CHEST", 1, 30));
                items.add(new Tuple<>("MWC_BLACKJEANS_BOOTS", 1, 30));
                items.add(new Tuple<>("MWC_KHAKIJEANS_BOOTS", 1, 30));
                items.add(new Tuple<>("MWC_SWAT_CHEST", 1, 30));
                items.add(new Tuple<>("MWC_SWAT_HELMET", 1, 30));
                items.add(new Tuple<>("MWC_SWAT_BOOTS", 1, 30));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_ELECTRIC_BOX_OPENED":
                items.add(new Tuple<>("HBM_CIRCUIT_TARGETING_TIER2", 1, 10));
                items.add(new Tuple<>("HBM_CIRCUIT_TARGETING_TIER1", 1, 20));
                items.add(new Tuple<>("HBM_CIRCUIT_ALUMINIUM", 1, 20));
                items.add(new Tuple<>("HBM_CIRCUIT_COPPER", 1, 10));
                items.add(new Tuple<>("HBM_CIRCUIT_RAW", 1, 30));
                items.add(new Tuple<>("HBM_MOTOR", 1, 10));
                items.add(new Tuple<>("HBM_WIRE_TUNGSTEN", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_COPPER", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_ALUMINIUM", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_RED_COPPER", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_GOLD", 32, 60));
                items.add(new Tuple<>("HBM_COIL_TUNGSTEN", 4, 50));
                items.add(new Tuple<>("HBM_COIL_COPPER", 4, 50));
                items.add(new Tuple<>("HBM_COIL_COPPER_TORUS", 2, 30));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "HBM_RADIOREC":
                items.add(new Tuple<>("HBM_CIRCUIT_TARGETING_TIER2", 1, 10));
                items.add(new Tuple<>("HBM_CIRCUIT_TARGETING_TIER1", 1, 20));
                items.add(new Tuple<>("HBM_CIRCUIT_ALUMINIUM", 1, 20));
                items.add(new Tuple<>("HBM_CIRCUIT_COPPER", 1, 10));
                items.add(new Tuple<>("HBM_CIRCUIT_RAW", 1, 30));
                items.add(new Tuple<>("HBM_MOTOR", 1, 10));
                items.add(new Tuple<>("HBM_WIRE_TUNGSTEN", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_COPPER", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_ALUMINIUM", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_RED_COPPER", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_GOLD", 32, 60));
                items.add(new Tuple<>("HBM_COIL_TUNGSTEN", 4, 50));
                items.add(new Tuple<>("HBM_COIL_COPPER", 4, 50));
                items.add(new Tuple<>("HBM_COIL_COPPER TORUS", 2, 30));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_ELECTRIC_BOX":
                items.add(new Tuple<>("HBM_CIRCUIT_TARGETING_TIER2", 1, 10));
                items.add(new Tuple<>("HBM_CIRCUIT_TARGETING_TIER1", 1, 20));
                items.add(new Tuple<>("HBM_CIRCUIT_ALUMINIUM", 1, 20));
                items.add(new Tuple<>("HBM_CIRCUIT_COPPER", 1, 10));
                items.add(new Tuple<>("HBM_CIRCUIT_RAW", 1, 30));
                items.add(new Tuple<>("HBM_MOTOR", 1, 10));
                items.add(new Tuple<>("HBM_WIRE_TUNGSTEN", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_COPPER", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_ALUMINIUM", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_RED_COPPER", 32, 60));
                items.add(new Tuple<>("HBM_WIRE_GOLD", 32, 60));
                items.add(new Tuple<>("HBM_COIL_TUNGSTEN", 4, 50));
                items.add(new Tuple<>("HBM_COIL_COPPER", 4, 50));
                items.add(new Tuple<>("HBM_COIL_COPPER_TORUS", 2, 30));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "HBM_SAFE":
                items.add(new Tuple<>("MONEYSAFE", 1, 100));
                items.add(new Tuple<>("MONEYSAFE", 1, 100));
                items.add(new Tuple<>("MONEYSAFE", 1, 100));
                items.add(new Tuple<>("MONEYSAFE", 1, 100));
                items.add(new Tuple<>("MONEYSAFE", 2, 100));
                items.add(new Tuple<>("GOLDMC", 1, 80));
                items.add(new Tuple<>("GOLDMC", 2, 40));
                items.add(new Tuple<>("GOLDMC", 3, 20));
                items.add(new Tuple<>("MONEYSAFE", 4, 10));
                finalItem = generateLoot(items, 8);
                return finalItem;
        }
        return null;
    }


    private List<SimpleEntry<ItemStack, Integer>> generateLoot(List<Tuple<String, Integer, Integer>> items, int nb) {
        Random rand = new Random();
        List<SimpleEntry<ItemStack, Integer>> finalItems = new ArrayList<>();
        nb = getWeightedRandom(nb);
        for (int x = 0; x < nb; x++) {
            boolean isSelected = false;
            while (!isSelected) {
                int i = rand.nextInt(items.size());
                Tuple<String, Integer, Integer> item = items.get(i);
                isSelected = rand.nextInt(100) < item.getThird();

                if (isSelected) {
                    if (item.getFirst().startsWith("GOLDMC")) {
                        finalItems.add(new SimpleEntry<>(genGold(), item.getSecond()));
                        continue;
                    }
                    if (item.getFirst().startsWith("MONEYSAFE")) {
                        finalItems.add(new SimpleEntry<>(genMoneySafe(), item.getSecond()));
                        continue;
                    }
                    if (item.getFirst().startsWith("MONEY")) {
                        finalItems.add(new SimpleEntry<>(genMoney(), item.getSecond()));
                        continue;
                    } else {
                        finalItems.add(new SimpleEntry<>(getItemStackFromString(item.getFirst()), item.getSecond()));
                    }
                }
            }
        }
        return finalItems;
    }

    private Inventory createGUI(List<SimpleEntry<ItemStack, Integer>> loot, Tuple<String, Integer, Integer> crate, Player player, boolean isSafe) {
        if (loot == null || crate == null) {
            throw new IllegalArgumentException("Loot list or crate cannot be null.");
        }

        if (isSafe) System.out.println("createGUI - Initializing inventory for crate: " + crate.getFirst() + " and player: " + player.getName());

        Random random = new Random();
        String name = getDisName(crate.getFirst());
        Inventory gui = Bukkit.createInventory(null, crate.getThird(), "§8§l" + name);

        Set<Integer> occupiedSlots = new HashSet<>();

        for (SimpleEntry<ItemStack, Integer> entry : loot) {
            ItemStack item = entry.getKey();
            if (item == null || entry.getValue() <= 0) {
                continue;
            }

            int slot;
            do {
                slot = random.nextInt(crate.getThird());
            } while (occupiedSlots.contains(slot));

            if (isSafe) System.out.println("createGUI - Assigning item to slot: " + slot);
            occupiedSlots.add(slot);

            ItemStack cobweb = new ItemStack(Material.BARRIER);
            ItemMeta cobwebMeta = cobweb.getItemMeta();
            cobwebMeta.setDisplayName("§7Searching...");
            cobwebMeta.setLore(Collections.singletonList("§8[§7     §8]"));
            cobweb.setItemMeta(cobwebMeta);
            gui.setItem(slot, cobweb);
        }

        playerProgress.put(player, 0);
        playersWithOpenInventory.add(player);
        processNextItem(gui, loot, crate, occupiedSlots, 0, player, isSafe);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onInventoryClick(InventoryClickEvent event) {
                if (event.getInventory().equals(gui)) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
                        if (isSafe) System.out.println("onInventoryClick - Clicked on BARRIER slot. Cancelling event.");
                        event.setCancelled(true);
                    }
                }
            }

            @EventHandler
            public void onInventoryClose(InventoryCloseEvent event) {
                if (event.getPlayer().equals(player) && event.getInventory().equals(gui)) {
                    if (isSafe) System.out.println("onInventoryClose - Inventory closed by player: " + player.getName());
                    BukkitTask task = activeTasks.get(player);
                    if (task != null) {
                        task.cancel();
                    }
                    playersWithOpenInventory.remove(player);
                }
            }

            @EventHandler
            public void onInventoryOpen(InventoryOpenEvent event) {
                if (event.getPlayer().equals(player) && event.getInventory().equals(gui)) {
                    if (!playersWithOpenInventory.contains(player)) {
                        if (isSafe) System.out.println("onInventoryOpen - Inventory re-opened by player: " + player.getName());
                        int progress = playerProgress.getOrDefault(player, 0);
                        playersWithOpenInventory.add(player);
                        processNextItem(gui, loot, crate, occupiedSlots, progress, player, isSafe);
                    }
                }
            }
        }, main);

        return gui;
    }

    private void processNextItem(Inventory gui, List<SimpleEntry<ItemStack, Integer>> lootList, Tuple<String, Integer, Integer> crate, Set<Integer> occupiedSlots, int currentIndex, Player player, boolean isSafe) {
        if (currentIndex >= lootList.size()) {
            if (isSafe) System.out.println("processNextItem - All items processed for player: " + player.getName());
            return;
        }

        SimpleEntry<ItemStack, Integer> entry = lootList.get(currentIndex);
        ItemStack item = entry.getKey();
        if (item == null || entry.getValue() <= 0) {
            if (isSafe) System.out.println("processNextItem - Skipping invalid item at index: " + currentIndex);
            processNextItem(gui, lootList, crate, occupiedSlots, currentIndex + 1, player, isSafe);
            return;
        }

        final ItemStack finalItem = item.clone();
        finalItem.setAmount(getWeightedRandom(entry.getValue()));

        int slot = occupiedSlots.stream()
                .filter(s -> gui.getItem(s) != null && gui.getItem(s).getType() == Material.BARRIER)
                .findFirst()
                .orElse(-1);

        if (isSafe) System.out.println("processNextItem - Processing item at index: " + currentIndex + ", slot: " + slot);

        ItemStack cobweb = new ItemStack(Material.BARRIER);
        ItemMeta cobwebMeta = cobweb.getItemMeta();
        cobwebMeta.setDisplayName("§7Searching...");
        StringBuilder progressBar = new StringBuilder("§8[§7     §8]");
        cobwebMeta.setLore(Collections.singletonList(progressBar.toString()));
        cobweb.setItemMeta(cobwebMeta);
        gui.setItem(slot, cobweb);

        final int finalSlot = slot;
        long delay = isSafe ? 40L : 5L;

        BukkitTask task = Bukkit.getScheduler().runTaskLater(main, new Runnable() {
            int progress = 0;
            final int maxProgress = 5;

            @Override
            public void run() {
                if (!playersWithOpenInventory.contains(player)) {
                    if (isSafe) System.out.println("processNextItem - Player no longer in inventory: " + player.getName());
                    return;
                }

                if (progress < maxProgress) {
                    progress++;

                    if (isSafe) System.out.println("processNextItem - Updating progress bar for player: " + player.getName() + ", progress: " + progress + "/" + maxProgress);

                    ItemMeta meta = cobweb.getItemMeta();
                    StringBuilder progressBar = new StringBuilder("§8[§7");
                    for (int i = 0; i < progress; i++) {
                        progressBar.append("█");
                    }
                    for (int i = progress; i < maxProgress; i++) {
                        progressBar.append(" ");
                    }
                    progressBar.append("§8]");

                    meta.setLore(Collections.singletonList(progressBar.toString()));
                    cobweb.setItemMeta(meta);
                    gui.setItem(finalSlot, cobweb);

                    Bukkit.getScheduler().runTask(main, () -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.4f, 1.0f);
                    });

                    Bukkit.getScheduler().runTaskLater(main, this, delay);
                } else {
                    if (isSafe) System.out.println("processNextItem - Replacing placeholder with final item for player: " + player.getName());
                    gui.setItem(finalSlot, finalItem);
                    Bukkit.getScheduler().runTaskLater(main, () -> {
                        playerProgress.put(player, currentIndex + 1);
                        processNextItem(gui, lootList, crate, occupiedSlots, currentIndex + 1, player, isSafe);
                    }, 1L);
                }
            }
        }, delay);

        activeTasks.put(player, task);
    }


    private String getDisName(String type) {
        if (type.startsWith("MWC") || type.startsWith("HBM") || type.startsWith("CFM")) {
            String[] names = type.toLowerCase().split("_");
            if (names.length > 1) {
                String name = names[1].replace("_", " ");
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                return name;
            }
        }
        System.out.println("Error getDisName for type : " +type);
        return null;
    }
    public static ItemStack getItemStackFromString(String itemName) {
        Material material = Material.matchMaterial(itemName.toUpperCase());
        if (material == null) {
            System.out.println("Error get Item Stack from string with "+itemName);
            return new ItemStack(Material.AIR);
        }
        return new ItemStack(material);
    }

    private ItemStack genMoneySafe() {
        Random random = new Random();
        int nb = random.nextInt(5);
        nb = getWeightedRandom(nb);
        int amoney = 0;
        switch (nb) {
            case 0:
                amoney = 200;
                break;
            case 1:
                amoney = 400;
                break;
            case 2:
                amoney = 600;
                break;
            case 3:
                amoney = 850;
                break;
            case 4:
                amoney = 1000;
                break;
            case 5:
                amoney = 1500;
                break;
        }
        ItemStack money = new ItemStack(Material.matchMaterial("openwarprops:money"));
        ItemMeta meta = money.getItemMeta();
        meta.setLore(Arrays.asList("§7$"+amoney));
        meta.setDisplayName("§6Money");
        money.setItemMeta(meta);
        return money;
    }
    private ItemStack genMoney() {
        Random random = new Random();
        int choice = random.nextInt(3);
        int amoney = 200;
        if (choice == 0) {
            amoney = 50;
        } else if (choice == 1) {
            amoney = 150;
        } else if (choice == 2) {
            amoney = 200;
        }
        ItemStack money = new ItemStack(Material.matchMaterial("openwarprops:money"));
        ItemMeta meta = money.getItemMeta();
        meta.setLore(Arrays.asList("§7$"+amoney));
        meta.setDisplayName("§6Money");
        money.setItemMeta(meta);
        return money;
    }
    private ItemStack genGold() {
        ItemStack gold = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = gold.getItemMeta();
        meta.setLore(Arrays.asList("§eRare Loot"));
        meta.setDisplayName("§6§lGold Ingot");
        gold.setItemMeta(meta);
        return gold;
    }

    public static int getWeightedRandom(int nb) {
        if (nb == 0){
            return nb;
        }
        int totalWeight = (nb * (nb + 1)) / 2;
        Random random = new Random();
        int randomNumber = random.nextInt(totalWeight);

        for (int i = 1; i <= nb; i++) {
            randomNumber -= i;
            if (randomNumber < 0) {
                return i;
            }
        }
        return nb;
    }
}