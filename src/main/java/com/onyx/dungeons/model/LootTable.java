package com.onyx.dungeons.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tabla de recompensas.
 * Modo "unique" (por defecto): cada entrada tira su probabilidad; las que pasan entran a un pool, se mezcla
 * y se toman hasta maxRolls items distintos (nunca se repite una entrada ni un item igual).
 * Las entradas con probabilidad 100% siempre aparecen.
 */
public class LootTable {
    public final String id;
    public int minRolls = 1;
    public int maxRolls = 3;
    public boolean unique = true;
    public final List<LootEntry> entries = new ArrayList<>();

    public LootTable(String id) { this.id = id; }

    public List<ItemStack> roll(Random r) {
        List<ItemStack> out = new ArrayList<>();
        if (entries.isEmpty()) return out;
        int lo = Math.max(0, Math.min(minRolls, maxRolls));
        int hi = Math.max(0, Math.max(minRolls, maxRolls));
        int target = lo == hi ? lo : lo + r.nextInt(hi - lo + 1);
        if (target <= 0) return out;
        if (unique) {
            // Las entradas con 100% siempre salen; el resto compite por los huecos restantes.
            List<LootEntry> pool = new ArrayList<>();
            for (LootEntry e : entries) {
                if (e.chance >= 100.0) {
                    addUnique(out, e.make(r));
                } else if (r.nextDouble() * 100.0 < e.chance) {
                    pool.add(e);
                }
            }
            Collections.shuffle(pool, r);
            for (LootEntry e : pool) {
                if (out.size() >= target) break;
                addUnique(out, e.make(r));
            }
        } else {
            int attempts = 0;
            while (out.size() < target && attempts < target * 25) {
                attempts++;
                LootEntry e = entries.get(r.nextInt(entries.size()));
                if (r.nextDouble() * 100.0 < e.chance) out.add(e.make(r));
            }
        }
        return out;
    }

    private static void addUnique(List<ItemStack> out, ItemStack it) {
        for (ItemStack o : out) {
            if (o.isSimilar(it)) return;
        }
        out.add(it);
    }

    public void save(ConfigurationSection s) {
        s.set("min-rolls", minRolls);
        s.set("max-rolls", maxRolls);
        s.set("unique", unique);
        ConfigurationSection es = s.createSection("entries");
        int i = 0;
        for (LootEntry e : entries) {
            e.save(es.createSection(String.valueOf(i++)));
        }
    }

    public static LootTable load(String id, ConfigurationSection s) {
        LootTable t = new LootTable(id);
        t.minRolls = s.getInt("min-rolls", 1);
        t.maxRolls = s.getInt("max-rolls", 3);
        t.unique = s.getBoolean("unique", true);
        ConfigurationSection es = s.getConfigurationSection("entries");
        if (es != null) {
            for (String k : es.getKeys(false)) {
                ConfigurationSection c = es.getConfigurationSection(k);
                if (c == null) continue;
                LootEntry e = LootEntry.load(c);
                if (e != null) t.entries.add(e);
            }
        }
        return t;
    }
}
