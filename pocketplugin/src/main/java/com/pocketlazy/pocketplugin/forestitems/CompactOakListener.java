package com.pocketlazy.pocketplugin.forestitems;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class CompactOakListener implements Listener {
    public static final int OAK_SIZE = 27;
    private static final String OAK_TITLE = ChatColor.GREEN + "Compact Oak";
    private static final NamespacedKey OAK_UUID_KEY = new NamespacedKey("pocketplugin", "compact_oak_uuid");
    public static final String FILL_OWNER = "Shakzan_Fill";

    public static boolean isCompactOak(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PLAYER_HEAD) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(OAK_UUID_KEY, PersistentDataType.STRING);
    }

    public static ItemStack createCompactOak() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Compact Oak");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A compressed log of Oak.",
                ChatColor.DARK_GRAY + "Right-click to open"
        ));
        try {
            OfflinePlayer oakPlayer = Bukkit.getOfflinePlayer(FILL_OWNER);
            meta.setOwningPlayer(oakPlayer);
        } catch (Throwable ignored) {}
        String uuid = UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(OAK_UUID_KEY, PersistentDataType.STRING, uuid);
        head.setItemMeta(meta);
        return head;
    }

    public static String getOrAssignOakId(ItemStack oak) {
        if (!isCompactOak(oak)) return null;
        ItemMeta meta = oak.getItemMeta();
        String id = meta.getPersistentDataContainer().get(OAK_UUID_KEY, PersistentDataType.STRING);
        if (id == null) {
            id = UUID.randomUUID().toString();
            meta.getPersistentDataContainer().set(OAK_UUID_KEY, PersistentDataType.STRING, id);
            oak.setItemMeta(meta);
        }
        return id;
    }

    public static String getOakId(ItemStack oak) {
        if (!isCompactOak(oak)) return null;
        ItemMeta meta = oak.getItemMeta();
        return meta.getPersistentDataContainer().get(OAK_UUID_KEY, PersistentDataType.STRING);
    }

    public static void updateOakMeta(ItemStack oak) {
        if (!isCompactOak(oak)) return;
        SkullMeta meta = (SkullMeta) oak.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Compact Oak");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A compressed log of Oak.",
                ChatColor.DARK_GRAY + "Right-click to open"
        ));
        try {
            OfflinePlayer oakPlayer = Bukkit.getOfflinePlayer(FILL_OWNER);
            meta.setOwningPlayer(oakPlayer);
        } catch (Throwable ignored) {}
        oak.setItemMeta(meta);
    }

    // --- Listeners ---

    @EventHandler
    public void onOakOpen(PlayerInteractEvent event) {
        // Only allow opening from main hand, not offhand
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (!isCompactOak(item)) return;
        event.setCancelled(true);

        String oakId = getOrAssignOakId(item);
        updateOakMeta(item);

        Inventory inv = Bukkit.createInventory(event.getPlayer(), OAK_SIZE, OAK_TITLE);

        for (int i = 0; i < OAK_SIZE; i++) {
            if (i == 13) {
                ItemStack wisdomBook = new ItemStack(Material.WRITABLE_BOOK);
                ItemMeta bookMeta = wisdomBook.getItemMeta();
                bookMeta.setDisplayName(ChatColor.GRAY + "Forest Wisdom");
                bookMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "The wood pulses with ancient energy.",
                        ChatColor.GRAY + "Some say it can even upgrade the",
                        ChatColor.GRAY + "legendary Wood Chopper...",
                        "",
                        ChatColor.RED + "" + ChatColor.BOLD + "Locked"
                ));
                wisdomBook.setItemMeta(bookMeta);
                inv.setItem(i, wisdomBook);
            } else {
                ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = pane.getItemMeta();
                meta.setDisplayName(ChatColor.DARK_GRAY + "Locked");
                pane.setItemMeta(meta);
                inv.setItem(i, pane);
            }
        }

        event.getPlayer().openInventory(inv);
        CompactOakSession.setSession(event.getPlayer(), item);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_WOOD_PLACE, 1f, 1f);
    }

    @EventHandler
    public void onOakClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(OAK_TITLE)) return;
        Player player = (Player) event.getPlayer();
        CompactOakSession.OakRef ref = CompactOakSession.getSession(player);
        if (ref == null) return;
        ItemStack oak = ref.getOak(player);
        CompactOakSession.clearSession(player);
        String oakId = getOakId(oak);
        if (oakId == null) return;

        updateOakMeta(oak);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.1f);
    }

    @EventHandler
    public void onOakClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(OAK_TITLE)) return;
        event.setCancelled(true);

        if (event.getRawSlot() == 13) {
            event.getWhoClicked().closeInventory();
            Player player = (Player) event.getWhoClicked();
            ItemStack loreBook = getWisdomBook();
            openBook(player, loreBook);
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.1f);
        }
    }

    @EventHandler
    public void onOakDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(OAK_TITLE)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onOakDespawn(ItemDespawnEvent event) {
        ItemStack stack = event.getEntity().getItemStack();
        if (!isCompactOak(stack)) return;
        String oakId = getOakId(stack);
        if (oakId == null) return;
        CompactOakSession.deleteOak(oakId);
    }

    @EventHandler
    public void onOakBurn(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Item)) return;
        ItemStack stack = ((Item) event.getEntity()).getItemStack();
        if (!isCompactOak(stack)) return;
        String oakId = getOakId(stack);
        if (oakId == null) return;
        CompactOakSession.deleteOak(oakId);
    }

    public static void registerCustomRecipes(Plugin plugin, boolean enable) {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        List<NamespacedKey> removeKeys = new ArrayList<>();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof ShapedRecipe sr && sr.getResult().getType() == Material.BUNDLE) {
                removeKeys.add(sr.getKey());
            }
        }
        for (NamespacedKey k : removeKeys) Bukkit.removeRecipe(k);

        if (!enable) return;

        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, "compact_oak"), createCompactOak());
        recipe.addIngredient(Material.BUNDLE);
        recipe.addIngredient(Material.CHEST);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onOakCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || !isCompactOak(result)) return;
        ItemStack newOak = createCompactOak();
        event.getInventory().setResult(newOak);
    }

    public static ItemStack getWisdomBook() {
        ItemStack wisdomBook = new ItemStack(Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta bookMeta = (org.bukkit.inventory.meta.BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        bookMeta.setTitle(ChatColor.GRAY + "Forest Wisdom");
        bookMeta.setAuthor(ChatColor.GRAY + "Forest Spirits");
        bookMeta.setDisplayName(ChatColor.GRAY + "Forest Wisdom");
        bookMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "The wood pulses with ancient energy.",
                ChatColor.GRAY + "Some say it can even upgrade the",
                ChatColor.GRAY + "legendary Wood Chopper..."
        ));
        bookMeta.setPages(Arrays.asList(
                ChatColor.GRAY + "Compact Oak\n\n" +
                        ChatColor.GRAY + "This wood is denser and stronger than any other, pulsing with mysterious power.\n\n" +
                        ChatColor.GRAY + "Whispers in the forest tell of master lumberjacks using it to upgrade their Wood Choppers, unlocking new potential.\n\n" +
                        ChatColor.GRAY + "Its true secrets are known only to the wise."
        ));
        wisdomBook.setItemMeta(bookMeta);
        return wisdomBook;
    }

    public static void openBook(Player player, ItemStack book) {
        player.openBook(book);
    }
}