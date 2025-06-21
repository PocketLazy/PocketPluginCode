package com.pocketlazy.pocketplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EventListener implements Listener {
    private final PluginState state;
    private final Map<UUID, Integer> lastSeconds = new HashMap<>();

    public EventListener(PluginState state) {
        this.state = state;
        // Start combat timer action bar updates
        new BukkitRunnable() {
            public void run() {
                long now = System.currentTimeMillis();
                for (UUID uuid : state.getCombatTaggedPlayersSet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    if (!state.isCombatTagged(uuid)) {
                        state.untagPlayer(uuid);
                        lastSeconds.remove(uuid);
                        p.sendActionBar(ChatColor.GREEN + "You are no longer in combat!");
                    } else {
                        long end = state.getCombatTagEnd(uuid);
                        int sec = (int) ((end - now) / 1000) + 1;
                        if (!lastSeconds.containsKey(uuid) || !lastSeconds.get(uuid).equals(sec)) {
                            lastSeconds.put(uuid, sec);
                            p.sendActionBar(fancyActionBar(sec, state.getCombatTimerSeconds()));
                        }
                    }
                }
                lastSeconds.keySet().removeIf(uuid -> !state.isCombatTagged(uuid));
            }
        }.runTaskTimer(PocketPlugin.getInstance(), 0, 20);
    }

    private String fancyActionBar(int secondsLeft, int maxSeconds) {
        int totalBars = 16;
        int greenBars = (int) Math.round(((double) secondsLeft / Math.max(1, maxSeconds)) * totalBars);
        greenBars = Math.max(0, Math.min(greenBars, totalBars));
        int redBars = totalBars - greenBars;

        StringBuilder bar = new StringBuilder(ChatColor.DARK_GRAY + "[");
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < greenBars; i++) bar.append("■");
        bar.append(ChatColor.RED);
        for (int i = 0; i < redBars; i++) bar.append("■");
        bar.append(ChatColor.DARK_GRAY + "]");

        return ChatColor.BOLD + "" + ChatColor.GOLD + "⚔ " + ChatColor.RED + "Combat Timer: "
                + ChatColor.WHITE + secondsLeft + "s " + bar;
    }

    // TNT minecart block break prevention
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTNTMinecartExplode(EntityExplodeEvent e ) {
        if (e.getEntity().getType() != EntityType.TNT_MINECART) return;

        // Always block if cartProt is enabled
        if (state.cartProt.get()) {
            e.blockList().clear();
            return;
        }

        // If cartProtCombatOnly is enabled, apply cartProt logic for all TNT minecart explosions when any player is in combat
        if (state.cartProtCombatOnly.get()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (state.isCombatTagged(p.getUniqueId())) {
                    e.blockList().clear();
                    return;
                }
            }
        }
    }

    // End Crystal block break/damage prevention
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrystalExplode(EntityExplodeEvent e) {
        if (state.crystalProt.get() && e.getEntity().getType() == EntityType.END_CRYSTAL) {
            e.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrystalDamage(EntityDamageByEntityEvent e) {
        if (state.crystalProt.get() && e.getDamager().getType() == EntityType.END_CRYSTAL) {
            e.setCancelled(true);
        }
    }

    // Combat tagging logic (pairing)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (!state.combatProt.get()) return;
        Player victim = null, damager = null;
        if (e.getEntity() instanceof Player p1) victim = p1;
        if (e.getDamager() instanceof Player p2) damager = p2;
        else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p3)
            damager = p3;
        if (victim != null && damager != null && !victim.equals(damager)) {
            long until = System.currentTimeMillis() + state.getCombatTimerSeconds() * 1000L;
            state.tagPlayer(victim.getUniqueId(), damager.getUniqueId(), until);
            state.tagPlayer(damager.getUniqueId(), victim.getUniqueId(), until);
            victim.sendActionBar(fancyActionBar(state.getCombatTimerSeconds(), state.getCombatTimerSeconds()));
            damager.sendActionBar(fancyActionBar(state.getCombatTimerSeconds(), state.getCombatTimerSeconds()));
        }
    }

    // Remove combat tag on death & clear combat pairs for their opponent
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        state.untagPlayer(id);
        lastSeconds.remove(id);

        // Remove combat tag for anyone who was paired with this player
        for (UUID paired : state.getPairedWith(id)) {
            Player pairedPlayer = Bukkit.getPlayer(paired);
            if (pairedPlayer != null) {
                state.untagPlayer(paired);
                lastSeconds.remove(paired);
                pairedPlayer.sendActionBar(ChatColor.GREEN + "You are no longer in combat!");
            }
        }
    }

    // Kill player if logout while combat tagged, and clear their opponent's tag
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (!state.combatProt.get()) return;
        UUID id = e.getPlayer().getUniqueId();
        if (state.isCombatTagged(id)) {
            Player p = e.getPlayer();
            p.setHealth(0.0);
            Bukkit.getWorlds().get(0).strikeLightningEffect(p.getLocation());
            state.untagPlayer(id);
            lastSeconds.remove(id);
            // Remove combat tag for paired player
            UUID paired = state.getCombatOpponent(id);
            if (paired != null) {
                Player pairedPlayer = Bukkit.getPlayer(paired);
                if (pairedPlayer != null) {
                    state.untagPlayer(paired);
                    lastSeconds.remove(paired);
                    pairedPlayer.sendActionBar(ChatColor.GREEN + "You are no longer in combat!");
                }
            }
        }
    }

    // Block elytra gliding and rocket boosting during combat
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!state.elytraCombat.get()) return;
        UUID id = player.getUniqueId();
        if (state.isCombatTagged(id) && event.isGliding()) {
            event.setCancelled(true);
            player.setGliding(false);
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[Pocket] You cannot use elytra while in combat!");
        }
    }

    // Block elytra takeoff in combat (spacebar elytra launch)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onElytra(PlayerToggleFlightEvent e) {
        if (!state.elytraCombat.get()) return;
        Player player = e.getPlayer();
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || chest.getType() != Material.ELYTRA) return;
        UUID id = player.getUniqueId();
        if (state.isCombatTagged(id)) {
            e.setCancelled(true);
            player.setGliding(false);
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[Pocket] Combat prohibits flight");
        }
    }

    // Block using fireworks to boost elytra in combat
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!state.elytraCombat.get()) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        // Only care if in combat
        if (!state.isCombatTagged(id)) return;

        // Only care about right-click
        Action action = event.getAction();
        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) return;

        // Only block if player is gliding and using a firework
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FIREWORK_ROCKET && player.isGliding()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[Pocket] You cannot boost with rockets while in combat!");
        }
    }
}