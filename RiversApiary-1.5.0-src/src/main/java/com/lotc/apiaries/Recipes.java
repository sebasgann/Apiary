package com.lotc.apiaries;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers additive, version-safe Bukkit recipes for RiversApiary.
 * - Adds an Apiary crafting recipe (uses EXACT custom Beeswax, not vanilla honeycomb).
 * - Adds a Bee Smoker recipe (asymmetrical).
 * - Does NOT remove vanilla beehive recipes.
 *
 * Notes:
 * - We use RecipeChoice.ExactChoice(Items.build("beeswax")) to ensure only your custom Beeswax works.
 * - We pre-remove any old recipe with the same NamespacedKey (avoids duplication on /reload).
 * - unregisterAll() only removes OUR keys, not vanilla recipes.
 */
public final class Recipes {

    private Recipes() {}

    /** Public entry point used by ApiaryPlugin.onEnable(). */
    public static void registerAll(JavaPlugin plugin) {
        registerApiary(plugin);
        registerSmoker(plugin);
    }

    /** Called from ApiaryPlugin.onDisable() to clean our keys if desired. */
    public static void unregisterAll(JavaPlugin plugin) {
        // Only remove the recipes we registered, by their keys.
        Bukkit.removeRecipe(new NamespacedKey(plugin, "apiary"));
        Bukkit.removeRecipe(new NamespacedKey(plugin, "smoker"));
    }

    // --------------------------------------------------------------------
    // APIARY RECIPE
    // Shape: S S S
    //        B B B
    //        S S S
    //
    //  S = any slab (broad: any *_SLAB that is an item)
    //  B = EXACT your custom Beeswax (Items.build("beeswax"))
    //
    // Result = Items.build("apiary") with full PDC (managed hive)
    // --------------------------------------------------------------------
    private static void registerApiary(JavaPlugin plugin) {
        ItemStack result = Items.build("apiary"); // your managed hive block with PDC

        NamespacedKey key = new NamespacedKey(plugin, "apiary");
        // Avoid duplicate on /reload
        Bukkit.removeRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("SSS", "BBB", "SSS");

        // Any slab for simplicity (includes wood & stone slabs). If you want wood-only,
        // replace with a curated list of wood slabs.
        RecipeChoice anySlab = new RecipeChoice.MaterialChoice(allSlabs());
        recipe.setIngredient('S', anySlab);

        // EXACT custom Beeswax
        ItemStack beeswax = Items.build("beeswax");
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(beeswax));

        Bukkit.addRecipe(recipe);
    }

    // --------------------------------------------------------------------
// BEE SMOKER (Shapeless)
// Requires the following ingredients anywhere in the grid:
//   - 1x CHARCOAL
//   - 1x RESIN_CLUMP
//   - 1x PAPER
//   - 1x NETHERITE_INGOT
//
// Result = Items.build("smoker") (unplaceable lantern with PDC)
// --------------------------------------------------------------------
    private static void registerSmoker(JavaPlugin plugin) {
        ItemStack result = Items.build("smoker"); // unplaceable lantern

        NamespacedKey key = new NamespacedKey(plugin, "smoker");
        Bukkit.removeRecipe(key);

        // Shapeless recipe: items can be placed in any slot
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        recipe.addIngredient(Material.CHARCOAL);
        recipe.addIngredient(Material.RESIN_CLUMP);
        recipe.addIngredient(Material.PAPER);
        recipe.addIngredient(Material.NETHERITE_INGOT); // only one now

        Bukkit.addRecipe(recipe);
    }


    // Utility: collect all slab materials that are real items
    private static List<Material> allSlabs() {
        List<Material> list = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.isItem() && m.name().endsWith("_SLAB")) {
                list.add(m);
            }
        }
        return list;
    }
}
