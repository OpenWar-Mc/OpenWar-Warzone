package com.openwar.openwarwarzone.Handler;

import com.openwar.openwarlevels.manager.PlayerManager;
import com.openwar.openwarwarzone.Main;
import com.openwar.openwarwarzone.Utils.Tuple;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
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
import java.util.stream.Collectors;

public class LootCrate implements Listener {
    private final PlayerManager pl;
    private final Map<Location, CrateWrapper> crateInventoryMap;
    private final Map<Inventory, CrateWrapper> inventoryToWrapper;
    private Map<Location, Long> crateTimers = new HashMap<>();
    private JavaPlugin main;

    List<Tuple<String, Integer, Integer>> crates = new ArrayList<>();

    private static class CrateWrapper {
        Inventory inventory;
        List<SimpleEntry<ItemStack, Integer>> loot;
        List<Integer> occupiedSlots;
        Tuple<String, Integer, Integer> crateType;
        int currentProgress;
        BukkitTask currentTask;
        boolean isSafe;

        public CrateWrapper(Inventory inventory, List<SimpleEntry<ItemStack, Integer>> loot,
                            List<Integer> occupiedSlots, Tuple<String, Integer, Integer> crateType,
                            boolean isSafe) {
            this.inventory = inventory;
            this.loot = loot;
            this.occupiedSlots = occupiedSlots;
            this.crateType = crateType;
            this.currentProgress = 0;
            this.isSafe = isSafe;
        }
    }

    public LootCrate(PlayerManager pl, Main main) {
        this.pl = pl;
        this.crateInventoryMap = new HashMap<>();
        this.inventoryToWrapper = new WeakHashMap<>();
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
                    Tuple<String, Integer, Integer> crateType = found.get();
                    long cooldownTime = crateType.getSecond() * 60 * 1000L;
                    long currentTime = System.currentTimeMillis();

                    if (crateTimers.containsKey(crateLoc)) {
                        long lastOpenTime = crateTimers.get(crateLoc);
                        long timeSinceLastOpen = currentTime - lastOpenTime;

                        if (timeSinceLastOpen >= cooldownTime) {
                            CrateWrapper oldWrapper = crateInventoryMap.get(crateLoc);
                            if (oldWrapper != null) {
                                inventoryToWrapper.remove(oldWrapper.inventory);
                            }
                            boolean isSafe = crateType.getFirst().equals("HBM_SAFE");
                            if (isSafe) {
                                Bukkit.broadcastMessage("§8» §4Warzone §8« §f" + event.getPlayer().getName() + " §cis looting the Safe");
                            }
                            CrateWrapper newWrapper = regenerateCrate(event, crateLoc, crateType, isSafe);
                            crateInventoryMap.put(crateLoc, newWrapper);
                            crateTimers.put(crateLoc, currentTime);
                            event.getPlayer().openInventory(newWrapper.inventory);
                        } else {
                            CrateWrapper wrapper = crateInventoryMap.get(crateLoc);
                            if (wrapper != null) {
                                event.getPlayer().openInventory(wrapper.inventory);
                            }
                        }
                    } else {
                        boolean isSafe = crateType.getFirst().equals("HBM_SAFE");
                        if (isSafe) {
                            Bukkit.broadcastMessage("§8» §4Warzone §8« §f" + event.getPlayer().getName() + " §cis looting the Safe");
                        }
                        CrateWrapper wrapper = regenerateCrate(event, crateLoc, crateType, isSafe);
                        crateInventoryMap.put(crateLoc, wrapper);
                        crateTimers.put(crateLoc, currentTime);
                        event.getPlayer().openInventory(wrapper.inventory);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void invEvent(InventoryCloseEvent event) {
        if (event.getPlayer().getWorld().getName().equals("warzone")) {
            if (event.getInventory().getHolder() == null && event.getView().getTitle().startsWith("§8§l")) {
                Location crateLoc = null;
                Block targetBlock = event.getPlayer().getTargetBlock(null, 5);
                if (targetBlock != null) {
                    crateLoc = targetBlock.getLocation();
                }
                Location loc = new Location(targetBlock.getWorld(), 2768, 57, 3100);
                double distance = loc.distance(crateLoc);
                if (distance < 20) {
                    CrateWrapper wrapper = inventoryToWrapper.get(event.getInventory());
                    if (wrapper != null && isInventoryEmpty(event.getInventory())) {
                        if (targetBlock.getType().toString().equals("MWC_SUPPLY_DROP")) {
                            crateLoc.getBlock().setType(Material.AIR);
                            crateInventoryMap.remove(crateLoc);
                            crateTimers.remove(crateLoc);
                            inventoryToWrapper.remove(event.getInventory());
                        }
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

    private CrateWrapper regenerateCrate(PlayerInteractEvent event, Location crateLoc,
                                         Tuple<String, Integer, Integer> crateType, boolean isSafe) {
        Location loc = new Location(crateLoc.getWorld(),2768, 57 ,3100);
        double distance = loc.distance(crateLoc);
        List<SimpleEntry<ItemStack, Integer>> loot = createLoot(crateType, distance < 20);
        Inventory inv = Bukkit.createInventory(null, crateType.getThird(), "§8§l" + getDisName(crateType.getFirst()));
        CrateWrapper wrapper = new CrateWrapper(inv, loot, new ArrayList<>(), crateType, isSafe);
        Random random = new Random();

        for (SimpleEntry<ItemStack, Integer> entry : loot) {
            int slot;
            do {
                slot = random.nextInt(crateType.getThird());
            } while (wrapper.occupiedSlots.contains(slot));
            wrapper.occupiedSlots.add(slot);

            ItemStack cobweb = new ItemStack(Material.BARRIER);
            ItemMeta meta = cobweb.getItemMeta();
            meta.setDisplayName("§7Searching...");
            meta.setLore(Collections.singletonList("§8[§7     §8]"));
            cobweb.setItemMeta(meta);
            inv.setItem(slot, cobweb);
        }

        inventoryToWrapper.put(inv, wrapper);
        processNextItem(wrapper);
        return wrapper;
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
                items.add(new Tuple<>("MWC_ARMORPLATET2", 1, 10));
                items.add(new Tuple<>("MWC_ARMORPLATET1", 1, 10));
                items.add(new Tuple<>("MWC_M38MAG_2", 2, 30));
                items.add(new Tuple<>("MWC_M4A1MAG_2", 2, 40));
                items.add(new Tuple<>("MWC_M38_DMR", 1, 30));
                items.add(new Tuple<>("MWC_M4A1", 1, 40));
                items.add(new Tuple<>("MWC_SV98", 1, 10));
                items.add(new Tuple<>("MWC_ACOG", 1, 45));
                items.add(new Tuple<>("MWC_REMINGTON870", 1, 35));
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
                items.add(new Tuple<>("MWC_BULLET9X19MM", 40, 45));
                items.add(new Tuple<>("MWC_BULLET9X18MM", 38, 45));
                items.add(new Tuple<>("MWC_BULLET45ACP", 35, 55));
                items.add(new Tuple<>("MWC_BULLET762X39", 30, 45));
                items.add(new Tuple<>("MWC_BULLET762X54", 38, 10));
                items.add(new Tuple<>("MWC_BULLET556X45", 35, 50));
                items.add(new Tuple<>("MWC_BULLET545X39", 32, 50));
                items.add(new Tuple<>("MWC_SHOTGUN12GAUGE", 4, 35));
                items.add(new Tuple<>("MWC_AK74", 1, 5));
                items.add(new Tuple<>("MWC_AK47", 1, 5));
                items.add(new Tuple<>("MWC_MAC10", 1, 15));
                finalItem = generateLoot(items, 3);
                return finalItem;
            case "MWC_WEAPONS_CASE":
                items.add(new Tuple<>("MWC_SOCOM_MAG", 2, 35));
                items.add(new Tuple<>("MWC_SV98MAG_2", 2, 40));
                items.add(new Tuple<>("MWC_M38MAG_2", 2, 35));
                items.add(new Tuple<>("MWC_M4A1MAG_2", 2, 40));
                items.add(new Tuple<>("MWC_M38_DMR", 1, 30));
                items.add(new Tuple<>("MWC_REMINGTON870", 1, 35));
                items.add(new Tuple<>("MWC_M4A1", 1, 40));
                items.add(new Tuple<>("MWC_SV98", 1, 10));
                finalItem = generateLoot(items, 2);
                return finalItem;
            case "MWC_AMMO_BOX":
                items.add(new Tuple<>("MWC_BULLET9X19MM", 40, 35));
                items.add(new Tuple<>("MWC_BULLET9X18MM", 38, 35));
                items.add(new Tuple<>("MWC_BULLET45ACP", 35, 35));
                items.add(new Tuple<>("MWC_BULLET762X39", 30, 45));
                items.add(new Tuple<>("MWC_BULLET762X54", 38, 10));
                items.add(new Tuple<>("MWC_BULLET556X45", 35, 50));
                items.add(new Tuple<>("MWC_BULLET545X39", 32, 50));
                items.add(new Tuple<>("MWC_SHOTGUN12GAUGE", 4, 35));
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
                    items.add(new Tuple<>("MWC_ARMORPLATET3", 1, 80));
                    items.add(new Tuple<>("MWC_ARMORPLATET4", 1, 40));
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
                    items.add(new Tuple<>("MWC_ARMORPLATET3", 1, 20));
                    items.add(new Tuple<>("MWC_ARMORPLATET4", 1, 60));
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
                items.add(new Tuple<>("MWC_ARMORPLATET3", 1, 20));
                items.add(new Tuple<>("MWC_ARMORPLATET2", 1, 30));
                items.add(new Tuple<>("MWC_ARMORPLATET1", 1, 30));
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
    private void processNextItem(CrateWrapper wrapper) {
        if (wrapper.currentProgress >= wrapper.loot.size()) {
            return;
        }

        SimpleEntry<ItemStack, Integer> entry = wrapper.loot.get(wrapper.currentProgress);
        int slot = wrapper.occupiedSlots.get(wrapper.currentProgress);

        ItemStack finalItem = entry.getKey().clone();
        finalItem.setAmount(getWeightedRandom(entry.getValue()));

        ItemStack cobweb = wrapper.inventory.getItem(slot);
        if (cobweb == null || cobweb.getType() != Material.BARRIER) {
            wrapper.currentProgress++;
            processNextItem(wrapper);
            return;
        }

        wrapper.currentTask = Bukkit.getScheduler().runTaskLater(main, new Runnable() {
            int progress = 0;
            final int maxProgress = 5;

            @Override
            public void run() {
                if (wrapper.inventory.getViewers().isEmpty()) {
                    return;
                }

                if (progress < maxProgress) {
                    ItemMeta meta = cobweb.getItemMeta();
                    StringBuilder progressBar = new StringBuilder("§8[§7");
                    for (int i = 0; i < progress; i++) progressBar.append("█");
                    for (int i = progress; i < maxProgress; i++) progressBar.append(" ");
                    progressBar.append("§8]");
                    meta.setLore(Collections.singletonList(progressBar.toString()));
                    cobweb.setItemMeta(meta);
                    wrapper.inventory.setItem(slot, cobweb);

                    for (HumanEntity player : wrapper.inventory.getViewers()) {
                        Player p = (Player) player;
                        p.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.4f, 1.0f);
                    }

                    progress++;
                    wrapper.currentTask = Bukkit.getScheduler().runTaskLater(main, this, wrapper.isSafe ? 40L : 5L);
                } else {
                    wrapper.inventory.setItem(slot, finalItem);
                    wrapper.currentProgress++;
                    processNextItem(wrapper);
                }
            }
        }, wrapper.isSafe ? 40L : 5L);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        CrateWrapper wrapper = inventoryToWrapper.get(inv);
        if (wrapper != null && wrapper.currentProgress < wrapper.loot.size()) {
            if (wrapper.currentTask == null || wrapper.currentTask.isCancelled()) {
                processNextItem(wrapper);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        CrateWrapper wrapper = inventoryToWrapper.get(inv);
        if (wrapper != null && wrapper.currentTask != null) {
            wrapper.currentTask.cancel();
            wrapper.currentTask = null;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        CrateWrapper wrapper = inventoryToWrapper.get(inv);
        if (wrapper != null) {
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
                event.setCancelled(true);
            }
        }
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