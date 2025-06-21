package com.pocketlazy.pocketplugin;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PluginState {
    public final AtomicBoolean cartProt = new AtomicBoolean(false);
    public final AtomicBoolean cartProtCombatOnly = new AtomicBoolean(false);
    public final AtomicBoolean crystalProt = new AtomicBoolean(false);
    public final AtomicBoolean combatProt = new AtomicBoolean(false);
    public final AtomicBoolean elytraCombat = new AtomicBoolean(false);

    private int combatTimerSeconds = 15;

    // Player UUID -> time when combat tag ends (ms since epoch)
    private final Map<UUID, Long> combatTaggedPlayers = new HashMap<>();
    // Player UUID -> UUID of who they are in combat with
    private final Map<UUID, UUID> combatPairs = new HashMap<>();
    private final Object combatLock = new Object();

    public int getCombatTimerSeconds() {
        return combatTimerSeconds;
    }

    public void setCombatTimerSeconds(int seconds) {
        this.combatTimerSeconds = seconds;
    }

    /** Safely add or update a player's combat tag and their opponent. */
    public void tagPlayer(UUID player, UUID opponent, long expireMillis) {
        synchronized (combatLock) {
            combatTaggedPlayers.put(player, expireMillis);
            combatPairs.put(player, opponent);
        }
    }

    /** Safely clear a player's combat tag and their opponent. */
    public void untagPlayer(UUID player) {
        synchronized (combatLock) {
            combatTaggedPlayers.remove(player);
            combatPairs.remove(player);
        }
    }

    /** Check if the player is combat tagged. */
    public boolean isCombatTagged(UUID player) {
        synchronized (combatLock) {
            Long end = combatTaggedPlayers.get(player);
            return end != null && end > System.currentTimeMillis();
        }
    }

    /** Get the combat tag end time for a player (or -1 if not tagged). */
    public long getCombatTagEnd(UUID player) {
        synchronized (combatLock) {
            Long end = combatTaggedPlayers.get(player);
            return end == null ? -1 : end;
        }
    }

    /** Get the opponent for a tagged player. */
    public UUID getCombatOpponent(UUID player) {
        synchronized (combatLock) {
            return combatPairs.get(player);
        }
    }

    /** Remove expired combat tags and sync pairs. */
    public void cleanCombatTags() {
        synchronized (combatLock) {
            long now = System.currentTimeMillis();
            combatTaggedPlayers.entrySet().removeIf(e -> e.getValue() <= now);
            combatPairs.keySet().removeIf(uuid -> !combatTaggedPlayers.containsKey(uuid));
        }
    }

    /** Get a copy of all combat tagged player UUIDs. */
    public Set<UUID> getCombatTaggedPlayersSet() {
        synchronized (combatLock) {
            return new HashSet<>(combatTaggedPlayers.keySet());
        }
    }

    /**
     * Get all players paired with this player (reverse mapping, may be empty).
     * Useful for removing tags on death or quit for any player paired with this one.
     */
    public Set<UUID> getPairedWith(UUID player) {
        synchronized (combatLock) {
            Set<UUID> result = new HashSet<>();
            for (Map.Entry<UUID, UUID> entry : combatPairs.entrySet()) {
                if (entry.getValue().equals(player)) result.add(entry.getKey());
            }
            return result;
        }
    }
}