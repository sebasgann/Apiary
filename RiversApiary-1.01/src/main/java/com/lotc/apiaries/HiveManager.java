/**
 * RiversApiary v1.0 — HiveManager.java
 * Persistent-data (PDC) key registry and helpers for hive state (honey, wax, capacity, queens, smoked, temperament).
 *
 * NOTE: All functional code is unchanged. Only comments were regenerated for clarity.
 */
package com.lotc.apiaries;

import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class HiveManager {

    public static NamespacedKey KEY_MANAGED;
    public static NamespacedKey KEY_FRONTIER;

    public static NamespacedKey KEY_HONEY;
    public static NamespacedKey KEY_WAX;
    public static NamespacedKey KEY_CAP_H;
    public static NamespacedKey KEY_CAP_W;

    public static NamespacedKey KEY_T;
    public static NamespacedKey KEY_TMAXBONUS;
    public static NamespacedKey KEY_SMOKED_UNTIL;

    public static NamespacedKey KEY_QUEEN;
    public static NamespacedKey KEY_INCUBATE_UNTIL;

    /**
     * init: Initializes NamespacedKeys for persistent data (HiveManager.init).
     */
    public static void init(ApiaryPlugin plugin){
        KEY_MANAGED      = new NamespacedKey(plugin, "managed");
        KEY_FRONTIER     = new NamespacedKey(plugin, "frontier");

        KEY_HONEY        = new NamespacedKey(plugin, "honey");
        KEY_WAX          = new NamespacedKey(plugin, "wax");
        KEY_CAP_H        = new NamespacedKey(plugin, "cap_h");
        KEY_CAP_W        = new NamespacedKey(plugin, "cap_w");

        KEY_T            = new NamespacedKey(plugin, "T");
        KEY_TMAXBONUS    = new NamespacedKey(plugin, "TmaxBonus");
        KEY_SMOKED_UNTIL = new NamespacedKey(plugin, "smokedUntil");

        KEY_QUEEN        = new NamespacedKey(plugin, "queen_species");
        KEY_INCUBATE_UNTIL = new NamespacedKey(plugin, "incubateUntil");
    }

    /**
     * pdcOf: Helper/function within RiversApiary.
     */
    private static PersistentDataContainer pdcOf(BlockState st){
        if (!(st instanceof TileState ts)) return null;
        return ts.getPersistentDataContainer();
    }

    /**
     * isManaged: Helper/function within RiversApiary.
     */
    public static boolean isManaged(BlockState st){
        var p = pdcOf(st); return p!=null && p.has(KEY_MANAGED, PersistentDataType.STRING);
    }
    /**
     * isFrontier: Helper/function within RiversApiary.
     */
    public static boolean isFrontier(BlockState st){
        var p = pdcOf(st); return p!=null && p.has(KEY_FRONTIER, PersistentDataType.STRING);
    }

    /**
     * getInt: Helper/function within RiversApiary.
     */
    public static int getInt(PersistentDataContainer pdc, NamespacedKey k, int def){ Integer v = pdc.get(k, PersistentDataType.INTEGER); return v==null?def:v; }
    /**
     * getD: Helper/function within RiversApiary.
     */
    public static double getD(PersistentDataContainer pdc, NamespacedKey k, double def){ Double v = pdc.get(k, PersistentDataType.DOUBLE); return v==null?def:v; }
    /**
     * getL: Helper/function within RiversApiary.
     */
    public static long getL(PersistentDataContainer pdc, NamespacedKey k, long def){ Long v = pdc.get(k, PersistentDataType.LONG); return v==null?def:v; }
    /**
     * getS: Helper/function within RiversApiary.
     */
    public static String getS(PersistentDataContainer pdc, NamespacedKey k, String def){ String v = pdc.get(k, PersistentDataType.STRING); return v==null?def:v; }

    /**
     * isSmoked: Checks if a hive is currently in the smoked state (timestamp-based).
     */
    public static boolean isSmoked(BlockState st){
        var p = pdcOf(st); if (p==null) return false;
        Long until = p.get(KEY_SMOKED_UNTIL, PersistentDataType.LONG);
        return until != null && until > System.currentTimeMillis();
    }

    /**
     * getT: Reads hive temperament value T from PDC, falling back to base.
     */
    public static double getT(BlockState st, double base){
        var p = pdcOf(st); Double v = (p!=null) ? p.get(KEY_T, PersistentDataType.DOUBLE) : null;
        return v!=null ? v : base;
    }
    /**
     * setT: Writes hive temperament T back to PDC.
     */
    public static void setT(BlockState st, double v){
        if (!(st instanceof TileState ts)) return;
        ts.getPersistentDataContainer().set(KEY_T, PersistentDataType.DOUBLE, v);
        ts.update(true);
    }
}
