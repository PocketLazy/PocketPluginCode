package com.pocketlazy.pocketplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.pocketlazy.pocketplugin.forestitems.TreecapitatorAxesListener;
import com.pocketlazy.pocketplugin.forestitems.CompactOakListener;
public class RecipesListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // Categories menu
        if (title.equals(ChatColor.DARK_AQUA + "Recipe Categories")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            if (event.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
                RecipesOverviewGUI.open(player, RecipesOverviewGUI.Type.UTILITY);
            } else if (event.getCurrentItem().getType() == Material.OAK_LOG) {
                RecipesOverviewGUI.open(player, RecipesOverviewGUI.Type.MATERIAL);
            }
            return;
        }

        // Utility menu
        if (title.equals(ChatColor.GOLD + "Utility Recipes")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;
            if (clicked.isSimilar(StoragePouchListener.createPouch())) {
                RecipeDetailsGUI.openPouchRecipe(player);
            } else if (clicked.isSimilar(TreecapitatorAxesListener.createTreecapitator(0))) {
                RecipeDetailsGUI.openTreecapRecipe(player);
            } else if (clicked.isSimilar(TreecapitatorAxesListener.getLumberAxe(0))) {
                RecipeDetailsGUI.openLumberAxeRecipe(player);
            } else if (clicked.isSimilar(TreecapitatorAxesListener.getWoodenChopper(0))) {
                RecipeDetailsGUI.openWoodenChopperRecipe(player);
            }
            return;
        }

        // Material menu
        if (title.equals(ChatColor.GREEN + "Material Recipes")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;
            if (clicked.isSimilar(CompactOakListener.createCompactOak())) {
                RecipeDetailsGUI.openCompactOakRecipe(player);
            }
            return;
        }

        // Details (prevent taking)
        if (
                title.equals(ChatColor.YELLOW + "Storage Pouch Recipe") ||
                        title.equals(ChatColor.GREEN + "Treecapitator Recipe") ||
                        title.equals(ChatColor.YELLOW + "Compact Oak Recipe") ||
                        title.equals(ChatColor.YELLOW + "Wooden Chopper Recipe") ||
                        title.equals(ChatColor.GREEN + "Lumber Axe Recipe")
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe().getResult();
        if (result == null) return;

        // Only force update for Compact Oak
        if (result.isSimilar(CompactOakListener.createCompactOak())) {
            ItemStack expected = CompactOakListener.createCompactOak();
            // Use the plugin instance from your main class (adjust if needed)
            org.bukkit.plugin.Plugin plugin = PocketPlugin.getInstance();
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack != null && stack.isSimilar(expected)) {
                        player.getInventory().setItem(i, stack);
                    }
                }
                player.updateInventory();
            }, 2L);
        }
    }
}
