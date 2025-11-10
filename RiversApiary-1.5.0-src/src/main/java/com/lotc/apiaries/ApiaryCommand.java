package com.lotc.apiaries;

import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ApiaryCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ITEMS = Arrays.asList(
            "apiary","wild_hive",
            "smoker","queen_cage","bee_bread",
            "pollen","propolis","brood_wax","royal_jelly",
            "honey","beeswax",
            "queen_amber","queen_megachilidae","queen_pluvial","queen_apidae","queen_colletidae"
    );

    public ApiaryCommand(ApiaryPlugin plugin){ }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            p.sendMessage("Usage: /apiary give <item> [amount]");
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!ITEMS.contains(id)) { p.sendMessage("Unknown item: " + id); return true; }

        int amount = 1;
        if (args.length >= 3) { try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (Exception ignored) {} }

        ItemStack it = Items.build(id);
        if (it == null || it.getType() == Material.AIR) { p.sendMessage("That item is not available."); return true; }
        it.setAmount(amount);
        p.getInventory().addItem(it);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("give");
        if (args.length == 2) {
            List<String> out = new ArrayList<>();
            for (String s : ITEMS) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            return out;
        }
        return List.of();
    }
}
