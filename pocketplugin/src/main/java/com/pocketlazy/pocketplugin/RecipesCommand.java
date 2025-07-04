package com.pocketlazy.pocketplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RecipesCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            RecipesCategoryGUI.open(player); // Open the category menu
        } else {
            sender.sendMessage(ChatColor.RED + "Players only.");
        }
        return true;
    }
}