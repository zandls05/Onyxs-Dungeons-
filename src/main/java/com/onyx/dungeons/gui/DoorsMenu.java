package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.manager.WandManager.WandType;
import com.onyx.dungeons.model.DoorDef;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.model.Level;
import com.onyx.dungeons.util.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class DoorsMenu extends Menu {
    private final Dungeon d;
    private final Level lv;

    public DoorsMenu(OnyxDungeons plugin, Player player, Dungeon d, Level lv) {
        super(plugin, player, "&6Puertas - Nivel " + lv.number, 6);
        this.d = d;
        this.lv = lv;
    }

    @Override
    protected void build() {
        for (int i = 0; i < lv.doors.size() && i < 45; i++) {
            DoorDef door = lv.doors.get(i);
            set(i, ItemUtil.make(Material.IRON_DOOR, "&6Puerta " + door.id,
                    "&7" + door.region.describe(),
                    "&7Llave: &f" + (door.keyId == null || door.keyId.isEmpty() ? "ninguna (abre al click)" : door.keyId),
                    "", "&aClic izq: &7elegir llave", "&aClic der: &7ir a la puerta",
                    "&eShift + clic izq: &7volver a capturar los bloques",
                    "&cShift + clic der: &7eliminar"), e -> {
                if (e.isShiftClick() && e.isRightClick()) {
                    door.restore();
                    lv.doors.remove(door);
                    plugin.dungeons().save();
                    refresh();
                } else if (e.isShiftClick()) {
                    door.capture();
                    plugin.dungeons().save();
                    msg("&aBloques de la puerta re-capturados (" + door.snapshot.size() + ").");
                } else if (e.isRightClick()) {
                    Location c = door.region.getCenter();
                    if (c != null) plugin.dungeons().teleportPlugin(player, c.add(0, 1, 0));
                } else {
                    go(new KeysMenu(plugin, player, id -> {
                        door.keyId = id;
                        plugin.dungeons().save();
                    }, () -> new DoorsMenu(plugin, player, d, lv)));
                }
            });
        }
        set(49, ItemUtil.make(Material.GOLDEN_AXE, "&e&lNueva puerta (varita)",
                "&7Selecciona de punto a punto los bloques", "&7de la puerta. Se abre con la llave del nivel.",
                "&7Llave por defecto: &f" + (lv.keyId == null ? "ninguna" : lv.keyId)), e -> {
            plugin.wands().give(player, WandType.DOOR, d.id, lv.number, null);
            player.closeInventory();
        });
        set(45, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(new LevelMenu(plugin, player, d, lv)));
        fillEmpty();
    }
}
