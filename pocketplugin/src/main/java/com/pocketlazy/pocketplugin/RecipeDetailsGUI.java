package com.pocketlazy.pocketplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.pocketlazy.pocketplugin.forestitems.TreecapitatorAxesListener;
import com.pocketlazy.pocketplugin.forestitems.CompactOakListener;
public class RecipeDetailsGUI implements Listener {

    private static final ItemStack FILLER = createFiller();

    private static ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        meta.setCustomModelData(1); // Optional: Makes it unstack with player glass panes
        glass.setItemMeta(meta);
        return glass;
    }

    private static void fillEmpty(Inventory gui) {
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                gui.setItem(i, FILLER);
            }
        }
    }

    public static void openPouchRecipe(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.YELLOW + "Storage Pouch Recipe");
        gui.setItem(11, new ItemStack(Material.BUNDLE));
        gui.setItem(13, new ItemStack(Material.CHEST));
        gui.setItem(15, StoragePouchListener.createPouch());
        fillEmpty(gui);
        player.openInventory(gui);
    }

    public static void openWoodenChopperRecipe(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.YELLOW + "Wooden Chopper Recipe");
        ItemStack oakLog = new ItemStack(Material.OAK_LOG);
        ItemStack woodenAxe = new ItemStack(Material.WOODEN_AXE);
        ItemStack woodenChopper = CustomItems.getWoodenChopper();
        gui.setItem(10, oakLog);
        gui.setItem(11, oakLog);
        gui.setItem(12, oakLog);
        gui.setItem(19, oakLog);
        gui.setItem(20, woodenAxe);
        gui.setItem(21, oakLog);
        gui.setItem(16, oakLog);
        gui.setItem(24, woodenChopper);
        fillEmpty(gui);
        player.openInventory(gui);
    }

    public static void openLumberAxeRecipe(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GREEN + "Lumber Axe Recipe");
        ItemStack compact = CompactOakListener.createCompactOak();
        ItemStack woodenChopper = CustomItems.getWoodenChopper();
        ItemStack lumberAxe = CustomItems.getLumberAxe(0);
        gui.setItem(10, compact);
        gui.setItem(11, compact);
        gui.setItem(12, compact);
        gui.setItem(19, compact);
        gui.setItem(20, woodenChopper);
        gui.setItem(21, compact);
        gui.setItem(16, compact);
        gui.setItem(24, lumberAxe);
        fillEmpty(gui);
        player.openInventory(gui);
    }

    public static void openTreecapRecipe(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GREEN + "Treecapitator Recipe");
        ItemStack gold = new ItemStack(Material.GOLD_BLOCK);
        ItemStack apple = new ItemStack(Material.GOLDEN_APPLE);
        ItemStack lumberAxe = CustomItems.getLumberAxe(0);
        ItemStack treecap = TreecapitatorAxesListener.createTreecapitator();
        gui.setItem(10, gold);
        gui.setItem(11, apple);
        gui.setItem(12, gold);
        gui.setItem(19, apple);
        gui.setItem(20, lumberAxe);
        gui.setItem(21, apple);
        gui.setItem(16, gold);
        gui.setItem(24, treecap);
        fillEmpty(gui);
        player.openInventory(gui);
    }

    public static void openCompactOakRecipe(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.YELLOW + "Compact Oak Recipe");
        ItemStack oakLog = new ItemStack(Material.OAK_LOG);
        ItemStack compact = CompactOakListener.createCompactOak();
        // Centered 3x3
        gui.setItem(10, oakLog);
        gui.setItem(11, oakLog);
        gui.setItem(12, oakLog);
        gui.setItem(19, oakLog);
        gui.setItem(20, oakLog);
        gui.setItem(21, oakLog);
        gui.setItem(16, oakLog);
        gui.setItem(24, compact);
        fillEmpty(gui);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.endsWith("Recipe")) {
            event.setCancelled(true); // Lock all items in detail GUIs
        }
    }
}