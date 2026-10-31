package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.manager.WandManager.WandType;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.model.Level;
import com.onyx.dungeons.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class LevelMenu extends Menu {
    private final Dungeon d;
    private final Level lv;

    public LevelMenu(OnyxDungeons plugin, Player player, Dungeon d, Level lv) {
        super(plugin, player, "&5Nivel " + lv.number + ": " + lv.name, 4);
        this.d = d;
        this.lv = lv;
    }

    @Override
    protected void build() {
        set(10, ItemUtil.make(Material.WRITABLE_BOOK, "&eNombre del nivel", "&7" + lv.name, "", "&eClic para cambiar"), e ->
                plugin.prompts().ask(player, "&eEscribe el nombre del nivel:", text -> {
                    lv.name = text;
                    plugin.dungeons().save();
                    new LevelMenu(plugin, player, d, lv).open();
                }));
        set(11, ItemUtil.make(Material.AMETHYST_SHARD, "&dRegion del nivel",
                "&7" + (lv.region == null ? "No definida" : lv.region.describe()),
                "&8Al entrar se anuncia 'bajaste al nivel'. Si no la defines,",
                "&8se anuncia al abrir la ultima puerta del nivel anterior.",
                "", "&aClic izq: &7recibir varita de nivel", "&cShift + clic der: &7borrar"), e -> {
            if (e.isShiftClick() && e.isRightClick()) {
                lv.region = null;
                plugin.dungeons().save();
                refresh();
            } else {
                plugin.wands().give(player, WandType.LEVEL, d.id, lv.number, null);
                player.closeInventory();
            }
        });
        set(12, ItemUtil.make(Material.WITHER_SKELETON_SKULL, "&cJefes", "&7Cantidad: &f" + lv.bosses.size(), "", "&eClic para abrir"),
                e -> go(new BossListMenu(plugin, player, d, lv)));
        set(13, ItemUtil.make(Material.IRON_DOOR, "&6Puertas", "&7Cantidad: &f" + lv.doors.size(), "", "&eClic para abrir"),
                e -> go(new DoorsMenu(plugin, player, d, lv)));
        set(14, ItemUtil.make(Material.TRIPWIRE_HOOK, "&eLlave de este nivel",
                "&7Actual: &f" + (lv.keyId == null ? "ninguna" : lv.keyId),
                "&8Se entrega al derrotar a todos los jefes del nivel.",
                "", "&aClic izq: &7elegir llave", "&cClic der: &7quitar llave"), e -> {
            if (e.isRightClick()) {
                lv.keyId = null;
                plugin.dungeons().save();
                refresh();
            } else {
                go(new KeysMenu(plugin, player, id -> {
                    lv.keyId = id;
                    plugin.dungeons().save();
                }, () -> new LevelMenu(plugin, player, d, lv)));
            }
        });
        boolean isLast = lv.number == d.lastLevelNumber();
        set(15, ItemUtil.make(isLast ? Material.NETHER_STAR : Material.COAL, isLast ? "&6&lES EL ULTIMO NIVEL" : "&7Marcar como ultimo nivel",
                "&7Al derrotar a sus jefes la dungeon termina,",
                "&7se entregan las recompensas y todos vuelven al lobby.", "", "&eClic para marcar"), e -> {
            for (Level l : d.levels) l.last = false;
            lv.last = true;
            plugin.dungeons().save();
            refresh();
        });
        set(16, ItemUtil.make(Material.TNT, "&4Eliminar nivel"), e ->
                go(new ConfirmMenu(plugin, player, "Eliminar el nivel " + lv.number + "?", () -> {
                    d.levels.remove(lv);
                    int n = 1;
                    for (Level l : d.levels) l.number = n++;
                    plugin.dungeons().save();
                }, () -> new LevelsMenu(plugin, player, d))));
        set(31, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(new LevelsMenu(plugin, player, d)));
        fillEmpty();
    }
}
