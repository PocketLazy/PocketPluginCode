package com.pocketlazy.pocketplugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StoragePouchSession {
    private static final int POUCH_SIZE = 9;
    private static final Map<UUID, PouchRef> sessionMap = new HashMap<>();

    public static class PouchRef {
        private final UUID playerId;
        private final String pouchId;
        private final int slot;
        private final boolean offhand;

        public PouchRef(UUID playerId, String pouchId, int slot, boolean offhand) {
            this.playerId = playerId;
            this.pouchId = pouchId;
            this.slot = slot;
            this.offhand = offhand;
        }

        // Find actual ItemStack in inventory (so updates are saved!)
        public ItemStack getPouch(Player player) {
            if (offhand) {
                ItemStack off = player.getInventory().getItemInOffHand();
                if (StoragePouchListener.isPouch(off) && StoragePouchListener.getPouchId(off).equals(pouchId)) {
                    return off;
                }
            } else {
                ItemStack main = player.getInventory().getItem(slot);
                if (StoragePouchListener.isPouch(main) && StoragePouchListener.getPouchId(main).equals(pouchId)) {
                    return main;
                }
            }
            // Fallback: search for the pouch
            for (ItemStack item : player.getInventory().getContents()) {
                if (StoragePouchListener.isPouch(item) && StoragePouchListener.getPouchId(item).equals(pouchId)) {
                    return item;
                }
            }
            return null;
        }

        public String getPouchId() {
            return pouchId;
        }
    }

    public static void setSession(Player player, ItemStack pouch) {
        int slot = player.getInventory().getHeldItemSlot();
        boolean offhand = false;
        String pouchId = StoragePouchListener.getPouchId(pouch);
        if (pouchId == null) return;
        // If the hand item is not the pouch, check offhand
        if (!StoragePouchListener.isPouch(player.getInventory().getItemInMainHand())
                && StoragePouchListener.isPouch(player.getInventory().getItemInOffHand())
                && StoragePouchListener.getPouchId(player.getInventory().getItemInOffHand()).equals(pouchId)) {
            offhand = true;
            slot = -1;
        }
        sessionMap.put(player.getUniqueId(), new PouchRef(player.getUniqueId(), pouchId, slot, offhand));
    }

    public static PouchRef getSession(Player player) {
        return sessionMap.get(player.getUniqueId());
    }

    public static void clearSession(Player player) {
        sessionMap.remove(player.getUniqueId());
    }

    // File logic
    private static File getPouchFile(String pouchId) {
        File dir = new File(Bukkit.getPluginManager().getPlugin("PocketPlugin").getDataFolder(), "bundles");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, pouchId + ".yml");
    }

    public static ItemStack[] loadPouch(String pouchId) {
        File file = getPouchFile(pouchId);
        ItemStack[] arr = new ItemStack[POUCH_SIZE];
        if (file.exists()) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            for (int i = 0; i < POUCH_SIZE; i++) {
                ItemStack is = yml.getItemStack("slot" + i);
                arr[i] = (is != null) ? is : null;
            }
        }
        return arr;
    }

    public static void savePouch(String pouchId, ItemStack[] contents) {
        YamlConfiguration yml = new YamlConfiguration();
        for (int i = 0; i < POUCH_SIZE; i++) {
            yml.set("slot" + i, contents[i]);
        }
        try {
            yml.save(getPouchFile(pouchId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deletePouch(String pouchId) {
        File file = getPouchFile(pouchId);
        if (file.exists()) file.delete();
    }
}