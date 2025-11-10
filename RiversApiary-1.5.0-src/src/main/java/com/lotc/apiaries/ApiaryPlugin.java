package com.lotc.apiaries;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ApiaryPlugin extends JavaPlugin {

    private static ApiaryPlugin instance;
    public static ApiaryPlugin get() { return instance; }

    private Listeners listeners;

    @Override
    public void onEnable() {
        instance = this;

        // Load config (creates default if missing)
        saveDefaultConfig();

        // Initialize item registry and keyspace
        Items.init(this);
        HiveManager.init(this);

        // Command registration
        PluginCommand cmd = getCommand("apiary");
        ApiaryCommand api = new ApiaryCommand(this);
        if (cmd != null) {
            cmd.setExecutor(api);
            cmd.setTabCompleter(api);
        }

        // Register event listeners
        listeners = new Listeners(this);
        getServer().getPluginManager().registerEvents(listeners, this);

        // Start background tasks (smoke tick, temperament decay, incubation, megachilidae trail)
        Tasks.startSchedulers(this);

        // --- Register custom recipes (Apiary, Smoker, etc.) ---
        Recipes.registerAll(this);

        getLogger().info("[RiversApiary] Enabled successfully.");
    }

    @Override
    public void onDisable() {
        // Stop tasks safely
        Tasks.stopSchedulers();

        // --- Unregister only our recipes (vanilla untouched) ---
        try {
            Recipes.unregisterAll(this);
        } catch (Throwable ignored) {}

        getLogger().info("[RiversApiary] Disabled.");
    }

    /** Provides the active listener instance for other classes if needed. */
    public Listeners getListeners() {
        return listeners;
    }
}
