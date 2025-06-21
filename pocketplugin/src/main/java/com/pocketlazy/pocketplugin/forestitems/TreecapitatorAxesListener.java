package com.pocketlazy.pocketplugin.forestitems;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class TreecapitatorAxesListener implements Listener {
    // NamespacedKeys for NBT - must be initialized with plugin instance!
    public static NamespacedKey TREECAP_KEY;
    public static NamespacedKey WOODEN_CHOPPER_KEY;
    public static NamespacedKey LUMBER_AXE_KEY;

    /**
     * Call this as the VERY FIRST thing in your plugin's onEnable()!
     * Example:
     * TreecapitatorAxesListener.initializeKeys(this);
     */
    public static void initializeKeys(Plugin plugin) {
        TREECAP_KEY = new NamespacedKey(plugin, "treecapitator");
        WOODEN_CHOPPER_KEY = new NamespacedKey(plugin, "wooden_chopper");
        LUMBER_AXE_KEY = new NamespacedKey(plugin, "lumber_axe");
    }

    // Tool names
    public static final String TREECAP_NAME = ChatColor.DARK_GREEN + "Treecapitator";
    public static final String WOODEN_CHOPPER_NAME = ChatColor.YELLOW + "Wooden Chopper";
    public static final String LUMBER_AXE_NAME = ChatColor.GREEN + "Lumber Axe";

    // Lore generation that updates cooldown description according to current efficiency enchantment
    public static List<String> getTreecapLore(int efficiencyLevel) {
        double baseCooldown = 3.0;
        double cooldown = Math.max(0.5, baseCooldown - (efficiencyLevel * 0.5));
        return Arrays.asList(
                ChatColor.GREEN + "A mythical axe blessed by the forest.",
                "",
                ChatColor.GOLD + "Ability: " + ChatColor.DARK_GREEN + "Timber!",
                ChatColor.GRAY + "Break any log to fell all logs above.",
                ChatColor.DARK_GREEN + "Cooldown: " + cooldown + " seconds"
        );
    }
    public static List<String> getWoodenChopperLore(int efficiencyLevel) {
        double baseCooldown = 3.0;
        double cooldown = Math.max(0.5, baseCooldown - (efficiencyLevel * 0.5));
        return Arrays.asList(
                ChatColor.YELLOW + "A sturdy axe carved by novice lumberjacks.",
                "",
                ChatColor.GOLD + "Ability: " + ChatColor.GREEN + "Chop!",
                ChatColor.GRAY + "Break any log to fell 2 logs in a line.",
                ChatColor.DARK_GREEN + "Cooldown: " + cooldown + " seconds",
                ChatColor.DARK_GRAY + "Acts as a wooden axe if on cooldown."
        );
    }
    public static List<String> getLumberAxeLore(int efficiencyLevel) {
        double baseCooldown = 3.0;
        double cooldown = Math.max(0.5, baseCooldown - (efficiencyLevel * 0.5));
        return Arrays.asList(
                ChatColor.GREEN + "A legendary axe cherished by skilled loggers.",
                "",
                ChatColor.GOLD + "Ability: " + ChatColor.GREEN + "Chop!",
                ChatColor.GRAY + "Break any log to fell 3 logs in a line.",
                ChatColor.DARK_GREEN + "Cooldown: " + cooldown + " seconds",
                ChatColor.DARK_GRAY + "Acts as a stone axe if on cooldown."
        );
    }

    // Utility to update the lore of any custom axe after enchanting/repairing to reflect current efficiency
    public static void updateAxeLore(ItemStack axe) {
        if (axe == null || !axe.hasItemMeta()) return;
        ItemMeta meta = axe.getItemMeta();
        int efficiencyLevel = axe.getEnchantmentLevel(Enchantment.EFFICIENCY);

        if (isTreecapitator(axe)) {
            meta.setLore(getTreecapLore(efficiencyLevel));
        } else if (isWoodenChopper(axe)) {
            meta.setLore(getWoodenChopperLore(efficiencyLevel));
        } else if (isLumberAxe(axe)) {
            meta.setLore(getLumberAxeLore(efficiencyLevel));
        }
        axe.setItemMeta(meta);
    }

    // Cooldown tracking per player per axe
    private final Map<UUID, Long> treecapCooldown = new HashMap<>();
    private final Map<UUID, Long> woodenChopperCooldown = new HashMap<>();
    private final Map<UUID, Long> lumberAxeCooldown = new HashMap<>();

    // Block types
    private static final Set<Material> LOGS = EnumSet.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.BAMBOO_BLOCK, Material.CRIMSON_STEM, Material.WARPED_STEM
    );

    // ========================
    // Creation/Identification
    // ========================

    public static ItemStack createTreecapitator(int efficiencyLevel) {
        if (TREECAP_KEY == null) throw new IllegalStateException("TREECAP_KEY is not initialized! Call TreecapitatorAxesListener.initializeKeys(plugin) first!");
        ItemStack axe = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.setDisplayName(TREECAP_NAME);
        meta.setLore(getTreecapLore(efficiencyLevel));
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(TREECAP_KEY, PersistentDataType.BYTE, (byte)1);
        axe.setItemMeta(meta);
        return axe;
    }
    public static ItemStack getWoodenChopper(int efficiencyLevel) {
        if (WOODEN_CHOPPER_KEY == null) throw new IllegalStateException("WOODEN_CHOPPER_KEY is not initialized! Call TreecapitatorAxesListener.initializeKeys(plugin) first!");
        ItemStack axe = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.setDisplayName(WOODEN_CHOPPER_NAME);
        meta.setLore(getWoodenChopperLore(efficiencyLevel));
        meta.getPersistentDataContainer().set(WOODEN_CHOPPER_KEY, PersistentDataType.BYTE, (byte)1);
        axe.setItemMeta(meta);
        return axe;
    }
    public static ItemStack getLumberAxe(int efficiencyLevel) {
        if (LUMBER_AXE_KEY == null) throw new IllegalStateException("LUMBER_AXE_KEY is not initialized! Call TreecapitatorAxesListener.initializeKeys(plugin) first!");
        ItemStack axe = new ItemStack(Material.STONE_AXE, 1);
        ItemMeta meta = axe.getItemMeta();
        meta.setDisplayName(LUMBER_AXE_NAME);
        meta.setLore(getLumberAxeLore(efficiencyLevel));
        meta.getPersistentDataContainer().set(LUMBER_AXE_KEY, PersistentDataType.BYTE, (byte)1);
        axe.setItemMeta(meta);
        return axe;
    }

    public static boolean isTreecapitator(ItemStack stack) {
        if (TREECAP_KEY == null) throw new IllegalStateException("TREECAP_KEY is not initialized!");
        if (stack == null || stack.getType() != Material.GOLDEN_AXE) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(TREECAP_KEY, PersistentDataType.BYTE);
    }
    public static boolean isWoodenChopper(ItemStack stack) {
        if (WOODEN_CHOPPER_KEY == null) throw new IllegalStateException("WOODEN_CHOPPER_KEY is not initialized!");
        if (stack == null || stack.getType() != Material.WOODEN_AXE) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(WOODEN_CHOPPER_KEY, PersistentDataType.BYTE);
    }
    public static boolean isLumberAxe(ItemStack stack) {
        if (LUMBER_AXE_KEY == null) throw new IllegalStateException("LUMBER_AXE_KEY is not initialized!");
        if (stack == null || stack.getType() != Material.STONE_AXE) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(LUMBER_AXE_KEY, PersistentDataType.BYTE);
    }
    public static boolean isLog(Block block) {
        return LOGS.contains(block.getType());
    }

    // ========================
    // Core Ability Logic
    // ========================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (isTreecapitator(hand)) {
            handleTreecapitatorBreak(event, player, hand);
        } else if (isWoodenChopper(hand)) {
            handleWoodenChopperBreak(event, player, hand);
        } else if (isLumberAxe(hand)) {
            handleLumberAxeBreak(event, player, hand);
        }
    }

    private void handleTreecapitatorBreak(BlockBreakEvent event, Player player, ItemStack hand) {
        Block block = event.getBlock();
        if (!isLog(block)) return;
        int efficiencyLevel = hand.getEnchantmentLevel(Enchantment.EFFICIENCY);
        double baseCooldown = 3.0;
        double cooldown = Math.max(0.5, baseCooldown - (efficiencyLevel * 0.5));
        long last = treecapCooldown.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();

        if (now - last < (long)(cooldown * 1000)) {
            // On cooldown: allow vanilla break (normal golden axe)
            return;
        }
        // Off cooldown: fell all logs, set cooldown, cancel vanilla break
        event.setCancelled(true);
        treecapCooldown.put(player.getUniqueId(), now);

        int broken = fellTree(block, player, hand);

        if (broken > 1) {
            player.playSound(block.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1, 1.2f);
            player.sendActionBar(ChatColor.GOLD + "Timber! (" + broken + " logs)");
        }
    }

    private void handleWoodenChopperBreak(BlockBreakEvent event, Player player, ItemStack hand) {
        Block block = event.getBlock();
        if (!isLog(block)) return;
        int efficiencyLevel = hand.getEnchantmentLevel(Enchantment.EFFICIENCY);
        double baseCooldown = 3.0;
        double cooldown = Math.max(0.5, baseCooldown - (efficiencyLevel * 0.5));
        long last = woodenChopperCooldown.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();

        if (now - last < (long)(cooldown * 1000)) {
            // On cooldown: allow vanilla break (normal wooden axe)
            return;
        }
        // Off cooldown: break 2 logs in a line, set cooldown, cancel vanilla break
        event.setCancelled(true);
        woodenChopperCooldown.put(player.getUniqueId(), now);

        int broken = fellLogs(block, player, hand, 2);

        if (broken > 1) {
            player.playSound(block.getLocation(), Sound.BLOCK_WOOD_BREAK, 1, 1.2f);
            player.sendActionBar(ChatColor.GOLD + "Chop! (" + broken + " logs)");
        }
    }

    private void handleLumberAxeBreak(BlockBreakEvent event, Player player, ItemStack hand) {
        Block block = event.getBlock();
        if (!isLog(block)) return;
        int efficiencyLevel = hand.getEnchantmentLevel(Enchantment.EFFICIENCY);
        double baseCooldown = 3.0;
        double cooldown = Math.max(0.5, baseCooldown - (efficiencyLevel * 0.5));
        long last = lumberAxeCooldown.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();

        if (now - last < (long)(cooldown * 1000)) {
            // On cooldown: allow vanilla break (normal stone axe)
            return;
        }
        // Off cooldown: break 3 logs in a line, set cooldown, cancel vanilla break
        event.setCancelled(true);
        lumberAxeCooldown.put(player.getUniqueId(), now);

        int broken = fellLogs(block, player, hand, 3);

        if (broken > 1) {
            player.playSound(block.getLocation(), Sound.BLOCK_WOOD_BREAK, 1, 1.2f);
            player.sendActionBar(ChatColor.GOLD + "Chop! (" + broken + " logs)");
        }
    }

    // Timber/line breaking logic
    private int fellTree(Block base, Player player, ItemStack axe) {
        int broken = 0;
        Set<Block> visited = new HashSet<>();
        Queue<Block> todo = new ArrayDeque<>();
        todo.add(base);

        int maxLogs = 32; // Limit for treecapitator

        while (!todo.isEmpty() && broken < maxLogs) {
            Block b = todo.poll();
            if (!isLog(b) || !visited.add(b)) continue;
            b.breakNaturally(axe);
            broken++;
            if (broken >= maxLogs) break;
            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.SELF) continue;
                Block nb = b.getRelative(face);
                if (isLog(nb) && !visited.contains(nb)) {
                    todo.add(nb);
                }
            }
        }
        return broken;
    }

    private int fellLogs(Block base, Player player, ItemStack axe, int maxLogs) {
        int broken = 0;
        Set<Block> visited = new HashSet<>();
        Queue<Block> todo = new ArrayDeque<>();
        todo.add(base);

        while (!todo.isEmpty() && broken < maxLogs) {
            Block b = todo.poll();
            if (!isLog(b) || !visited.add(b)) continue;
            b.breakNaturally(axe);
            broken++;
            // Only go up for chain, you can enhance with face logic
            if (broken < maxLogs) {
                Block nb = b.getRelative(BlockFace.UP);
                if (isLog(nb) && !visited.contains(nb)) {
                    todo.add(nb);
                }
            }
        }
        // Durability logic: every 3 logs = -1
        if (broken > 0 && axe.getItemMeta() instanceof Damageable dmgMeta && !axe.getItemMeta().isUnbreakable()) {
            int unbreaking = axe.getEnchantmentLevel(Enchantment.UNBREAKING);
            int actualDamage = 0;
            Random rand = new Random();
            for (int i = 0; i < (broken / 3); i++) {
                if (unbreaking > 0 && rand.nextInt(unbreaking + 1) == 0) {
                    actualDamage++;
                } else if (unbreaking == 0) {
                    actualDamage++;
                }
            }
            int current = dmgMeta.getDamage();
            int newDamage = current + actualDamage;
            dmgMeta.setDamage(newDamage);
            axe.setItemMeta((ItemMeta) dmgMeta);
            if (newDamage >= axe.getType().getMaxDurability()) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1f);
            }
        }
        return broken;
    }

    // ========================
    // Recipe Registration (move your static registration here)
    // ========================

    public static void registerTreecapRecipe(Plugin plugin, boolean enable) {
        NamespacedKey key = new NamespacedKey(plugin, "treecapitator");
        Bukkit.removeRecipe(key);
        if (!enable) return;
        ShapedRecipe recipe = new ShapedRecipe(key, createTreecapitator(0));
        recipe.shape("GAG", "ALA", "GAG");
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('A', Material.GOLDEN_APPLE);
        recipe.setIngredient('L', Material.STONE_AXE); // Lumber Axe
        Bukkit.addRecipe(recipe);
    }
    public static void registerWoodenChopperRecipe(Plugin plugin, boolean enable) {
        NamespacedKey key = new NamespacedKey(plugin, "wooden_chopper");
        Bukkit.removeRecipe(key);
        if (!enable) return;
        ShapedRecipe recipe = new ShapedRecipe(key, getWoodenChopper(0));
        recipe.shape("LLL", "LAL", "LLL");
        recipe.setIngredient('A', Material.WOODEN_AXE);
        recipe.setIngredient('L', Material.OAK_LOG);
        Bukkit.addRecipe(recipe);
    }
    public static void registerLumberAxeRecipe(Plugin plugin, boolean enable) {
        NamespacedKey key = new NamespacedKey(plugin, "lumber_axe");
        Bukkit.removeRecipe(key);
        if (!enable) return;
        ShapedRecipe recipe = new ShapedRecipe(key, getLumberAxe(0));
        recipe.shape("CCC", "CLC", "CCC");
        recipe.setIngredient('L', Material.STONE_AXE);
        recipe.setIngredient('C', Material.PLAYER_HEAD); // Compact Oak head
        Bukkit.addRecipe(recipe);
    }

    // ========================
    // Crafting result NBT copy (if needed)
    // ========================

    @EventHandler
    public void onTreecapCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || !isTreecapitator(result)) return;

        // Look for a Lumber Axe in the matrix
        ItemStack axe = null;
        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (isLumberAxe(stack)) {
                axe = stack;
                break;
            }
        }
        if (axe == null) return;

        int efficiencyLevel = axe.getEnchantmentLevel(Enchantment.EFFICIENCY);

        // Copy NBT/enchants from source axe to treecapitator
        ItemMeta axeMeta = axe.getItemMeta();
        ItemStack treecap = createTreecapitator(efficiencyLevel);
        ItemMeta treecapMeta = treecap.getItemMeta();

        // Remove all enchants from treecap, then copy all except UNBREAKING from the source axe
        treecapMeta.getEnchants().forEach((enchant, level) -> treecapMeta.removeEnchant(enchant));
        axeMeta.getEnchants().forEach((enchant, level) -> {
            if (enchant != Enchantment.UNBREAKING) { // DURABILITY is Unbreaking
                treecapMeta.addEnchant(enchant, level, true);
            }
        });
        if (axeMeta.hasCustomModelData())
            treecapMeta.setCustomModelData(axeMeta.getCustomModelData());
        treecapMeta.setDisplayName(TREECAP_NAME);
        treecapMeta.setLore(getTreecapLore(efficiencyLevel));
        treecapMeta.setUnbreakable(true);
        treecapMeta.getPersistentDataContainer().set(TREECAP_KEY, PersistentDataType.BYTE, (byte)1);
        treecap.setItemMeta(treecapMeta);

        event.getInventory().setResult(treecap);
    }

    @EventHandler
    public void onLumberAxeCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || !isLumberAxe(result)) return;

        // Find Wooden Chopper
        ItemStack woodenChopper = null;
        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (isWoodenChopper(stack)) {
                woodenChopper = stack;
                break;
            }
        }
        if (woodenChopper == null) return;

        int efficiencyLevel = woodenChopper.getEnchantmentLevel(Enchantment.EFFICIENCY);

        // Copy NBT/enchants
        ItemStack lumberAxe = getLumberAxe(efficiencyLevel);
        ItemMeta srcMeta = woodenChopper.getItemMeta();
        ItemMeta dstMeta = lumberAxe.getItemMeta();

        dstMeta.getEnchants().forEach((enchant, level) -> dstMeta.removeEnchant(enchant));
        srcMeta.getEnchants().forEach((enchant, level) -> dstMeta.addEnchant(enchant, level, true));
        if (srcMeta.hasCustomModelData())
            dstMeta.setCustomModelData(srcMeta.getCustomModelData());
        dstMeta.setDisplayName(LUMBER_AXE_NAME);
        // Lore is set by getLumberAxe(efficiencyLevel)
        dstMeta.getPersistentDataContainer().set(LUMBER_AXE_KEY, PersistentDataType.BYTE, (byte)1);
        lumberAxe.setItemMeta(dstMeta);

        event.getInventory().setResult(lumberAxe);
    }

    @EventHandler
    public void onWoodenChopperCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || !isWoodenChopper(result)) return;

        // Look for a Wooden Axe in the matrix
        ItemStack axe = null;
        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (stack != null && stack.getType() == Material.WOODEN_AXE) {
                axe = stack;
                break;
            }
        }
        if (axe == null) return;

        int efficiencyLevel = axe.getEnchantmentLevel(Enchantment.EFFICIENCY);

        // Copy NBT/enchants from source axe to wooden chopper
        ItemMeta axeMeta = axe.getItemMeta();
        ItemStack woodenChopper = getWoodenChopper(efficiencyLevel);
        ItemMeta woodenChopperMeta = woodenChopper.getItemMeta();

        woodenChopperMeta.getEnchants().forEach((enchant, level) -> woodenChopperMeta.removeEnchant(enchant));
        axeMeta.getEnchants().forEach((enchant, level) -> {
            if (enchant != Enchantment.UNBREAKING) {
                woodenChopperMeta.addEnchant(enchant, level, true);
            }
        });
        if (axeMeta.hasCustomModelData())
            woodenChopperMeta.setCustomModelData(axeMeta.getCustomModelData());
        woodenChopperMeta.setDisplayName(WOODEN_CHOPPER_NAME);
        woodenChopperMeta.setLore(getWoodenChopperLore(efficiencyLevel));
        woodenChopperMeta.getPersistentDataContainer().set(WOODEN_CHOPPER_KEY, PersistentDataType.BYTE, (byte)1);
        woodenChopper.setItemMeta(woodenChopperMeta);

        event.getInventory().setResult(woodenChopper);
    }

    // ========================
    // Lore auto-update on enchant/repair
    // ========================

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        // Delay update so enchantments are applied
        Bukkit.getScheduler().runTaskLater(
                Objects.requireNonNull(event.getEnchanter().getServer().getPluginManager().getPlugin("PocketPlugin")),
                () -> updateAxeLore(event.getItem()),
                1L
        );
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result != null && (isTreecapitator(result) || isWoodenChopper(result) || isLumberAxe(result))) {
            updateAxeLore(result);
        }
    }

    // Handles /enchant as best as possible (item must be re-selected to update)
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item != null && (isTreecapitator(item) || isWoodenChopper(item) || isLumberAxe(item))) {
            updateAxeLore(item);
        }
    }
}