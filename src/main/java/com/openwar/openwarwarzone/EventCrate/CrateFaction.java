package com.openwar.openwarwarzone.EventCrate;


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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CrateFaction implements Listener{
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

    public CrateFaction(LevelSaveAndLoadBDD pl, Main main) {
        this.pl = pl;
        this.crateInventory = new HashMap<>();
        this.main = main;
        loadCrates();
    }

    private void loadCrates() {
        crates.add(new Tuple<>("MWC_SUPPLY_DROP", 35, 27));
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onLoot(PlayerInteractEvent event) {
        if (event.getPlayer().getWorld().getName().equals("faction")) {
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
                                    regenerateCrate(event, crateLoc, TriplesCouilles, false);
                                    event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cX §7Empty"));
                                } else {
                                    event.getPlayer().openInventory(inv);
                                }
                            }
                        }
                    } else {
                        boolean isSafe = false;
                        generateCrate(event, crateLoc, TriplesCouilles, isSafe);
                    }
                }
            }
        }
    }

    private void regenerateCrate(PlayerInteractEvent event, Location crateLoc, Tuple<String, Integer, Integer> TriplesCouilles, boolean isSafe) {
        Block block = crateLoc.getBlock();
        block.setType(Material.AIR);
    }
    private void generateCrate(PlayerInteractEvent event, Location crateLoc, Tuple<String, Integer, Integer> TriplesCouilles, boolean isSafe) {
        List<AbstractMap.SimpleEntry<ItemStack, Integer>> loot = createLoot(TriplesCouilles);
        Inventory inv = createGUI(loot, TriplesCouilles, event.getPlayer(), isSafe);
        crateInventory.put(crateLoc, inv);
        crateTimers.put(crateLoc, System.currentTimeMillis());
        event.getPlayer().openInventory(inv);
    }
    private List<AbstractMap.SimpleEntry<ItemStack, Integer>> createLoot(Tuple<String, Integer, Integer> tuple) {
        String type = tuple.getFirst();
        List<Tuple<String, Integer, Integer>> items = new ArrayList<>();
        List<AbstractMap.SimpleEntry<ItemStack, Integer>> finalItem;
        switch (type) {
            case "MWC_SUPPLY_DROP":
                items.add(new Tuple<>("MWC_JUGGERNAUT_HELMET", 1, 25));
                items.add(new Tuple<>("MWC_GHILLIE_HELMET", 1, 25));
                items.add(new Tuple<>("MWC_SWAT_VEST", 1, 25));
                items.add(new Tuple<>("MWC_M110_SASS", 1, 45));
                items.add(new Tuple<>("HBM_MINE_AP", 4, 55));
                items.add(new Tuple<>("HBM_GRENADE_IF_IMPACT", 2, 65));
                items.add(new Tuple<>("HBM_GRENADE_POISON", 2, 55));
                items.add(new Tuple<>("MWC_JUGGERNAUT_CHEST", 1, 25));
                items.add(new Tuple<>("MWC_GHILLIE_CHEST", 1, 25));
                items.add(new Tuple<>("MWC_USMC_VEST_URBAN", 1, 35));
                items.add(new Tuple<>("MWC_KRISS_VECTOR", 1, 65));
                items.add(new Tuple<>("MWC_M200_INTERVENTION", 1, 35));
                items.add(new Tuple<>("HBM_GRENADE_IF_TOXIC", 2, 55));
                items.add(new Tuple<>("MWC_JUGGERNAUT_BOOTS", 1, 25));
                items.add(new Tuple<>("MWC_GHILLIE_BOOTS", 1, 25));
                items.add(new Tuple<>("HBM_FUSION_CORE", 1, 20));
                items.add(new Tuple<>("MWC_DUFFLE_BAG", 1, 65));
                items.add(new Tuple<>("MWC_HK_417", 1, 55));
                items.add(new Tuple<>("HBM_GRENADE_IF_HE", 4, 65));
                finalItem = generateLoot(items, 6);
                return finalItem;
        }
        return null;
    }


    private List<AbstractMap.SimpleEntry<ItemStack, Integer>> generateLoot(List<Tuple<String, Integer, Integer>> items, int nb) {
        Random rand = new Random();
        List<AbstractMap.SimpleEntry<ItemStack, Integer>> finalItems = new ArrayList<>();
        nb = getWeightedRandom(nb);
        for (int x = 0; x < nb; x++) {
            boolean isSelected = false;
            while (!isSelected) {
                int i = rand.nextInt(items.size());
                Tuple<String, Integer, Integer> item = items.get(i);
                isSelected = rand.nextInt(100) < item.getThird();

                if (isSelected) {
                    if (item.getFirst().startsWith("GOLDMC")) {
                        finalItems.add(new AbstractMap.SimpleEntry<>(genGold(), item.getSecond()));
                        continue;
                    }
                    if (item.getFirst().startsWith("MONEYSAFE")) {
                        finalItems.add(new AbstractMap.SimpleEntry<>(genMoneySafe(), item.getSecond()));
                        continue;
                    }
                    if (item.getFirst().startsWith("MONEY")) {
                        finalItems.add(new AbstractMap.SimpleEntry<>(genMoney(), item.getSecond()));
                        continue;
                    } else {
                        finalItems.add(new AbstractMap.SimpleEntry<>(getItemStackFromString(item.getFirst()), item.getSecond()));
                    }
                }
            }
        }
        return finalItems;
    }

    private Inventory createGUI(List<AbstractMap.SimpleEntry<ItemStack, Integer>> loot, Tuple<String, Integer, Integer> crate, Player player, boolean isSafe) {
        if (loot == null || crate == null) {
            throw new IllegalArgumentException("Loot list or crate cannot be null.");
        }

        Random random = new Random();
        String name = getDisName(crate.getFirst());
        Inventory gui = Bukkit.createInventory(null, crate.getThird(), "§8§l" + name);

        Set<Integer> occupiedSlots = new HashSet<>();

        for (AbstractMap.SimpleEntry<ItemStack, Integer> entry : loot) {
            ItemStack item = entry.getKey();
            if (item == null || entry.getValue() <= 0) {
                continue;
            }

            int slot;
            do {
                slot = random.nextInt(crate.getThird());
            } while (occupiedSlots.contains(slot));

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
                        event.setCancelled(true);
                    }
                }
            }

            @EventHandler
            public void onInventoryClose(InventoryCloseEvent event) {
                if (event.getPlayer().equals(player) && event.getInventory().equals(gui)) {
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
                        int progress = playerProgress.getOrDefault(player, 0);
                        playersWithOpenInventory.add(player);
                        processNextItem(gui, loot, crate, occupiedSlots, progress, player, isSafe);
                    }
                }
            }
        }, main);

        return gui;
    }
    private void processNextItem(Inventory gui, List<AbstractMap.SimpleEntry<ItemStack, Integer>> lootList, Tuple<String, Integer, Integer> crate, Set<Integer> occupiedSlots, int currentIndex, Player player, boolean isSafe) {
        if (currentIndex >= lootList.size()) {
            return;
        }

        AbstractMap.SimpleEntry<ItemStack, Integer> entry = lootList.get(currentIndex);
        ItemStack item = entry.getKey();
        if (item == null || entry.getValue() <= 0) {
            processNextItem(gui, lootList, crate, occupiedSlots, currentIndex + 1, player, isSafe);
            return;
        }

        final ItemStack finalItem = item.clone();
        finalItem.setAmount(getWeightedRandom(entry.getValue()));

        int slot = occupiedSlots.stream()
                .filter(s -> gui.getItem(s) != null && gui.getItem(s).getType() == Material.BARRIER)
                .findFirst()
                .orElse(-1);

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
                    return;
                }

                if (progress < maxProgress) {
                    progress++;

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
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.4f, 1.0f);
                        }
                    });

                    Bukkit.getScheduler().runTaskLater(main, this, delay);
                } else {
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