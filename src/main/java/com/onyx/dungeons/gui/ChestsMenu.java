package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.manager.WandManager.WandType;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChestsMenu extends Menu {
    private final Dungeon d;

    public ChestsMenu(OnyxDungeons plugin, Player player, Dungeon d) {
        super(plugin, player, "&6Cofres: " + d.displayName, 6);
        this.d = d;
    }

    @Override
    protected void build() {
        String selected = plugin.wands().context(player).table;
        int i = 0;
        for (Map.Entry<String, String> en : new ArrayList<>(d.chests.entrySet())) {
            if (i >= 36) break;
            final String key = en.getKey();
            set(i++, ItemUtil.make(Material.CHEST, "&6Cofre " + key.replace(';', ' '),
                    "&7Tabla: &f" + en.getValue(), "", "&aClic izq: &7ir alli", "&cClic der: &7quitar"), e -> {
                if (e.isRightClick()) {
                    d.chests.remove(key);
                    plugin.dungeons().save();
                    refresh();
                } else {
                    String[] p = key.split(";");
                    World w = Bukkit.getWorld(p[0]);
                    if (w != null && p.length >= 4) {
                        try {
                            Location l = new Location(w, Integer.parseInt(p[1]) + 0.5, Integer.parseInt(p[2]) + 1, Integer.parseInt(p[3]) + 0.5);
                            plugin.dungeons().teleportPlugin(player, l);
                        } catch (NumberFormatException ignored) {
                            msg("&cUbicacion invalida.");
                        }
                    }
                }
            });
        }
        set(45, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(new DungeonMenu(plugin, player, d)));
        set(47, ItemUtil.make(Material.BOOK, "&eTabla seleccionada: &f" + (selected == null ? "ninguna" : selected),
                "&7Los cofres que registres usaran esta tabla.", "", "&eClic para elegir"), e ->
                go(new LootListMenu(plugin, player, id -> plugin.wands().context(player).table = id, () -> new ChestsMenu(plugin, player, d))));
        List<String> lore = new ArrayList<>();
        lore.add("&7Clic derecho en un cofre o barril para registrarlo.");
        lore.add("&7Clic derecho otra vez para quitarlo.");
        lore.add("&8Al abrirlo en la dungeon se llena con items");
        lore.add("&8aleatorios de la tabla (sin repetir).");
        set(49, ItemUtil.make(Material.ECHO_SHARD, "&a&lVarita de cofres", lore), e -> {
            if (plugin.wands().context(player).table == null) {
                msg("&cPrimero elige una tabla de loot.");
                return;
            }
            plugin.wands().give(player, WandType.CHEST, d.id, 1, plugin.wands().context(player).table);
            player.closeInventory();
        });
        fillEmpty();
    }
}
