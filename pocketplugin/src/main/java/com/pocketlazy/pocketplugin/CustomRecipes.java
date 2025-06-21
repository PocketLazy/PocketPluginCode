package com.pocketlazy.pocketplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import com.pocketlazy.pocketplugin.forestitems.CompactOakListener;

public class CustomRecipes {
    // Register custom Compact Oak recipe via crafting event
    public static void registerCompactOakListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPrepareItemCraft(PrepareItemCraftEvent event) {
                ItemStack[] matrix = event.getInventory().getMatrix();
                int oakLogCount = 0;
                boolean hasOther = false;
                for (ItemStack stack : matrix) {
                    if (stack == null || stack.getType() == Material.AIR) continue;
                    if (stack.getType() == Material.OAK_LOG) {
                        oakLogCount += stack.getAmount();
                    } else {
                        hasOther = true;
                        break;
                    }
                }
                if (!hasOther && oakLogCount == 160) {
                    event.getInventory().setResult(CompactOakListener.createCompactOak());
                } else if (event.getInventory().getResult() != null &&
                        event.getInventory().getResult().isSimilar(CompactOakListener.createCompactOak())) {
                    // Only clear the result if it would have been the Compact Oak
                    event.getInventory().setResult(null);
                }
            }
        }, plugin);
    }
}