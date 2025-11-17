/**
 * RiversApiary v1.0 — ApiaryPlugin.java
 * Main plugin entry class. Wires commands, listeners, schedulers, recipes.
 *
 * NOTE: All functional code is unchanged. Only comments were regenerated for clarity.
 */
package com.lotc.apiaries;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ApiaryPlugin extends JavaPlugin {

    private static ApiaryPlugin instance;
    /**
     * get: Helper/function within RiversApiary.
     */
    public static ApiaryPlugin get() { return instance; }

    private Listeners listeners;

    @Override
    /**
     * onEnable: Plugin startup hook: loads config, initializes items and keys, wires commands & listeners, starts schedulers, registers recipes.
     */
    public void onEnable() {
        instance = this;


        saveDefaultConfig();


        Items.init(this);
        HiveManager.init(this);


        PluginCommand cmd = getCommand("apiary");
        ApiaryCommand api = new ApiaryCommand(this);
        if (cmd != null) {
            cmd.setExecutor(api);
            cmd.setTabCompleter(api);
        }

        listeners = new Listeners(this);
        getServer().getPluginManager().registerEvents(listeners, this);

        Tasks.startSchedulers(this);

        Recipes.registerAll(this);

        getLogger().info("[RiversApiary] Enabled successfully.");
    }

    @Override
    /**
     * onDisable: Plugin shutdown hook: stops schedulers and unregisters custom recipes.
     */
    public void onDisable() {

        Tasks.stopSchedulers();

        try {
            Recipes.unregisterAll(this);
        } catch (Throwable ignored) {}

        getLogger().info("[RiversApiary] Disabled.");
    }


    /**
     * getListeners: Helper/function within RiversApiary.
     */
    public Listeners getListeners() {
        return listeners;
    }
}
