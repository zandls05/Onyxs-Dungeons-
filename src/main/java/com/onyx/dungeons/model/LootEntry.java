package com.onyx.dungeons.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class LootEntry {
    public ItemStack item;
    /** Probabilidad en porcentaje (0-100) de que el item entre al pool de un cofre. */
    public double chance = 100.0;
    public int min = 1;
    public int max = 1;

    public LootEntry(ItemStack item) {
        this.item = item.clone();
        this.item.setAmount(1);
    }

    public ItemStack make(Random r) {
        ItemStack it = item.clone();
        int lo = Math.max(1, Math.min(min, max));
        int hi = Math.max(1, Math.max(min, max));
        int amount = lo == hi ? lo : lo + r.nextInt(hi - lo + 1);
        it.setAmount(Math.max(1, Math.min(amount, Math.max(1, it.getMaxStackSize()))));
        return it;
    }

    public void save(ConfigurationSection s) {
        s.set("item", item);
        s.set("chance", chance);
        s.set("min", min);
        s.set("max", max);
    }

    public static LootEntry load(ConfigurationSection s) {
        ItemStack it = s.getItemStack("item");
        if (it == null) return null;
        LootEntry e = new LootEntry(it);
        e.chance = s.getDouble("chance", 100.0);
        e.min = s.getInt("min", 1);
        e.max = s.getInt("max", 1);
        return e;
    }
}
