package com.pocketlazy.pocketplugin;

import com.pocketlazy.pocketplugin.forestitems.CompactOakListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.pocketlazy.pocketplugin.forestitems.TreecapitatorAxesListener;

public class PocketPlugin extends JavaPlugin {
    private static PocketPlugin instance;
    private PluginState state;
    private boolean pouchRecipeEnabled = true; // Default is enabled
    private boolean treecapRecipeEnabled = true; // Default is enabled
    private boolean compactOakEnabled = true; // Default: enabled

    public static PocketPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        state = new PluginState();

        // --- FIX: Initialize Treecapitator Keys FIRST! ---
        TreecapitatorAxesListener.initializeKeys(this);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new EventListener(state), this);
        Bukkit.getPluginManager().registerEvents(new StoragePouchListener(), this);
        Bukkit.getPluginManager().registerEvents(new TreecapitatorAxesListener(), this);
        Bukkit.getPluginManager().registerEvents(new RecipesCategoryGUI(), this);
        Bukkit.getPluginManager().registerEvents(new RecipesOverviewGUI(), this);
        Bukkit.getPluginManager().registerEvents(new RecipeDetailsGUI(), this);
        Bukkit.getPluginManager().registerEvents(new RecipesListener(), this);
        Bukkit.getPluginManager().registerEvents(new CustomItems(), this);

        // Enable Compact Oak (forestitems)
        Bukkit.getPluginManager().registerEvents(new CompactOakListener(), this);
        CompactOakListener.registerCustomRecipes(this, compactOakEnabled);

        // (Optional: Remove this line if you don't want the old version)
        // CustomRecipes.registerCompactOakListener(this);

        // Register custom axes recipes (vanilla, for base recipes)
        TreecapitatorAxesListener.registerWoodenChopperRecipe(this, true);
        TreecapitatorAxesListener.registerLumberAxeRecipe(this, true);
        TreecapitatorAxesListener.registerTreecapRecipe(this, treecapRecipeEnabled);

        // Register /pocket command (if defined in plugin.yml)
        if (this.getCommand("pocket") != null) {
            PocketCommand pocketCommand = new PocketCommand(state, this);
            this.getCommand("pocket").setExecutor(pocketCommand);
            this.getCommand("pocket").setTabCompleter(pocketCommand);
        }

        // Register custom pouch recipes at startup (enable/disable support)
        StoragePouchListener.registerCustomRecipes(this, pouchRecipeEnabled);

        getLogger().info("PocketPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PocketPlugin disabled!");
    }

    public PluginState getPluginState() {
        return state;
    }

    // --- Support for recipe toggle and PocketCommand ---
    public boolean isPouchRecipeEnabled() {
        return pouchRecipeEnabled;
    }

    public void setPouchRecipeEnabled(boolean value) {
        this.pouchRecipeEnabled = value;
    }

    public boolean isTreecapRecipeEnabled() {
        return treecapRecipeEnabled;
    }

    public void setTreecapRecipeEnabled(boolean value) {
        this.treecapRecipeEnabled = value;
    }
}