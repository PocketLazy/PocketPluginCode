package com.pocketlazy.pocketplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.pocketlazy.pocketplugin.forestitems.TreecapitatorAxesListener;
import com.pocketlazy.pocketplugin.forestitems.CompactOakListener;
import java.util.*;

public class PocketCommand implements CommandExecutor, TabCompleter {
    private final PluginState state;
    private final Plugin plugin;

    // Add a map of all custom items and their pretty names for tab completion and listing
    private static final Map<String, String> CUSTOM_ITEM_ALIASES = Map.ofEntries(
            Map.entry("pouch", "Storage Pouch"),
            Map.entry("wooden_chopper", "Wooden Chopper"),
            Map.entry("woodenchopper", "Wooden Chopper"),
            Map.entry("lumber_axe", "Lumber Axe"),
            Map.entry("lumberaxe", "Lumber Axe"),
            Map.entry("treecapitator", "Treecapitator"),
            Map.entry("compact_oak", "Compact Oak"),
            Map.entry("compactoak", "Compact Oak")
    );

    // Updated command tree: now give uses all available item keys for tab-complete
    private static final Map<String, List<String>> COMMAND_TREE = Map.ofEntries(
            Map.entry("cartprot", List.of("enable", "disable", "status", "combatonly")),
            Map.entry("crystals", List.of("enable", "disable", "status")),
            Map.entry("combatprot", List.of("enable", "disable", "status")),
            Map.entry("ctimer", List.of("<seconds>", "reset")),
            Map.entry("elytracombat", List.of("enable", "disable", "status")),
            Map.entry("reset", List.of("ctimer")),
            Map.entry("give", new ArrayList<>(CUSTOM_ITEM_ALIASES.keySet())),
            Map.entry("help", List.of()),
            Map.entry("reload", List.of()),
            Map.entry("pouchrecipe", List.of("enable", "disable", "status")),
            Map.entry("treecaprecipe", List.of("enable", "disable", "status")),
            Map.entry("recipes", List.of()),
            Map.entry("craft", List.of())
    );

    public PocketCommand(PluginState state, Plugin plugin) {
        this.state = state;
        this.plugin = plugin;
    }

    private void send(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[Pocket] " + ChatColor.RESET + msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Only let OPs use most commands, but anyone can use /pocket recipes, /pocket craft
        String sub = (args.length > 0) ? args[0].toLowerCase(Locale.ROOT) : "";
        boolean isRecipes = sub.equals("recipes") || sub.equals("help") || sub.equals("craft") || sub.isEmpty();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.isOp() && !isRecipes) {
                send(sender, ChatColor.RED + "You must be OP to use this command.");
                return true;
            }
        } else if (!isRecipes) {
            send(sender, ChatColor.RED + "You must be OP to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(sender, args.length > 1 ? args[1] : null);
            return true;
        }

        switch (sub) {
            case "cartprot":
                handleCartProtCommand(sender, args);
                break;
            case "crystals":
                handleToggleCommand(sender, state.crystalProt, "End Crystals", args);
                break;
            case "combatprot":
                handleToggleCommand(sender, state.combatProt, "Combat Protection", args);
                break;
            case "elytracombat":
                handleToggleCommand(sender, state.elytraCombat, "Elytra Combat Restriction", args);
                break;
            case "ctimer":
                if (args.length == 2 && args[1].equalsIgnoreCase("reset")) {
                    state.setCombatTimerSeconds(15);
                    send(sender, ChatColor.GREEN + "Combat timer reset to default (15 seconds).");
                } else if (args.length == 2) {
                    try {
                        int sec = Integer.parseInt(args[1]);
                        if (sec < 1) throw new NumberFormatException();
                        state.setCombatTimerSeconds(sec);
                        send(sender, ChatColor.AQUA + "Combat timer set to " + ChatColor.YELLOW + sec + ChatColor.AQUA + " seconds.");
                    } catch (NumberFormatException e) {
                        send(sender, ChatColor.RED + "Time must be a positive integer.");
                    }
                } else {
                    send(sender, ChatColor.YELLOW + "Usage: /pocket ctimer <seconds> OR /pocket ctimer reset");
                }
                break;
            case "reset":
                if (args.length == 2 && args[1].equalsIgnoreCase("ctimer")) {
                    state.setCombatTimerSeconds(15);
                    send(sender, ChatColor.GREEN + "Combat timer reset to default (15 seconds).");
                } else {
                    send(sender, ChatColor.YELLOW + "Usage: /pocket reset ctimer");
                }
                break;
            case "give":
                handleGiveCommand(sender, args);
                break;
            case "reload":
                Bukkit.getPluginManager().disablePlugin(PocketPlugin.getInstance());
                Bukkit.getPluginManager().enablePlugin(PocketPlugin.getInstance());
                send(sender, ChatColor.GREEN + "PocketPlugin reloaded!");
                break;
            case "pouchrecipe":
                handlePouchRecipeCommand(sender, args);
                break;
            case "treecaprecipe":
                handleTreecapRecipeCommand(sender, args);
                break;
            case "recipes":
            case "craft":
                if (sender instanceof Player) {
                    RecipesCategoryGUI.open((Player)sender);
                } else {
                    send(sender, ChatColor.RED + "Only players can view recipes GUI.");
                }
                break;
            default:
                send(sender, ChatColor.RED + "Unknown subcommand. Use " + ChatColor.YELLOW + "/pocket help" + ChatColor.RED + " for help.");
                break;
        }
        return true;
    }

    // /pocket give <item> <player>
    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Show all options for custom items (list)
            send(sender, ChatColor.AQUA + "Custom items you can give:");
            for (Map.Entry<String, String> entry : CUSTOM_ITEM_ALIASES.entrySet()) {
                send(sender, "  " + ChatColor.YELLOW + entry.getKey() + ChatColor.GRAY + " - " + entry.getValue());
            }
            send(sender, ChatColor.AQUA + "Usage: " + ChatColor.YELLOW + "/pocket give <item> <player>");
            return;
        }
        String itemKey = args[1].toLowerCase();
        if (!CUSTOM_ITEM_ALIASES.containsKey(itemKey)) {
            send(sender, ChatColor.RED + "Unknown custom item: " + itemKey);
            send(sender, ChatColor.AQUA + "Custom items: " + String.join(", ", CUSTOM_ITEM_ALIASES.keySet()));
            return;
        }

        final Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                send(sender, ChatColor.RED + "Player not found: " + args[2]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player)sender;
        } else {
            send(sender, ChatColor.RED + "You must specify a player from console!");
            return;
        }

        ItemStack toGive = getCustomItem(itemKey);
        if (toGive == null) {
            send(sender, ChatColor.RED + "Failed to create custom item for: " + itemKey);
            return;
        }

        target.getInventory().addItem(toGive);
        send(sender, ChatColor.GREEN + "Gave " + ChatColor.YELLOW + toGive.getItemMeta().getDisplayName()
                + ChatColor.GREEN + " to " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + "!");

        // --- FIX: Force player head texture update for Compact Oak ---
        if (itemKey.equals("compact_oak") || itemKey.equals("compactoak")) {
            ItemStack expected = CompactOakListener.createCompactOak();
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i = 0; i < target.getInventory().getSize(); i++) {
                        ItemStack stack = target.getInventory().getItem(i);
                        if (stack != null && stack.isSimilar(expected)) {
                            target.getInventory().setItem(i, stack);
                        }
                    }
                    target.updateInventory();
                }
            }.runTaskLater(plugin, 2L);
        }
    }

    // Return the actual ItemStack for each custom item
    private ItemStack getCustomItem(String itemKey) {
        switch (itemKey) {
            case "pouch":
                return StoragePouchListener.createPouch();
            case "wooden_chopper":
            case "woodenchopper":
                return CustomItems.getWoodenChopper();
            case "lumber_axe":
            case "lumberaxe":
                return CustomItems.getLumberAxe(0);
            case "treecapitator":
                return TreecapitatorAxesListener.createTreecapitator();
            case "compact_oak":
            case "compactoak":
                return CompactOakListener.createCompactOak();
            default:
                return null;
        }
    }

    private void handlePouchRecipeCommand(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String action = args[1].toLowerCase(Locale.ROOT);
            switch (action) {
                case "enable":
                case "on":
                    PocketPlugin.getInstance().setPouchRecipeEnabled(true);
                    StoragePouchListener.registerCustomRecipes(plugin, true);
                    send(sender, ChatColor.GREEN + "Storage pouch recipes enabled.");
                    break;
                case "disable":
                case "off":
                    PocketPlugin.getInstance().setPouchRecipeEnabled(false);
                    StoragePouchListener.registerCustomRecipes(plugin, false);
                    send(sender, ChatColor.RED + "Storage pouch recipes disabled.");
                    break;
                case "status":
                    boolean enabled = PocketPlugin.getInstance().isPouchRecipeEnabled();
                    send(sender, ChatColor.AQUA + "Storage pouch recipes are currently " +
                            (enabled ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) + ChatColor.AQUA + ".");
                    break;
                default:
                    send(sender, ChatColor.YELLOW + "Usage: /pocket pouchrecipe <enable|disable|status>");
            }
        } else {
            boolean enabled = PocketPlugin.getInstance().isPouchRecipeEnabled();
            send(sender, ChatColor.AQUA + "Storage pouch recipes are currently " +
                    (enabled ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) + ChatColor.AQUA + ".");
            send(sender, ChatColor.YELLOW + "Usage: /pocket pouchrecipe <enable|disable|status>");
        }
    }

    private void handleTreecapRecipeCommand(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String action = args[1].toLowerCase(Locale.ROOT);
            switch (action) {
                case "enable":
                case "on":
                    PocketPlugin.getInstance().setTreecapRecipeEnabled(true);
                    TreecapitatorAxesListener.registerTreecapRecipe(plugin, true);
                    send(sender, ChatColor.GREEN + "Treecapitator recipe enabled.");
                    break;
                case "disable":
                case "off":
                    PocketPlugin.getInstance().setTreecapRecipeEnabled(false);
                    TreecapitatorAxesListener.registerTreecapRecipe(plugin, false);
                    send(sender, ChatColor.RED + "Treecapitator recipe disabled.");
                    break;
                case "status":
                    boolean enabled = PocketPlugin.getInstance().isTreecapRecipeEnabled();
                    send(sender, ChatColor.AQUA + "Treecapitator recipe is currently " +
                            (enabled ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) + ChatColor.AQUA + ".");
                    break;
                default:
                    send(sender, ChatColor.YELLOW + "Usage: /pocket treecaprecipe <enable|disable|status>");
            }
        } else {
            boolean enabled = PocketPlugin.getInstance().isTreecapRecipeEnabled();
            send(sender, ChatColor.AQUA + "Treecapitator recipe is currently " +
                    (enabled ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) + ChatColor.AQUA + ".");
            send(sender, ChatColor.YELLOW + "Usage: /pocket treecaprecipe <enable|disable|status>");
        }
    }

    private void handleCartProtCommand(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String action = args[1].toLowerCase(Locale.ROOT);
            switch (action) {
                case "enable":
                case "on":
                    state.cartProt.set(true);
                    send(sender, ChatColor.GREEN + "Cart Protection enabled.");
                    break;
                case "disable":
                case "off":
                    state.cartProt.set(false);
                    send(sender, ChatColor.RED + "Cart Protection disabled.");
                    break;
                case "status":
                    send(sender, ChatColor.AQUA + "Cart Protection is currently " +
                            (state.cartProt.get() ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) +
                            ChatColor.AQUA + ".");
                    send(sender, ChatColor.AQUA + "Combat-only mode is " +
                            (state.cartProtCombatOnly.get() ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) +
                            ChatColor.AQUA + ".");
                    break;
                case "combatonly":
                    state.cartProtCombatOnly.set(!state.cartProtCombatOnly.get());
                    send(sender, ChatColor.AQUA + "Cart Protection 'combat-only' mode is now "
                            + (state.cartProtCombatOnly.get() ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED"))
                            + ChatColor.AQUA + ".");
                    send(sender, ChatColor.GRAY + "When enabled, minecart protection only applies if the player is in combat.");
                    break;
                default:
                    send(sender, ChatColor.YELLOW + "Usage: /pocket cartprot <enable|disable|status|combatonly>");
            }
        } else {
            send(sender, ChatColor.AQUA + "Cart Protection is currently " +
                    (state.cartProt.get() ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) +
                    ChatColor.AQUA + ".");
            send(sender, ChatColor.AQUA + "Combat-only mode is " +
                    (state.cartProtCombatOnly.get() ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) +
                    ChatColor.AQUA + ".");
            send(sender, ChatColor.YELLOW + "Usage: /pocket cartprot <enable|disable|status|combatonly>");
        }
    }

    private void handleToggleCommand(CommandSender sender, java.util.concurrent.atomic.AtomicBoolean toggle, String feature, String[] args) {
        if (args.length == 2) {
            String action = args[1].toLowerCase(Locale.ROOT);
            if (action.equals("enable") || action.equals("on")) {
                toggle.set(true);
                send(sender, ChatColor.GREEN + feature + " enabled.");
            } else if (action.equals("disable") || action.equals("off")) {
                toggle.set(false);
                send(sender, ChatColor.RED + feature + " disabled.");
            } else if (action.equals("status")) {
                send(sender, ChatColor.AQUA + feature + " is currently " +
                        (toggle.get() ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) + ChatColor.AQUA + ".");
            } else {
                send(sender, ChatColor.YELLOW + "Usage: /pocket " + feature.toLowerCase().replace(" ", "") + " <enable|disable|status>");
            }
        } else {
            send(sender, ChatColor.AQUA + feature + " is currently " +
                    (toggle.get() ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED")) + ChatColor.AQUA + ".");
            send(sender, ChatColor.YELLOW + "Usage: /pocket " + feature.toLowerCase().replace(" ", "") + " <enable|disable|status>");
        }
    }

    private void showHelp(CommandSender s, String sub) {
        if (sub == null) {
            s.sendMessage("");
            s.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "PocketPlugin " + ChatColor.GRAY + "— Command Reference:");
            s.sendMessage("");
            s.sendMessage(ChatColor.GOLD + "/pocket <subcommand>" + ChatColor.GRAY + " — Main command");
            for (String main : COMMAND_TREE.keySet()) {
                s.sendMessage(ChatColor.YELLOW + "  • " + ChatColor.AQUA + main +
                        (COMMAND_TREE.get(main).isEmpty() ? "" : ChatColor.GRAY + " <sub>"));
            }
            s.sendMessage("");
            s.sendMessage(ChatColor.GRAY + "Type " + ChatColor.YELLOW + "/pocket help <subcommand>" + ChatColor.GRAY + " for subcommands/info.");
            s.sendMessage("");
        } else {
            String key = sub.toLowerCase(Locale.ROOT);
            if (COMMAND_TREE.containsKey(key)) {
                s.sendMessage("");
                s.sendMessage(ChatColor.GOLD + "/pocket " + key + ChatColor.GRAY + " — Subcommands:");
                List<String> subs = COMMAND_TREE.get(key);
                for (String child : subs) {
                    s.sendMessage(ChatColor.YELLOW + "  • " + ChatColor.AQUA + child);
                }
                s.sendMessage("");
                s.sendMessage(ChatColor.GRAY + "Example: " + ChatColor.YELLOW + "/pocket " + key + (subs.isEmpty() ? "" : " <" + String.join("|", subs) + ">"));
                s.sendMessage("");
            } else {
                s.sendMessage(ChatColor.RED + "Unknown subcommand for help: " + sub);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player && !((Player)sender).isOp()) return Collections.emptyList();

        if (args.length == 1) {
            return partial(COMMAND_TREE.keySet(), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("give")) {
                return partial(CUSTOM_ITEM_ALIASES.keySet(), args[1]);
            } else if (COMMAND_TREE.containsKey(sub)) {
                return partial(COMMAND_TREE.get(sub), args[1]);
            }
        }
        // Tab-complete player names for /pocket give <item> <player>
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return partial(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> partial(Collection<String> opts, String start) {
        List<String> r = new ArrayList<>();
        for (String s : opts) if (s.toLowerCase(Locale.ROOT).startsWith(start.toLowerCase(Locale.ROOT))) r.add(s);
        return r;
    }
}