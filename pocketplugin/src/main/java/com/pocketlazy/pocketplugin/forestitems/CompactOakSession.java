package com.pocketlazy.pocketplugin.forestitems;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class CompactOakSession {
    private static final int OAK_SIZE = 27;
    private static final Map<UUID, OakRef> sessionMap = new HashMap<>();

    public static class OakRef {
        private final UUID playerId;
        private final String oakId;
        private final int slot;
        private final boolean offhand;

        public OakRef(UUID playerId, String oakId, int slot, boolean offhand) {
            this.playerId = playerId;
            this.oakId = oakId;
            this.slot = slot;
            this.offhand = offhand;
        }

        // Find actual ItemStack in inventory (so updates are saved!)
        public ItemStack getOak(Player player) {
            if (offhand) {
                ItemStack off = player.getInventory().getItemInOffHand();
                if (CompactOakListener.isCompactOak(off) && CompactOakListener.getOakId(off).equals(oakId)) {
                    return off;
                }
            } else {
                ItemStack main = player.getInventory().getItem(slot);
                if (CompactOakListener.isCompactOak(main) && CompactOakListener.getOakId(main).equals(oakId)) {
                    return main;
                }
            }
            // Fallback: search for the oak
            for (ItemStack item : player.getInventory().getContents()) {
                if (CompactOakListener.isCompactOak(item) && CompactOakListener.getOakId(item).equals(oakId)) {
                    return item;
                }
            }
            return null;
        }

        public String getOakId() {
            return oakId;
        }
    }

    public static void setSession(Player player, ItemStack oak) {
        int slot = player.getInventory().getHeldItemSlot();
        boolean offhand = false;
        String oakId = CompactOakListener.getOakId(oak);
        if (oakId == null) return;
        // If the hand item is not the oak, check offhand
        if (!CompactOakListener.isCompactOak(player.getInventory().getItemInMainHand())
                && CompactOakListener.isCompactOak(player.getInventory().getItemInOffHand())
                && CompactOakListener.getOakId(player.getInventory().getItemInOffHand()).equals(oakId)) {
            offhand = true;
            slot = -1;
        }
        sessionMap.put(player.getUniqueId(), new OakRef(player.getUniqueId(), oakId, slot, offhand));
    }

    public static OakRef getSession(Player player) {
        return sessionMap.get(player.getUniqueId());
    }

    public static void clearSession(Player player) {
        sessionMap.remove(player.getUniqueId());
    }

    public static void deleteOak(String oakId) {
        // If you ever add per-oak file storage, clean up here
        File file = new File(Bukkit.getPluginManager().getPlugin("PocketPlugin").getDataFolder(), "compact_oaks/" + oakId + ".yml");
        if (file.exists()) file.delete();
    }
}