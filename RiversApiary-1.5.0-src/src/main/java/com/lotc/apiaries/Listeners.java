package com.lotc.apiaries;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityEnterBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Listeners implements Listener {

    private final ApiaryPlugin plugin;

    // Cooldowns / gates
    private final Map<String, Long> extractCd = new HashMap<>(); // per-hive extraction cooldown
    private final Map<String, Long> soundCd   = new HashMap<>(); // per-player sound cooldown (baseline behavior)
    private final Map<UUID, Long>   feedCd    = new HashMap<>(); // per-player bee bread cooldown
    private final Map<String, Long> reagentCd = new HashMap<>(); // per-hive reagent cooldown
    private final java.util.Map<java.util.UUID, Integer> apiaryCount = new java.util.HashMap<>();
    private final java.io.File apiaryDataFile;
    private final org.bukkit.configuration.file.YamlConfiguration apiaryData;



    public Listeners(ApiaryPlugin plugin) {
        this.plugin = plugin;

        // Persistent placement data file
        this.apiaryDataFile = new java.io.File(plugin.getDataFolder(), "apiarydata.yml");
        if (!apiaryDataFile.exists()) {
            try {
                apiaryDataFile.createNewFile();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to create apiarydata.yml");
            }
        }
        this.apiaryData = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(apiaryDataFile);

        // Load saved data into memory
        for (String key : apiaryData.getKeys(false)) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(key);
                int count = apiaryData.getInt(key, 0);
                apiaryCount.put(uuid, count);
            } catch (Exception ignored) {}
        }
    }

    private void saveApiaryData() {
        for (java.util.UUID uuid : apiaryCount.keySet()) {
            apiaryData.set(uuid.toString(), apiaryCount.get(uuid));
        }
        try {
            apiaryData.save(apiaryDataFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save apiarydata.yml");
        }
    }

    // ---------- Utility ----------
    public static void tagBeeHome(Bee bee, Block b){
        var p = bee.getPersistentDataContainer();
        p.set(Items.nk("home_x"), PersistentDataType.INTEGER, b.getX());
        p.set(Items.nk("home_y"), PersistentDataType.INTEGER, b.getY());
        p.set(Items.nk("home_z"), PersistentDataType.INTEGER, b.getZ());
    }

    public static boolean sameHome(Block b, Bee bee){
        var p = bee.getPersistentDataContainer();
        if (!p.has(Items.nk("home_x"), PersistentDataType.INTEGER)) return false;
        return p.get(Items.nk("home_x"), PersistentDataType.INTEGER) == b.getX()
                && p.get(Items.nk("home_y"), PersistentDataType.INTEGER) == b.getY()
                && p.get(Items.nk("home_z"), PersistentDataType.INTEGER) == b.getZ();
    }

    private <T,Z> void copyIfPresent(PersistentDataContainer from, PersistentDataContainer to, NamespacedKey key, PersistentDataType<T,Z> type){
        Z val = from.get(key, type);
        if (val != null) to.set(key, type, val);
    }

    // ---------- Placement ----------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item == null) return;
        ItemMeta im = item.getItemMeta();
        if (im == null) return;
        var ip = im.getPersistentDataContainer();

        Player player = e.getPlayer();

        // --------------------------------------------------------------------------------------------
        // LIMIT: Players may only place up to 2 Apiary blocks (persistent)
        // --------------------------------------------------------------------------------------------
        boolean isApiaryItem = ip.has(Items.nk("managed"), org.bukkit.persistence.PersistentDataType.STRING);
        if (isApiaryItem) {
            int count = apiaryCount.getOrDefault(player.getUniqueId(), 0);
            if (count >= 2) {
                e.setCancelled(true);
                player.sendMessage("§cYou may only place up to §e2 Apiaries§c at a time.");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }

            // Otherwise increment placement count and save
            apiaryCount.put(player.getUniqueId(), count + 1);
            saveApiaryData();
        }

        // --------------------------------------------------------------------------------------------
        // Prevent placement of unplaceable custom items (Smoker, Propolis, etc.)
        // --------------------------------------------------------------------------------------------
        Byte up = im.getPersistentDataContainer().get(Items.nk("unplaceable"), org.bukkit.persistence.PersistentDataType.BYTE);
        if (up != null && up == 1) {
            e.setCancelled(true);
            return;
        }

        boolean managed = ip.has(Items.nk("managed"), org.bukkit.persistence.PersistentDataType.STRING);
        boolean frontier = ip.has(Items.nk("frontier"), org.bukkit.persistence.PersistentDataType.STRING);
        if (!managed && !frontier) return;

        Block b = e.getBlockPlaced();
        if (!(b.getState() instanceof TileState ts)) return;
        var p = ts.getPersistentDataContainer();

        // Copy persistent data from item → placed block
        if (managed) p.set(HiveManager.KEY_MANAGED, org.bukkit.persistence.PersistentDataType.STRING, "1");
        else p.set(HiveManager.KEY_FRONTIER, org.bukkit.persistence.PersistentDataType.STRING, "1");

        copyIfPresent(ip, p, HiveManager.KEY_HONEY, org.bukkit.persistence.PersistentDataType.INTEGER);
        copyIfPresent(ip, p, HiveManager.KEY_WAX, org.bukkit.persistence.PersistentDataType.INTEGER);
        copyIfPresent(ip, p, HiveManager.KEY_CAP_H, org.bukkit.persistence.PersistentDataType.INTEGER);
        copyIfPresent(ip, p, HiveManager.KEY_CAP_W, org.bukkit.persistence.PersistentDataType.INTEGER);
        copyIfPresent(ip, p, HiveManager.KEY_TMAXBONUS, org.bukkit.persistence.PersistentDataType.DOUBLE);
        copyIfPresent(ip, p, HiveManager.KEY_QUEEN, org.bukkit.persistence.PersistentDataType.STRING);
        ts.update(true);

        // Spawn starter bees for new Apiary or Wild Hive
        if (managed || frontier) {
            for (int i = 0; i < 2; i++) {
                Bee bee = (Bee) b.getWorld().spawnEntity(b.getLocation().add(.5, .5, .5), EntityType.BEE);
                bee.setAdult();
                bee.setInvulnerable(true);
                bee.setPersistent(true);
                bee.setRemoveWhenFarAway(false);
                try {
                    var attr = bee.getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_SCALE"));
                    if (attr != null) attr.setBaseValue(0.25D);
                } catch (Throwable ignored) {}
                tagBeeHome(bee, b);
            }
        }
    }

    // ---------- Bee returns (deposit) ----------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnter(EntityEnterBlockEvent e){
        if (!(e.getEntity() instanceof Bee bee)) return;
        Block b = e.getBlock();
        if (!(b.getState() instanceof TileState ts)) return;
        var p = ts.getPersistentDataContainer();
        boolean managed  = p.has(HiveManager.KEY_MANAGED,  PersistentDataType.STRING);
        boolean frontier = p.has(HiveManager.KEY_FRONTIER, PersistentDataType.STRING);
        if (!(managed || frontier)) return;
        if (!bee.hasNectar()) return;

        try { bee.setHasNectar(false); } catch (Throwable ignored) {}
        int capH = 64 + HiveManager.getInt(p, HiveManager.KEY_CAP_H, 0);
        int capW = 64 + HiveManager.getInt(p, HiveManager.KEY_CAP_W, 0);
        int honey = HiveManager.getInt(p, HiveManager.KEY_HONEY, 0);
        int wax   = HiveManager.getInt(p, HiveManager.KEY_WAX, 0);

        if (Math.random() < 0.5) p.set(HiveManager.KEY_WAX, PersistentDataType.INTEGER, Math.min(capW, wax + 1));
        else p.set(HiveManager.KEY_HONEY, PersistentDataType.INTEGER, Math.min(capH, honey + 1));
        ts.update(true);
    }

    // ---------- Break ----------
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.BEEHIVE && b.getType() != Material.BEE_NEST) return;
        if (!(b.getState() instanceof TileState ts)) return;
        var p = ts.getPersistentDataContainer();

        boolean managed = p.has(HiveManager.KEY_MANAGED, org.bukkit.persistence.PersistentDataType.STRING);
        boolean frontier = p.has(HiveManager.KEY_FRONTIER, org.bukkit.persistence.PersistentDataType.STRING);
        if (!(managed || frontier)) return;

        // Trigger aggression for unsmoked hives
        if (managed) {
            double T = HiveManager.getT(b.getState(), plugin.getConfig().getDouble("temperament.base", 0.8));
            boolean freeHarvest = T <= 0.0 && plugin.getConfig().getBoolean("temperament.collapse.atZero.freeHarvest", true);
            if (!HiveManager.isSmoked(b.getState()) && !freeHarvest) {
                spawnAggressors(b, e.getPlayer(), 2);

            }
        } else {
            if (!HiveManager.isSmoked(b.getState())) spawnAggressors(b, e.getPlayer(), 2);

        }

        // Despawn bees linked to this hive
        for (Entity ent : b.getWorld().getNearbyEntities(b.getLocation(), 32, 16, 32)) {
            if (ent instanceof Bee bee && sameHome(b, bee)) bee.remove();
        }

        // Drop Apiary or Wild Hive item with preserved data
        e.setDropItems(false);
        ItemStack drop = Items.build(managed ? "apiary" : "wild_hive");
        ItemMeta im = drop.getItemMeta();
        var ipdc = im.getPersistentDataContainer();
        copyIfPresent(p, ipdc, HiveManager.KEY_HONEY, org.bukkit.persistence.PersistentDataType.INTEGER);
        copyIfPresent(p, ipdc, HiveManager.KEY_WAX, org.bukkit.persistence.PersistentDataType.INTEGER);
        copyIfPresent(p, ipdc, HiveManager.KEY_CAP_H, org.bukkit.persistence.PersistentDataType.INTEGER);
        copyIfPresent(p, ipdc, HiveManager.KEY_CAP_W, org.bukkit.persistence.PersistentDataType.INTEGER);
        copyIfPresent(p, ipdc, HiveManager.KEY_TMAXBONUS, org.bukkit.persistence.PersistentDataType.DOUBLE);
        copyIfPresent(p, ipdc, HiveManager.KEY_QUEEN, org.bukkit.persistence.PersistentDataType.STRING);
        drop.setItemMeta(im);
        b.getWorld().dropItemNaturally(b.getLocation().add(.5, .5, .5), drop);

        // --------------------------------------------------------------------------------------------
        // Decrement player's Apiary placement count (persistent)
        // --------------------------------------------------------------------------------------------
        if (managed && e.getPlayer() != null) {
            java.util.UUID id = e.getPlayer().getUniqueId();
            int current = apiaryCount.getOrDefault(id, 0);
            apiaryCount.put(id, Math.max(0, current - 1));
            saveApiaryData();
        }
    }

    // ---------- Interact (smoker / bee bread / reagents / extraction / queen cage / royal jelly) ----------
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e){
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (!(b.getState() instanceof TileState ts)) return;
        var p = ts.getPersistentDataContainer();

        boolean managed  = p.has(HiveManager.KEY_MANAGED,  PersistentDataType.STRING);
        boolean frontier = p.has(HiveManager.KEY_FRONTIER, PersistentDataType.STRING);
        if (!(managed || frontier)) return;

        Player pl = e.getPlayer();
        ItemStack held = e.getItem();
        ItemMeta im = held != null ? held.getItemMeta() : null;
        String id = null;
        if (im != null){
            var ip = im.getPersistentDataContainer();
            if (ip.has(Items.nk("smoker"), PersistentDataType.STRING)) id = "smoker";
            else if (ip.has(Items.nk("beebread"), PersistentDataType.STRING)) id = "beebread";
            else if (ip.has(Items.nk("reagent"), PersistentDataType.STRING)) id = ip.get(Items.nk("reagent"), PersistentDataType.STRING);
            else if (ip.has(Items.nk("queen_species"), PersistentDataType.STRING)) id = "queen_item";
            else if (ip.has(Items.nk("queen_cage"), PersistentDataType.STRING)) id = "queen_cage";
        }

        // block non-smoker item use on wild hives
        if (frontier && id != null && !"smoker".equals(id)) { e.setCancelled(true); return; }

        // --- smoker
        if ("smoker".equals(id)){
            e.setCancelled(true);
            long ms = (long)(plugin.getConfig().getDouble("cooldowns.smokeSeconds", 60.0) * 1000);

            // inline setSmoked (baseline style): write smokedUntil directly to PDC
            if (b.getState() instanceof TileState ts2){
                PersistentDataContainer p2 = ts2.getPersistentDataContainer();
                p2.set(HiveManager.KEY_SMOKED_UNTIL, PersistentDataType.LONG, System.currentTimeMillis() + ms);
                ts2.update(true);
            }

            World w = b.getWorld();
            w.playSound(b.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 1f, 1f);
            w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, b.getLocation().add(.5, .8, .5), 8, .1, .1, .1, .01);
            return;
        }

        // --- bee bread
        if ("beebread".equals(id) && managed){
            e.setCancelled(true);
            UUID u = pl.getUniqueId();
            long now = System.currentTimeMillis();
            if (now - feedCd.getOrDefault(u, 0L) < 2000L) return;
            feedCd.put(u, now);

            double base = plugin.getConfig().getDouble("temperament.base", 0.8);
            double t    = HiveManager.getT(b.getState(), base);
            double tmax = plugin.getConfig().getDouble("temperament.ceiling.baseMax", 1.0)
                    + HiveManager.getD(p, HiveManager.KEY_TMAXBONUS, 0.0);
            t = Math.min(tmax, t + plugin.getConfig().getDouble("temperament.onFeedBeeBread", 0.10));
            p.set(HiveManager.KEY_T, PersistentDataType.DOUBLE, t);
            ts.update(true);

            if (held != null){
                if (held.getAmount() > 1) held.setAmount(held.getAmount()-1);
                else pl.getInventory().setItemInMainHand(null);
            }

            b.getWorld().playSound(b.getLocation(), Sound.ENTITY_BEE_LOOP, 0.8f, 1f);
            b.getWorld().spawnParticle(Particle.WAX_ON, b.getX()+.5, b.getY()+.7, b.getZ()+.5, 8, .25,.25,.25, .05);
            return;
        }

        // --- upgrades (pollen/propolis/brood_wax)
        if (id != null && managed && (id.equals("pollen") || id.equals("propolis") || id.equals("brood_wax"))){
            e.setCancelled(true);
            String hiveKey = b.getWorld().getUID()+":"+b.getX()+","+b.getY()+","+b.getZ();
            long now = System.currentTimeMillis();
            long gate = 1000L;
            if (now - reagentCd.getOrDefault(hiveKey, 0L) < gate) return;

            boolean changed = false;
            if ("pollen".equals(id)){
                int cur = HiveManager.getInt(p, HiveManager.KEY_CAP_H, 0);
                if (cur < 64){ p.set(HiveManager.KEY_CAP_H, PersistentDataType.INTEGER, Math.min(64, cur + 16)); changed = true; }
            } else if ("propolis".equals(id)){
                int cur = HiveManager.getInt(p, HiveManager.KEY_CAP_W, 0);
                if (cur < 64){ p.set(HiveManager.KEY_CAP_W, PersistentDataType.INTEGER, Math.min(64, cur + 16)); changed = true; }
            } else if ("brood_wax".equals(id)){
                double cur = HiveManager.getD(p, HiveManager.KEY_TMAXBONUS, 0.0);
                if (cur < 0.20){ p.set(HiveManager.KEY_TMAXBONUS, PersistentDataType.DOUBLE, Math.min(0.20, cur + 0.05)); changed = true; }
            }

            if (changed){
                ts.update(true);
                if (held != null){
                    if (held.getAmount() > 1) held.setAmount(held.getAmount()-1);
                    else pl.getInventory().setItemInMainHand(null);
                }
                b.getWorld().playSound(b.getLocation(), Sound.BLOCK_HONEY_BLOCK_SLIDE, 1f, 1f);
                b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getX()+.5, b.getY()+.7, b.getZ()+.5, 8, .25,.25,.25, .05);
                reagentCd.put(hiveKey, now);
            }
            return;
        }

        // --- queen cage remove
        if ("queen_cage".equals(id) && managed){
            e.setCancelled(true);
            if (!p.has(HiveManager.KEY_QUEEN, PersistentDataType.STRING)) {
                pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
                return;
            }
            String sp = p.get(HiveManager.KEY_QUEEN, PersistentDataType.STRING);
            p.remove(HiveManager.KEY_QUEEN);
            ts.update(true);
            // return token
            pl.getInventory().addItem(Items.build("queen_"+sp));
            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 1f, 1.2f);
            b.getWorld().spawnParticle(Particle.SWEEP_ATTACK, b.getX()+.5, b.getY()+.7, b.getZ()+.5, 12, .25,.25,.25, .05);

            // despawn homed bees (queen & workers) and respawn two workers
            for (Entity ent : b.getWorld().getNearbyEntities(b.getLocation(), 32, 16, 32)){
                if (ent instanceof Bee bee && sameHome(b, bee)) bee.remove();
            }
            for (int i = 0; i < 2; i++){
                Bee bee = (Bee)b.getWorld().spawnEntity(b.getLocation().add(.5,.5,.5), EntityType.BEE, CreatureSpawnEvent.SpawnReason.CUSTOM);
                bee.setAdult(); bee.setInvulnerable(true); bee.setPersistent(true); bee.setRemoveWhenFarAway(false);
                try {
                    var attr = bee.getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_SCALE"));
                    if (attr != null) attr.setBaseValue(0.25D);
                } catch (Throwable ignored){}
                tagBeeHome(bee, b);
            }
            return;
        }

        // --- queen insert
        if ("queen_item".equals(id) && managed){
            e.setCancelled(true);
            boolean hasCage = pl.getInventory().containsAtLeast(Items.build("queen_cage"), 1);
            if (!hasCage){ pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f); return; }
            String species = im.getPersistentDataContainer().get(Items.nk("queen_species"), PersistentDataType.STRING);
            if (species != null){
                p.set(HiveManager.KEY_QUEEN, PersistentDataType.STRING, species);
                ts.update(true);
                if (held != null){
                    if (held.getAmount() > 1) held.setAmount(held.getAmount()-1);
                    else pl.getInventory().setItemInMainHand(null);
                }
                Bee q = (Bee)b.getWorld().spawnEntity(b.getLocation().add(.5,.5,.5), EntityType.BEE, CreatureSpawnEvent.SpawnReason.CUSTOM);
                q.setAdult(); q.setInvulnerable(true); q.setPersistent(true); q.setRemoveWhenFarAway(false);
                String pretty = Character.toUpperCase(species.charAt(0)) + species.substring(1);
                q.setCustomName("§e" + pretty + " Queen Bee");
                q.setCustomNameVisible(false);
                try { var attr=q.getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_SCALE")); if (attr != null) attr.setBaseValue(0.5D);} catch(Throwable ignored){}
                tagBeeHome(q, b);
                b.getWorld().playSound(b.getLocation(), Sound.ENTITY_BEE_POLLINATE, 1f, 1f);
                b.getWorld().spawnParticle(Particle.FALLING_HONEY, b.getX()+.5, b.getY()+.7, b.getZ()+.5, 14, .25,.25,.25, .05);
            }
            return;
        }

        // --- royal jelly (incubation)
        if ("royal_jelly".equals(id) && managed){
            e.setCancelled(true);
            if (p.has(HiveManager.KEY_QUEEN, PersistentDataType.STRING)){
                pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
                return;
            }
            long until = System.currentTimeMillis() + (long)(plugin.getConfig().getInt("queens.incubationSeconds", 60) * 1000L);
            p.set(HiveManager.KEY_INCUBATE_UNTIL, PersistentDataType.LONG, until);
            ts.update(true);
            if (held != null){
                if (held.getAmount() > 1) held.setAmount(held.getAmount()-1);
                else pl.getInventory().setItemInMainHand(null);
            }
            // visual pulse tick handled by Tasks
            return;
        }

        // --- extraction (custom honey/wax) ---
        if (held == null) return;
        Material tool = held.getType();
        boolean bottle = tool == Material.GLASS_BOTTLE;
        boolean shears = tool == Material.SHEARS;
        if (!bottle && !shears) return;

        e.setCancelled(true);

        String key = pl.getUniqueId()+":"+b.getWorld().getUID()+":"+b.getX()+","+b.getY()+","+b.getZ();
        long now = System.currentTimeMillis();
        long gate = (long)(plugin.getConfig().getDouble("cooldowns.extractionSeconds", 1.0) * 1000);
        if (now - extractCd.getOrDefault(key, 0L) < gate) return;
        extractCd.put(key, now);

        boolean smoked = HiveManager.isSmoked(b.getState());
        if (!smoked) spawnAggressors(b, pl, 2);

        int honey = HiveManager.getInt(p, HiveManager.KEY_HONEY, 0);
        int wax   = HiveManager.getInt(p, HiveManager.KEY_WAX, 0);

        // per-player sound cooldown (1s)
        String skey = "snd:"+pl.getUniqueId();
        long sndLast = soundCd.getOrDefault(skey, 0L);
        boolean canSound = (now - sndLast) >= (long)(plugin.getConfig().getDouble("cooldowns.soundSeconds",1.0)*1000);

        if (bottle && honey > 0) {
            if (held.getAmount() > 1) held.setAmount(held.getAmount()-1);
            else pl.getInventory().setItemInMainHand(null);
            pl.getInventory().addItem(Items.build("honey"));
            p.set(HiveManager.KEY_HONEY, PersistentDataType.INTEGER, honey - 1);
            ts.update(true);
            if (canSound){
                b.getWorld().playSound(b.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1f);
                soundCd.put(skey, now);
            }
            b.getWorld().spawnParticle(Particle.FALLING_HONEY, b.getX()+.5, b.getY()+.7, b.getZ()+.5, 10, .25,.25,.25, .05);
            return;
        }

        if (shears && wax > 0) {
            pl.getInventory().addItem(Items.build("beeswax"));
            p.set(HiveManager.KEY_WAX, PersistentDataType.INTEGER, wax - 1);
            ts.update(true);
            if (canSound){
                b.getWorld().playSound(b.getLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 1f, 1f);
                soundCd.put(skey, now);
            }
            b.getWorld().spawnParticle(Particle.CLOUD, b.getX()+.5, b.getY()+.7, b.getZ()+.5, 8, .25,.25,.25, .05);
            return;
        }
        pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
    }

    // ---------- Aggression (baseline; renamed species) ----------
    private void spawnAggressors(Block b, Player target, int count){
        if (!(b.getState() instanceof TileState ts)) return;
        var p = ts.getPersistentDataContainer();
        String sp = p.get(HiveManager.KEY_QUEEN, PersistentDataType.STRING);
        if ("apidae".equalsIgnoreCase(sp)) return; // pacifist (renamed)
        int lifetime = "colletidae".equalsIgnoreCase(sp) ? 30
                : plugin.getConfig().getInt("aggression.recall.lifetimeSeconds", 10); // fury (renamed)

        World world = b.getWorld();

        // Spawn hive aggressors
        for (int i = 0; i < count; i++){
            Bee bee = (Bee)world.spawnEntity(b.getLocation().add(.5,.5,.5), EntityType.BEE, CreatureSpawnEvent.SpawnReason.CUSTOM);
            bee.setAdult();
            bee.setInvulnerable(true);
            bee.setPersistent(true);
            bee.setRemoveWhenFarAway(false);
            bee.setTarget(target);
            bee.setAnger(200);
            try {
                var attr = bee.getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_SCALE"));
                if (attr != null) attr.setBaseValue(0.25D);
            } catch (Throwable ignored){}

            // Keep aggressive then remove
            int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (!bee.isValid()) { Bukkit.getScheduler().cancelTask(taskId[0]); return; }
                if (bee.getTarget() == null || !bee.getTarget().isValid()) bee.setTarget(target);
                bee.setAnger(200);
                if (bee.hasStung()) bee.setHasStung(false);
            }, 20L, 20L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.getScheduler().cancelTask(taskId[0]);
                if (bee.isValid()) bee.remove();
            }, lifetime * 20L);
        }

        // Nearby bees aggression (10 block radius)
        double radius = 10.0;
        for (Entity ent : world.getNearbyEntities(b.getLocation(), radius, radius, radius)) {
            if (ent instanceof Bee bee) {
                if (!bee.isValid()) continue;
                bee.setTarget(target);
                bee.setAnger(200);

                int[] repeatId = new int[1];
                repeatId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    if (!bee.isValid()) { Bukkit.getScheduler().cancelTask(repeatId[0]); return; }
                    if (bee.getTarget() == null || !bee.getTarget().isValid()) bee.setTarget(target);
                    bee.setAnger(200);
                    if (bee.hasStung()) bee.setHasStung(false);
                }, 20L, 20L);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.getScheduler().cancelTask(repeatId[0]);
                    if (bee.isValid()) {
                        bee.setTarget(null);
                        bee.setAnger(0);
                    }
                }, lifetime * 20L);
            }
        }

        // Feedback
        world.spawnParticle(Particle.SMOKE, b.getLocation().add(.5, .7, .5), 10, .3, .3, .3, .05);
        world.playSound(b.getLocation(), Sound.ENTITY_BEE_STING, 1f, 1f);
    }

    // ---------- Megachilidae Queen: standard nectar from flowers ----------
    @EventHandler(ignoreCancelled = true)
    public void onBeePollinateMegachilidae(org.bukkit.event.entity.EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Bee bee)) return;

        var bpdc = bee.getPersistentDataContainer();
        if (!bpdc.has(Items.nk("home_x"), org.bukkit.persistence.PersistentDataType.INTEGER)) return;

        org.bukkit.block.Block home = bee.getWorld().getBlockAt(
                bpdc.get(Items.nk("home_x"), org.bukkit.persistence.PersistentDataType.INTEGER),
                bpdc.get(Items.nk("home_y"), org.bukkit.persistence.PersistentDataType.INTEGER),
                bpdc.get(Items.nk("home_z"), org.bukkit.persistence.PersistentDataType.INTEGER)
        );
        if (!(home.getState() instanceof org.bukkit.block.TileState ts)) return;
        var hivePdc = ts.getPersistentDataContainer();

        if (!hivePdc.has(HiveManager.KEY_MANAGED, org.bukkit.persistence.PersistentDataType.STRING)
                && !hivePdc.has(HiveManager.KEY_FRONTIER, org.bukkit.persistence.PersistentDataType.STRING)) return;

        String queen = hivePdc.get(HiveManager.KEY_QUEEN, org.bukkit.persistence.PersistentDataType.STRING);
        if (queen == null || !queen.equalsIgnoreCase("megachilidae")) return;

        // vanilla flower collection; no crop checks
        org.bukkit.Material mat = event.getBlock().getType();
        boolean validFlower = mat.name().contains("FLOWER") || mat == org.bukkit.Material.SUNFLOWER || mat == org.bukkit.Material.LILAC;
        if (!validFlower) return;

        try { bee.setHasNectar(true); } catch (Throwable ignored) {}
        bee.getWorld().playSound(bee.getLocation(), org.bukkit.Sound.ENTITY_BEE_POLLINATE, 0.5f, 1.0f);
    }

    private void growNearbyCrops(org.bukkit.Location center, int radius) {
        org.bukkit.World world = center.getWorld();
        if (world == null) return;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(center.getBlockX()+x, center.getBlockY()+y, center.getBlockZ()+z);
                    org.bukkit.Material m = block.getType();
                    boolean isCrop = (m == org.bukkit.Material.WHEAT || m == org.bukkit.Material.CARROTS || m == org.bukkit.Material.POTATOES
                            || m == org.bukkit.Material.BEETROOTS || m == org.bukkit.Material.NETHER_WART || m == org.bukkit.Material.TORCHFLOWER_CROP);
                    if (!isCrop) continue;

                    if (block.getBlockData() instanceof org.bukkit.block.data.Ageable age) {
                        if (age.getAge() < age.getMaximumAge() && Math.random() < 0.25) {
                            age.setAge(age.getAge() + 1);
                            block.setBlockData(age);
                            world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                                    block.getLocation().add(0.5, 0.5, 0.5),
                                    3, 0.2, 0.2, 0.2, 0.01);
                        }
                    }
                }
            }
        }
    }
}
