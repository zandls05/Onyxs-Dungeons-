package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends Menu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    public MainMenu(OnyxDungeons plugin, Player player) {
        super(plugin, player, "&5&lOnyx Dungeons", 6);
    }

    @Override
    protected void build() {
        List<Dungeon> list = plugin.dungeons().all();
        for (int i = 0; i < list.size() && i < SLOTS.length; i++) {
            Dungeon d = list.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("&7ID: &f" + d.id);
            lore.add("&7Estado: &f" + Text.strip(plugin.messages().text("state-" + d.state.name().toLowerCase(), null)));
            lore.add("&7Jugadores: &f" + d.minPlayers + "-" + d.maxPlayers + "  &7Cuenta: &f" + d.countdown + "s");
            lore.add((d.lobbyZone != null ? "&a✔" : "&c✘") + " &7Zona de espera");
            lore.add((d.teleport != null ? "&a✔" : "&c✘") + " &7Teletransporte");
            lore.add((d.mapRegion != null ? "&a✔" : "&c✘") + " &7Mapa");
            lore.add("&7Niveles: &f" + d.levels.size());
            lore.add("");
            lore.add("&eClic para configurar");
            set(SLOTS[i], ItemUtil.make(d.isReady() ? Material.ENDER_EYE : Material.GRAY_DYE, d.displayName, lore),
                    e -> { click(); go(new DungeonMenu(plugin, player, d)); });
        }
        set(45, ItemUtil.make(Material.EMERALD, "&a&lCrear dungeon", "&7Crea una nueva dungeon."), e ->
                plugin.prompts().ask(player, "&eEscribe el &fID &ede la nueva dungeon (letras, numeros, - y _):", text -> {
                    Dungeon d = plugin.dungeons().create(text.trim());
                    if (d == null) {
                        msg("&cID invalido o ya existe.");
                        new MainMenu(plugin, player).open();
                    } else {
                        msg("&aDungeon &f" + d.id + " &acreada.");
                        new DungeonMenu(plugin, player, d).open();
                    }
                }));
        set(47, ItemUtil.make(Material.CHEST, "&6Tablas de recompensas", "&7Items, probabilidades y cantidades."),
                e -> go(new LootListMenu(plugin, player, null, () -> new MainMenu(plugin, player))));
        set(48, ItemUtil.make(Material.TRIPWIRE_HOOK, "&eLlaves", "&7Elige que item es cada llave."),
                e -> go(new KeysMenu(plugin, player, null, () -> new MainMenu(plugin, player))));
        set(49, ItemUtil.make(Material.BOOK, "&bMensajes globales", "&7Titulos y mensajes de eventos."),
                e -> go(new MessagesMenu(plugin, player, null, () -> new MainMenu(plugin, player))));
        set(51, ItemUtil.make(Material.COMPASS, "&dSpawn de /dungeon spawn (lobby)",
                "&7Actual: &f" + com.onyx.dungeons.util.LocUtil.pretty(plugin.dungeons().getLobbySpawn()),
                "&aClic izq: &7fijar aqui", "&aClic der: &7teletransportarme"), e -> {
            if (e.isRightClick()) {
                plugin.dungeons().teleportLobby(player);
            } else {
                plugin.dungeons().setLobbySpawn(player.getLocation());
                msg("&aSpawn del lobby fijado aqui.");
                refresh();
            }
        });
        set(52, ItemUtil.make(Material.REDSTONE, "&cRecargar configuracion"), e -> {
            plugin.reloadAll();
            msg("&aConfiguracion recargada.");
            go(new MainMenu(plugin, player));
        });
        set(53, ItemUtil.make(Material.BARRIER, "&cCerrar"), e -> player.closeInventory());
        fillEmpty();
    }
}
