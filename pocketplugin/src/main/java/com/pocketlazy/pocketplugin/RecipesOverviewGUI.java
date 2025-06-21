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
public class RecipesOverviewGUI implements Listener {

    private static final ItemStack FILLER = createFiller();

    private static ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        meta.setCustomModelData(1);
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

    public static void open(Player player, Type type) {
        Inventory gui = Bukkit.createInventory(null, 27, type == Type.UTILITY ? ChatColor.GOLD + "Utility Recipes" : ChatColor.GREEN + "Material Recipes");

        if (type == Type.UTILITY) {
            gui.setItem(10, StoragePouchListener.createPouch());
            gui.setItem(12, TreecapitatorAxesListener.createTreecapitator());
            gui.setItem(14, CustomItems.getLumberAxe(0));
            gui.setItem(16, CustomItems.getWoodenChopper());
        } else {
            gui.setItem(11, CompactOakListener.createCompactOak());
        }

        fillEmpty(gui);
        player.openInventory(gui);
    }

    public enum Type { UTILITY, MATERIAL }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.GOLD + "Utility Recipes") && !title.equals(ChatColor.GREEN + "Material Recipes")) return;

        event.setCancelled(true); // Lock the items

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();

        if (title.equals(ChatColor.GOLD + "Utility Recipes")) {
            if (clicked.isSimilar(StoragePouchListener.createPouch())) {
                RecipeDetailsGUI.openPouchRecipe(player);
            } else if (clicked.isSimilar(TreecapitatorAxesListener.createTreecapitator())) {
                RecipeDetailsGUI.openTreecapRecipe(player);
            } else if (clicked.isSimilar(CustomItems.getLumberAxe(0))) {
                RecipeDetailsGUI.openLumberAxeRecipe(player);
            } else if (clicked.isSimilar(CustomItems.getWoodenChopper())) {
                RecipeDetailsGUI.openWoodenChopperRecipe(player);
            }
        } else if (title.equals(ChatColor.GREEN + "Material Recipes")) {
            if (clicked.isSimilar(CompactOakListener.createCompactOak())) {
                RecipeDetailsGUI.openCompactOakRecipe(player);
            }
        }
    }
}