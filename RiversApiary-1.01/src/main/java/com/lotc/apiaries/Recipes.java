/**
 * RiversApiary v1.0 — Recipes.java
 * Registers additive Bukkit recipes (Apiary, Bee Smoker) using exact custom Beeswax ingredient.
 *
 * NOTE: All functional code is unchanged. Only comments were regenerated for clarity.
 */
package com.lotc.apiaries;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Recipes {

    /**
     * Recipes: Helper/function within RiversApiary.
     */
    private Recipes() {}


    /**
     * registerAll: Registers additive crafting recipes (Apiary & Smoker).
     */
    public static void registerAll(JavaPlugin plugin) {
        registerApiary(plugin);
        registerSmoker(plugin);
    }


    /**
     * unregisterAll: Removes only this plugin's custom recipes by key.
     */
    public static void unregisterAll(JavaPlugin plugin) {

        Bukkit.removeRecipe(new NamespacedKey(plugin, "apiary"));
        Bukkit.removeRecipe(new NamespacedKey(plugin, "smoker"));
    }












    /**
     * registerApiary: Helper/function within RiversApiary.
     */
    private static void registerApiary(JavaPlugin plugin) {
        ItemStack result = Items.build("apiary");

        NamespacedKey key = new NamespacedKey(plugin, "apiary");

        Bukkit.removeRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("SSS", "BBB", "SSS");



        RecipeChoice anySlab = new RecipeChoice.MaterialChoice(allSlabs());
        recipe.setIngredient('S', anySlab);


        ItemStack beeswax = Items.build("beeswax");
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(beeswax));

        Bukkit.addRecipe(recipe);
    }











    /**
     * registerSmoker: Helper/function within RiversApiary.
     */
    private static void registerSmoker(JavaPlugin plugin) {
        ItemStack result = Items.build("smoker");

        NamespacedKey key = new NamespacedKey(plugin, "smoker");
        Bukkit.removeRecipe(key);


        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        recipe.addIngredient(Material.CHARCOAL);
        recipe.addIngredient(Material.RESIN_CLUMP);
        recipe.addIngredient(Material.PAPER);
        recipe.addIngredient(Material.NETHERITE_INGOT);

        Bukkit.addRecipe(recipe);
    }



    /**
     * allSlabs: Helper/function within RiversApiary.
     */
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
