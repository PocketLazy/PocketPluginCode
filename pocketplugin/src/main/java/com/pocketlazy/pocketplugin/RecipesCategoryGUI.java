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

public class RecipesCategoryGUI implements Listener {
    public static final String TITLE = ChatColor.DARK_AQUA + "Recipe Categories";

    private static final ItemStack FILLER = createFiller();

    private static ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        meta.setCustomModelData(1); // Optional
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

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, TITLE);

        ItemStack util = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta utilMeta = util.getItemMeta();
        utilMeta.setDisplayName(ChatColor.GOLD + "Utility");
        util.setItemMeta(utilMeta);

        ItemStack mat = new ItemStack(Material.OAK_LOG);
        ItemMeta matMeta = mat.getItemMeta();
        matMeta.setDisplayName(ChatColor.GREEN + "Material");
        mat.setItemMeta(matMeta);

        gui.setItem(3, util);
        gui.setItem(5, mat);

        fillEmpty(gui);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory inv = event.getInventory();
        if (!event.getView().getTitle().equals(TITLE)) return;

        event.setCancelled(true); // Lock the items

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();
        if (clicked.getType() == Material.REDSTONE_BLOCK) {
            RecipesOverviewGUI.open(player, RecipesOverviewGUI.Type.UTILITY);
        } else if (clicked.getType() == Material.OAK_LOG) {
            RecipesOverviewGUI.open(player, RecipesOverviewGUI.Type.MATERIAL);
        }
    }
}