package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.BossDef;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.model.Level;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.LocUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BossListMenu extends Menu {
    private final Dungeon d;
    private final Level lv;

    public BossListMenu(OnyxDungeons plugin, Player player, Dungeon d, Level lv) {
        super(plugin, player, "&cJefes - Nivel " + lv.number, 6);
        this.d = d;
        this.lv = lv;
    }

    static Material eggOf(BossDef b) {
        Material m = Material.matchMaterial(b.type.name() + "_SPAWN_EGG");
        return m == null ? Material.ZOMBIE_HEAD : m;
    }

    @Override
    protected void build() {
        for (int i = 0; i < lv.bosses.size() && i < 45; i++) {
            BossDef b = lv.bosses.get(i);
            boolean external = b.tag != null && !b.tag.isEmpty();
            set(i, ItemUtil.make(eggOf(b), b.name,
                    "&7ID: &f" + b.id,
                    external ? "&7Jefe externo, tag: &f" + b.tag : "&7Tipo: &f" + b.type.name(),
                    "&7Vida: &f" + b.health + (b.damage > 0 ? "  &7Dano: &f" + b.damage : ""),
                    "&7Aparece en: &f" + LocUtil.pretty(b.spawn),
                    "", "&eClic para editar"), e -> go(new BossMenu(plugin, player, d, lv, b)));
        }
        set(47, ItemUtil.make(Material.ZOMBIE_SPAWN_EGG, "&a&lAnadir jefe aqui",
                "&7Crea un jefe que aparece en tu posicion", "&7cuando los jugadores llegan a este nivel."), e -> {
            BossDef b = new BossDef(lv.nextBossId());
            b.spawn = player.getLocation();
            lv.bosses.add(b);
            plugin.dungeons().save();
            go(new BossMenu(plugin, player, d, lv, b));
        });
        set(48, ItemUtil.make(Material.NAME_TAG, "&b&lAnadir jefe externo (tag)",
                "&7Para jefes invocados por otro plugin o /summon.",
                "&7Detecta su muerte por el scoreboard tag."), e ->
                plugin.prompts().ask(player, "&eEscribe el &ftag &edel jefe externo (ej: &fboss_cripta_1&e). Dale ese tag con &f/tag @e add <tag>&e:", text -> {
                    BossDef b = new BossDef(lv.nextBossId());
                    b.tag = text.trim();
                    b.name = "&fJefe externo (" + b.tag + ")";
                    lv.bosses.add(b);
                    plugin.dungeons().save();
                    new BossListMenu(plugin, player, d, lv).open();
                }));
        set(49, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(new LevelMenu(plugin, player, d, lv)));
        fillEmpty();
    }
}
