package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.model.Level;
import com.onyx.dungeons.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LevelsMenu extends Menu {
    private final Dungeon d;

    public LevelsMenu(OnyxDungeons plugin, Player player, Dungeon d) {
        super(plugin, player, "&5Niveles: " + d.displayName, 6);
        this.d = d;
    }

    @Override
    protected void build() {
        for (int i = 0; i < d.levels.size() && i < 45; i++) {
            Level lv = d.levels.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("&7Jefes: &f" + lv.bosses.size() + "  &7Puertas: &f" + lv.doors.size());
            lore.add("&7Llave: &f" + (lv.keyId == null ? "ninguna" : lv.keyId));
            lore.add("&7Region: &f" + (lv.region == null ? "no definida" : "definida"));
            if (lv.number == d.lastLevelNumber()) lore.add("&6&lULTIMO NIVEL");
            lore.add("");
            lore.add("&eClic para editar");
            set(i, ItemUtil.make(lv.number == d.lastLevelNumber() ? Material.NETHER_STAR : Material.LADDER,
                    "&aNivel " + lv.number + " &7- &f" + lv.name, lore), e -> go(new LevelMenu(plugin, player, d, lv)));
        }
        set(49, ItemUtil.make(Material.EMERALD, "&a&lAnadir nivel"), e -> {
            int n = 1;
            for (Level l : d.levels) n = Math.max(n, l.number + 1);
            d.levels.add(new Level(n));
            plugin.dungeons().save();
            refresh();
        });
        set(45, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(new DungeonMenu(plugin, player, d)));
        fillEmpty();
    }
}
