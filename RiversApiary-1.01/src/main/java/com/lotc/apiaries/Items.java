/**
 * RiversApiary v1.0 — Items.java
 * Custom item factory: builds all plugin items with PDC, names, lore, and unplaceable flags.
 *
 * NOTE: All functional code is unchanged. Only comments were regenerated for clarity.
 */
package com.lotc.apiaries;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class Items {

    private static ApiaryPlugin plugin;

    /**
     * init: Initializes NamespacedKeys for persistent data (HiveManager.init).
     */
    public static void init(ApiaryPlugin pl){ plugin = pl; }
    /**
     * nk: Helper/function within RiversApiary.
     */
    public static NamespacedKey nk(String k){ return new NamespacedKey(plugin, k); }

    /**
     * resinMaterial: Helper/function within RiversApiary.
     */
    private static Material resinMaterial() {
        Material m = Material.matchMaterial("RESIN_CLUMP");
        return m != null ? m : Material.SLIME_BALL;
    }

    /**
     * basic: Helper/function within RiversApiary.
     */
    private static ItemStack basic(Material m, String name, java.util.List<String> lore){
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        if (lore != null) im.setLore(lore);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(im);
        return it;
    }

    /**
     * build: Helper/function within RiversApiary.
     */
    public static ItemStack build(String id){
        switch (id){
            case "apiary": {
                ItemStack it = basic(Material.BEEHIVE, "§eApiary", java.util.List.of("§7A hand-built home for bees."));
                ItemMeta im = it.getItemMeta();
                im.getPersistentDataContainer().set(nk("managed"), PersistentDataType.STRING, "1");
                it.setItemMeta(im);
                return it;
            }
            case "wild_hive": {
                ItemStack it = basic(Material.BEE_NEST, "§eWild Bee Hive", java.util.List.of("§7A feral nest of comb and hum."));
                ItemMeta im = it.getItemMeta();
                im.getPersistentDataContainer().set(nk("frontier"), PersistentDataType.STRING, "1");
                it.setItemMeta(im);
                return it;
            }
            case "smoker": {
                ItemStack it = new ItemStack(Material.LANTERN);
                ItemMeta im = it.getItemMeta();
                im.setDisplayName("§eBee Smoker");
                im.setLore(java.util.List.of("§7Warm breath of the embers.","§7Smoke curls like a tame ghost."));
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                im.getPersistentDataContainer().set(nk("smoker"), PersistentDataType.STRING, "1");
                im.getPersistentDataContainer().set(nk("unplaceable"), PersistentDataType.BYTE, (byte)1);
                it.setItemMeta(im);
                return it;
            }
            case "queen_cage": {
                ItemStack it = new ItemStack(Material.NOTE_BLOCK);
                ItemMeta im = it.getItemMeta();
                im.setDisplayName("§eQueen Bee Cage");
                im.setLore(java.util.List.of("§7Woven soundwood, quiet and sure."));
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                im.getPersistentDataContainer().set(nk("queen_cage"), PersistentDataType.STRING, "1");
                im.getPersistentDataContainer().set(nk("unplaceable"), PersistentDataType.BYTE, (byte)1);
                it.setItemMeta(im);
                return it;
            }

            case "bee_bread": {
                ItemStack it = new ItemStack(Material.GOLD_NUGGET);
                ItemMeta im = it.getItemMeta();
                im.setDisplayName("§eBee Bread");
                im.setLore(java.util.List.of("§7Pollen pressed into a sun-sweet loaf."));
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                im.getPersistentDataContainer().set(nk("beebread"), PersistentDataType.STRING, "1");
                it.setItemMeta(im);
                return it;
            }
            case "royal_jelly": {
                ItemStack it = new ItemStack(Material.HONEYCOMB);
                ItemMeta im = it.getItemMeta();
                im.setDisplayName("§eRoyal Jelly");
                im.setLore(java.util.List.of("§7The queen’s first feast."));
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                im.getPersistentDataContainer().set(nk("reagent"), PersistentDataType.STRING, "royal_jelly");
                it.setItemMeta(im);
                return it;
            }
            case "honey": {
                ItemStack it = new ItemStack(Material.HONEY_BOTTLE);
                ItemMeta im = it.getItemMeta();
                im.setDisplayName("§eHoney");
                im.setLore(java.util.List.of("§7Sweet and slow as summer."));
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                it.setItemMeta(im);
                return it;
            }
            case "beeswax": {
                ItemStack it = new ItemStack(Material.HONEYCOMB);
                ItemMeta im = it.getItemMeta();
                im.setDisplayName("§eBeeswax");
                im.setLore(java.util.List.of("§7Warm gold with the scent of hive."));
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                it.setItemMeta(im);
                return it;
            }

            case "pollen": {
                ItemStack it = new ItemStack(Material.YELLOW_DYE);
                ItemMeta im = it.getItemMeta();
                im.setDisplayName("§ePollen");
                im.setLore(java.util.List.of("§7Amber dust that coaxes combs."));
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                im.getPersistentDataContainer().set(nk("reagent"), PersistentDataType.STRING, "pollen");
                it.setItemMeta(im);
                return it;
            }
            case "propolis": {
                ItemStack it = new ItemStack(resinMaterial());
                ItemMeta im = it.getItemMeta();
                im.setDisplayName("§ePropolis");
                im.setLore(java.util.List.of("§7Tree-resin, warm and binding."));
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                im.getPersistentDataContainer().set(nk("reagent"), PersistentDataType.STRING, "propolis");
                im.getPersistentDataContainer().set(nk("unplaceable"), PersistentDataType.BYTE, (byte)1);
                it.setItemMeta(im);
                return it;
            }
            case "brood_wax": {
                ItemStack it = new ItemStack(Material.GLOWSTONE_DUST);
                ItemMeta im = it.getItemMeta();
                im.setDisplayName("§eBrood Wax");
                im.setLore(java.util.List.of("§7Warm hush sealed in comb."));
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                im.getPersistentDataContainer().set(nk("reagent"), PersistentDataType.STRING, "brood_wax");
                it.setItemMeta(im);
                return it;
            }


            case "queen_megachilidae":  return queenToken("megachilidae");
            case "queen_apidae":        return queenToken("apidae");
            case "queen_colletidae":    return queenToken("colletidae");
        }
        return null;
    }

    /**
     * queenToken: Helper/function within RiversApiary.
     */
    private static ItemStack queenToken(String species){
        ItemStack it = new ItemStack(Material.BEE_SPAWN_EGG);
        ItemMeta im = it.getItemMeta();
        String pretty = Character.toUpperCase(species.charAt(0)) + species.substring(1);
        im.setDisplayName("§e" + pretty + " Queen Bee");
        im.setLore(java.util.List.of("§7A matriarch swaddled in silence."));
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        var p = im.getPersistentDataContainer();
        p.set(nk("queen_species"), PersistentDataType.STRING, species);
        it.setItemMeta(im);
        return it;
    }
}
