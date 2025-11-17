/**
 * RiversApiary v1.0 — Tasks.java
 * Schedulers: periodic smoker visuals/sounds, temperament decay, queen incubation, Megachilidae trail pulses.
 *
 * NOTE: All functional code is unchanged. Only comments were regenerated for clarity.
 */
package com.lotc.apiaries;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Bee;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

public final class Tasks {

    private static BukkitTask smokeTick, temperamentDecay, incubationTick;

    private static BukkitTask megachilidaeTrail;

    /**
     * startSchedulers: Starts repeating tasks: smoker particles/sound, temperament decay, queen incubation, Megachilidae trail.
     */
    public static void startSchedulers(ApiaryPlugin plugin) {


        smokeTick = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World w : Bukkit.getWorlds()) {
                for (Chunk c : w.getLoadedChunks()) {
                    for (BlockState st : c.getTileEntities()) {
                        if (st.getType() != Material.BEEHIVE && st.getType() != Material.BEE_NEST) continue;
                        if (!HiveManager.isSmoked(st)) continue;

                        Location loc = st.getLocation().add(0.5, 0.8, 0.5);
                        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 6, 0.1, 0.1, 0.1, 0.01);
                        w.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 1f);
                    }
                }
            }
        }, 40L, 40L);



        temperamentDecay = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double hourly = plugin.getConfig().getDouble("temperament.hourlyDecay", 0.01);
            double perMin = hourly / 60.0;

            for (World w : Bukkit.getWorlds()) {
                for (Chunk c : w.getLoadedChunks()) {
                    for (BlockState st : c.getTileEntities()) {
                        if (!(st instanceof TileState ts)) continue;
                        if (!ts.getPersistentDataContainer().has(HiveManager.KEY_MANAGED, PersistentDataType.STRING)) continue;

                        double base = plugin.getConfig().getDouble("temperament.base", 0.8);
                        double t = HiveManager.getT(st, base);
                        t = Math.max(0.0, t - perMin);
                        HiveManager.setT(st, t);
                    }
                }
            }
        }, 1200L, 1200L);


        megachilidaeTrail = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World w : Bukkit.getWorlds()) {
                for (Bee bee : w.getEntitiesByClass(Bee.class)) {
                    if (!bee.isValid() || !bee.hasNectar()) continue;
                    var pdc = bee.getPersistentDataContainer();
                    if (!pdc.has(Items.nk("home_x"), org.bukkit.persistence.PersistentDataType.INTEGER)) continue;

                    Block home = w.getBlockAt(
                            pdc.get(Items.nk("home_x"), org.bukkit.persistence.PersistentDataType.INTEGER),
                            pdc.get(Items.nk("home_y"), org.bukkit.persistence.PersistentDataType.INTEGER),
                            pdc.get(Items.nk("home_z"), org.bukkit.persistence.PersistentDataType.INTEGER)
                    );
                    if (!(home.getState() instanceof org.bukkit.block.TileState ts)) continue;
                    var hivePdc = ts.getPersistentDataContainer();

                    if (!hivePdc.has(HiveManager.KEY_MANAGED, PersistentDataType.STRING)
                            && !hivePdc.has(HiveManager.KEY_FRONTIER, PersistentDataType.STRING)) continue;
                    String queen = hivePdc.get(HiveManager.KEY_QUEEN, org.bukkit.persistence.PersistentDataType.STRING);
                    if (queen == null || !queen.equalsIgnoreCase("megachilidae")) continue;

                    Location loc = bee.getLocation();


                    w.spawnParticle(Particle.FALLING_NECTAR, loc, 1, 0.15, 0.15, 0.15, 0.01);


                    if (bee.getTicksLived() % 10 == 0) {
                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                Block block = w.getBlockAt(loc.getBlockX() + dx, loc.getBlockY() - 1, loc.getBlockZ() + dz);
                                Material m = block.getType();
                                if (!(m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES ||
                                        m == Material.BEETROOTS || m == Material.NETHER_WART || m == Material.TORCHFLOWER_CROP))
                                    continue;
                                if (!(block.getBlockData() instanceof Ageable age)) continue;
                                if (age.getAge() >= age.getMaximumAge()) continue;

                                if (Math.random() < 0.25) {
                                    age.setAge(age.getAge() + 1);
                                    block.setBlockData(age);

                                    Location bLoc = block.getLocation().add(0.5, 0.5, 0.5);
                                    w.spawnParticle(Particle.SCRAPE, bLoc, 4, 0.2, 0.2, 0.2, 0.01);
                                }
                            }
                        }
                    }
                }
            }
        }, 0L, 1L);



        incubationTick = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            for (World w : Bukkit.getWorlds()) {
                for (Chunk c : w.getLoadedChunks()) {
                    for (BlockState st : c.getTileEntities()) {
                        if (!(st instanceof TileState ts)) continue;

                        var p = ts.getPersistentDataContainer();
                        if (!p.has(HiveManager.KEY_MANAGED, PersistentDataType.STRING)
                                && !p.has(HiveManager.KEY_FRONTIER, PersistentDataType.STRING)) continue;

                        Long until = p.get(HiveManager.KEY_INCUBATE_UNTIL, PersistentDataType.LONG);
                        if (until == null) continue;


                        w.spawnParticle(Particle.WAX_ON, st.getLocation().add(.5, .8, .5), 4, .15, .15, .15, .01);


                        if (now >= until) {
                            p.remove(HiveManager.KEY_INCUBATE_UNTIL);


                            String[] species = new String[]{"megachilidae", "apidae", "colletidae"};
                            String sp = species[(int) (Math.random() * species.length)];

                            p.set(HiveManager.KEY_QUEEN, PersistentDataType.STRING, sp);
                            ts.update(true);


                            Bee q = (Bee) w.spawnEntity(st.getLocation().add(.5, .5, .5), EntityType.BEE);
                            q.setAdult();
                            q.setInvulnerable(true);
                            q.setPersistent(true);
                            q.setRemoveWhenFarAway(false);


                            String pretty = Character.toUpperCase(sp.charAt(0)) + sp.substring(1);
                            q.setCustomName("§e" + pretty + " Queen Bee");
                            q.setCustomNameVisible(false);

                            try {
                                var sc = q.getAttribute(Attribute.valueOf("GENERIC_SCALE"));
                                if (sc != null) sc.setBaseValue(0.5D);
                            } catch (Throwable ignored) {}

                            w.playSound(st.getLocation(), Sound.ENTITY_BEE_POLLINATE, 1f, 1f);
                            w.spawnParticle(Particle.FALLING_HONEY, st.getLocation().add(.5, .8, .5), 14, .25, .25, .25, .02);
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    /**
     * stopSchedulers: Stops all repeating tasks safely.
     */
    public static void stopSchedulers() {
        if (smokeTick != null) smokeTick.cancel();
        if (temperamentDecay != null) temperamentDecay.cancel();
        if (incubationTick != null) incubationTick.cancel();
        if (megachilidaeTrail != null) megachilidaeTrail.cancel();

    }
}
