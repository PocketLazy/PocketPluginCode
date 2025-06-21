package com.pocketlazy.pocketplugin;

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

public class StoragePouchListener implements Listener {
    public static final int BUNDLE_SIZE = 9;
    // CHANGED: Displayed GUI title is now "Storage Pouch"
    private static final String BUNDLE_TITLE = ChatColor.GOLD + "Storage Pouch";
    private static final NamespacedKey POUCH_UUID_KEY = new NamespacedKey("pocketplugin", "pouch_uuid");

    // For best compatibility, use the Mojang "MHF_Chest" player for chest texture
    public static final String CHEST_OWNER = "MHF_Chest";

    public static boolean isPouch(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PLAYER_HEAD) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(POUCH_UUID_KEY, PersistentDataType.STRING);
    }

    public static ItemStack createPouch() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Storage Pouch");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A magical storage pouch.",
                ChatColor.DARK_GRAY + "Right-click to open"
        ));
        // Set the head owner to MHF_Chest
        try {
            OfflinePlayer chestPlayer = Bukkit.getOfflinePlayer(CHEST_OWNER);
            meta.setOwningPlayer(chestPlayer);
        } catch (Throwable ignored) {}
        String uuid = UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(POUCH_UUID_KEY, PersistentDataType.STRING, uuid);
        head.setItemMeta(meta);
        return head;
    }

    public static String getOrAssignPouchId(ItemStack pouch) {
        if (!isPouch(pouch)) return null;
        ItemMeta meta = pouch.getItemMeta();
        String id = meta.getPersistentDataContainer().get(POUCH_UUID_KEY, PersistentDataType.STRING);
        if (id == null) {
            id = UUID.randomUUID().toString();
            meta.getPersistentDataContainer().set(POUCH_UUID_KEY, PersistentDataType.STRING, id);
            pouch.setItemMeta(meta);
        }
        return id;
    }

    public static String getPouchId(ItemStack pouch) {
        if (!isPouch(pouch)) return null;
        ItemMeta meta = pouch.getItemMeta();
        return meta.getPersistentDataContainer().get(POUCH_UUID_KEY, PersistentDataType.STRING);
    }

    public static void updatePouchMeta(ItemStack pouch) {
        if (!isPouch(pouch)) return;
        SkullMeta meta = (SkullMeta) pouch.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Storage Pouch");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A magical storage pouch.",
                ChatColor.DARK_GRAY + "Right-click to open"
        ));
        try {
            OfflinePlayer chestPlayer = Bukkit.getOfflinePlayer(CHEST_OWNER);
            meta.setOwningPlayer(chestPlayer);
        } catch (Throwable ignored) {}
        pouch.setItemMeta(meta);
    }

    // --- Listeners ---

    @EventHandler
    public void onPouchOpen(PlayerInteractEvent event) {
        // Only allow opening from main hand, not offhand
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (!isPouch(item)) return;
        event.setCancelled(true);

        String pouchId = getOrAssignPouchId(item);
        updatePouchMeta(item);

        Inventory inv = Bukkit.createInventory(event.getPlayer(), BUNDLE_SIZE, BUNDLE_TITLE);
        ItemStack[] contents = StoragePouchSession.loadPouch(pouchId);
        for (int i = 0; i < BUNDLE_SIZE; i++) {
            inv.setItem(i, contents[i]);
        }
        event.getPlayer().openInventory(inv);
        StoragePouchSession.setSession(event.getPlayer(), item);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ITEM_BUNDLE_INSERT, 1f, 1f);
    }

    @EventHandler
    public void onPouchClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(BUNDLE_TITLE)) return;
        Player player = (Player) event.getPlayer();
        StoragePouchSession.PouchRef ref = StoragePouchSession.getSession(player);
        if (ref == null) return;
        ItemStack pouch = ref.getPouch(player);
        StoragePouchSession.clearSession(player);
        String pouchId = getPouchId(pouch);
        if (pouchId == null) return;

        Inventory inv = event.getInventory();
        ItemStack[] save = new ItemStack[BUNDLE_SIZE];
        for (int i = 0; i < BUNDLE_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            save[i] = (it != null && it.getType() != Material.AIR) ? it.clone() : null;
        }
        StoragePouchSession.savePouch(pouchId, save);
        updatePouchMeta(pouch);
        player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 1f, 1.1f);
    }

    @EventHandler
    public void onPouchClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(BUNDLE_TITLE)) return;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if ((cursor != null && isPouch(cursor)) ||
                (current != null && isPouch(current) && event.isShiftClick())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPouchDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(BUNDLE_TITLE)) return;
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < BUNDLE_SIZE) {
                ItemStack is = event.getOldCursor();
                if (is != null && isPouch(is)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPouchDespawn(ItemDespawnEvent event) {
        ItemStack stack = event.getEntity().getItemStack();
        if (!isPouch(stack)) return;
        String pouchId = getPouchId(stack);
        if (pouchId == null) return;
        ItemStack[] contents = StoragePouchSession.loadPouch(pouchId);
        for (ItemStack drop : contents) {
            if (drop != null && drop.getType() != Material.AIR) {
                event.getLocation().getWorld().dropItemNaturally(event.getLocation(), drop);
            }
        }
        StoragePouchSession.deletePouch(pouchId);
    }

    @EventHandler
    public void onPouchBurn(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Item)) return;
        ItemStack stack = ((Item) event.getEntity()).getItemStack();
        if (!isPouch(stack)) return;
        String pouchId = getPouchId(stack);
        if (pouchId == null) return;
        ItemStack[] contents = StoragePouchSession.loadPouch(pouchId);
        for (ItemStack drop : contents) {
            if (drop != null && drop.getType() != Material.AIR) {
                event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), drop);
            }
        }
        StoragePouchSession.deletePouch(pouchId);
    }

    // --- Pouch Recipe Registration (bundle + chest -> pouch head) ---

    public static void registerCustomRecipes(Plugin plugin, boolean enable) {
        // Remove all vanilla bundle recipes (optional)
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

        // Add our pouch recipe: shapeless (bundle + chest -> pouch head)
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, "storage_pouch"), createPouch());
        recipe.addIngredient(Material.BUNDLE);
        recipe.addIngredient(Material.CHEST);
        Bukkit.addRecipe(recipe);
    }

    // --- Ensure every crafted pouch has a unique UUID ---
    @EventHandler
    public void onPouchCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || !isPouch(result)) return;

        // Give a fresh pouch with a unique UUID for every craft
        ItemStack newPouch = createPouch();
        event.getInventory().setResult(newPouch);
    }
}