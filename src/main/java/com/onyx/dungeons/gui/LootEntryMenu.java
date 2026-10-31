package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.LootEntry;
import com.onyx.dungeons.model.LootTable;
import com.onyx.dungeons.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public class LootEntryMenu extends Menu {
    private final LootTable table;
    private final LootEntry entry;
    private final Supplier<Menu> back;

    public LootEntryMenu(OnyxDungeons plugin, Player player, LootTable table, LootEntry entry, Supplier<Menu> back) {
        super(plugin, player, "&6Editar item", 4);
        this.table = table;
        this.entry = entry;
        this.back = back;
    }

    @Override
    protected void persist() {
        plugin.loot().save();
    }

    @Override
    protected void build() {
        set(4, entry.item.clone());
        numericD(11, Material.EXPERIENCE_BOTTLE, "Probabilidad %", entry.chance, 1.0, 0.0, 100.0, v -> entry.chance = v);
        numeric(13, Material.IRON_NUGGET, "Cantidad minima", entry.min, 1, 1, 64, v -> {
            entry.min = v;
            if (entry.max < v) entry.max = v;
        });
        numeric(15, Material.GOLD_NUGGET, "Cantidad maxima", entry.max, 1, 1, 64, v -> {
            entry.max = v;
            if (entry.min > v) entry.min = v;
        });
        set(22, ItemUtil.make(Material.TNT, "&cQuitar de la tabla"), e -> {
            table.entries.remove(entry);
            plugin.loot().save();
            go(back.get());
        });
        set(31, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(back.get()));
        fillEmpty();
    }
}
