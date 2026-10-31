package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.manager.WandManager.WandType;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.LocUtil;
import com.onyx.dungeons.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DungeonMenu extends Menu {
    private final Dungeon d;

    public DungeonMenu(OnyxDungeons plugin, Player player, Dungeon d) {
        super(plugin, player, "&5Dungeon: " + d.displayName, 6);
        this.d = d;
    }

    @Override
    protected void build() {
        // --- zona de espera
        set(10, ItemUtil.make(Material.BLAZE_ROD, "&6Zona de espera (plataforma)",
                "&7" + (d.lobbyZone == null ? "No definida" : d.lobbyZone.describe()),
                "", "&aClic izq: &7recibir varita de zona", "&cShift + clic der: &7borrar zona"), e -> {
            if (e.isShiftClick() && e.isRightClick()) {
                d.lobbyZone = null;
                plugin.dungeons().save();
                refresh();
            } else {
                plugin.wands().give(player, WandType.ZONE, d.id, 1, null);
                player.closeInventory();
            }
        });
        // --- teletransporte
        set(11, ItemUtil.make(Material.ENDER_PEARL, "&bTeletransporte de entrada",
                "&7" + LocUtil.pretty(d.teleport), "", "&aClic izq: &7fijar aqui", "&aClic der: &7ir alli"), e -> {
            if (e.isRightClick()) {
                if (d.teleport != null) plugin.dungeons().teleportPlugin(player, d.teleport);
            } else {
                d.teleport = player.getLocation();
                plugin.dungeons().save();
                msg("&aTeletransporte de entrada fijado aqui.");
                refresh();
            }
        });
        // --- holograma
        set(12, ItemUtil.make(Material.NAME_TAG, "&dHolograma",
                "&7Ubicacion: &f" + LocUtil.pretty(d.hologramLoc),
                "&7Proveedor: &f" + plugin.holograms().providerName(),
                "&7Lineas: &f" + d.hologramLines.size(),
                "", "&aClic izq: &7colocar aqui", "&aShift + clic izq: &7editar lineas",
                "&cClic der: &7quitar", "&8Placeholders: {name} {state} {players} {max} {min} {time} {level}"), e -> {
            if (e.isRightClick()) {
                d.hologramLoc = null;
                plugin.holograms().remove("dungeon_" + d.id);
                plugin.dungeons().save();
                refresh();
            } else if (e.isShiftClick()) {
                plugin.prompts().ask(player, "&eEscribe las lineas separadas por &f| &e(ej: &f&5&l{name}|&7Estado: {state}|{players}/{max}&e):", text -> {
                    d.hologramLines = new ArrayList<>(Arrays.asList(text.split("\\|")));
                    plugin.dungeons().save();
                    plugin.dungeons().refreshHologram(d);
                    msg("&aLineas del holograma actualizadas.");
                    new DungeonMenu(plugin, player, d).open();
                });
            } else {
                d.hologramLoc = player.getLocation().clone().add(0, 2.0, 0);
                plugin.dungeons().save();
                plugin.dungeons().refreshHologram(d);
                msg("&aHolograma colocado.");
                refresh();
            }
        });
        // --- mapa
        set(13, ItemUtil.make(Material.FILLED_MAP, "&bMapa de la dungeon",
                "&7" + (d.mapRegion == null ? "No definido" : d.mapRegion.describe()),
                "&8Se usa para bloquear entradas por tp y proteger bloques.",
                "", "&aClic izq: &7recibir varita de mapa", "&cShift + clic der: &7borrar"), e -> {
            if (e.isShiftClick() && e.isRightClick()) {
                d.mapRegion = null;
                plugin.dungeons().save();
                refresh();
            } else {
                plugin.wands().give(player, WandType.MAP, d.id, 1, null);
                player.closeInventory();
            }
        });
        set(14, ItemUtil.make(Material.LADDER, "&aNiveles, jefes y puertas",
                "&7Niveles: &f" + d.levels.size(), "", "&eClic para abrir"),
                e -> go(new LevelsMenu(plugin, player, d)));
        set(15, ItemUtil.make(Material.CHEST, "&6Cofres de recompensa",
                "&7Registrados: &f" + d.chests.size(), "", "&eClic para abrir"),
                e -> go(new ChestsMenu(plugin, player, d)));
        set(16, ItemUtil.make(Material.DIAMOND, "&bRecompensa final",
                "&7Tabla: &f" + (d.finalTable == null ? "ninguna" : d.finalTable),
                "&7Comandos: &f" + d.finalCommands.size(),
                "&8Comandos: usa %player%. Separados por ;",
                "", "&aClic izq: &7elegir tabla", "&aClic der: &7editar comandos", "&cShift + clic der: &7quitar tabla"), e -> {
            if (e.isShiftClick() && e.isRightClick()) {
                d.finalTable = null;
                plugin.dungeons().save();
                refresh();
            } else if (e.isRightClick()) {
                plugin.prompts().ask(player, "&eEscribe los comandos de consola separados por &f; &e(ej: &fgive %player% diamond 5; eco give %player% 500&e) o &fninguno&e:", text -> {
                    d.finalCommands.clear();
                    if (!text.trim().equalsIgnoreCase("ninguno")) {
                        for (String c : text.split(";")) if (!c.trim().isEmpty()) d.finalCommands.add(c.trim());
                    }
                    plugin.dungeons().save();
                    new DungeonMenu(plugin, player, d).open();
                });
            } else {
                go(new LootListMenu(plugin, player, id -> {
                    d.finalTable = id;
                    plugin.dungeons().save();
                }, () -> new DungeonMenu(plugin, player, d)));
            }
        });

        // --- numeros
        numeric(19, Material.PLAYER_HEAD, "Jugadores minimos", d.minPlayers, 1, 1, 100, v -> {
            d.minPlayers = v;
            if (d.maxPlayers < v) d.maxPlayers = v;
        });
        numeric(20, Material.PLAYER_HEAD, "Jugadores maximos", d.maxPlayers, 1, 1, 100, v -> {
            d.maxPlayers = v;
            if (d.minPlayers > v) d.minPlayers = v;
        });
        numeric(21, Material.CLOCK, "Cuenta atras (segundos)", d.countdown, 5, 3, 3600, v -> d.countdown = v);
        set(22, ItemUtil.make(Material.WRITABLE_BOOK, "&eNombre visible", "&7" + d.displayName, "", "&eClic para cambiar"), e ->
                plugin.prompts().ask(player, "&eEscribe el nuevo nombre (acepta colores &&):", text -> {
                    d.displayName = text;
                    plugin.dungeons().save();
                    new DungeonMenu(plugin, player, d).open();
                }));
        set(23, ItemUtil.make(Material.BOOK, "&bMensajes de esta dungeon", "&7Sobrescribe los mensajes globales."),
                e -> go(new MessagesMenu(plugin, player, d, () -> new DungeonMenu(plugin, player, d))));

        // --- control
        List<String> status = new ArrayList<>();
        status.add("&7Estado: &f" + Text.strip(plugin.messages().text("state-" + d.state.name().toLowerCase(), null)));
        status.add(d.isReady() ? "&aLista para jugar" : "&cFalta: zona de espera, teletransporte o niveles");
        set(25, ItemUtil.make(Material.LIME_DYE, "&a&lIniciar ahora", status), e -> {
            if (plugin.dungeons().forceStart(d, player)) msg("&aDungeon iniciada.");
            else msg("&cNo se pudo iniciar (esta incompleta o ya en curso).");
            player.closeInventory();
        });
        set(26, ItemUtil.make(Material.RED_DYE, "&c&lDetener / reiniciar",
                "&7Termina la partida, cierra puertas y devuelve jugadores."), e -> {
            plugin.dungeons().stop(d);
            msg("&eDungeon detenida y reiniciada.");
            refresh();
        });
        set(34, ItemUtil.make(Material.TNT, "&4Eliminar dungeon", "&7No se puede deshacer."), e ->
                go(new ConfirmMenu(plugin, player, "Eliminar la dungeon " + d.id + "?", () -> plugin.dungeons().delete(d.id),
                        () -> new MainMenu(plugin, player))));
        set(49, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(new MainMenu(plugin, player)));
        fillEmpty();
    }
}
