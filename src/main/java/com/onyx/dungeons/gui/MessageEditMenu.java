package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.manager.MessageManager;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MessageEditMenu extends Menu {
    private final Dungeon d;
    private final String key;
    private final Supplier<Menu> back;

    public MessageEditMenu(OnyxDungeons plugin, Player player, Dungeon d, String key, Supplier<Menu> back) {
        super(plugin, player, "&bMensaje: " + key, 4);
        this.d = d;
        this.key = key;
        this.back = back;
    }

    private Material iconOf(String f) {
        switch (f) {
            case "title": return Material.OAK_SIGN;
            case "subtitle": return Material.SPRUCE_SIGN;
            case "chat": return Material.PAPER;
            case "actionbar": return Material.NAME_TAG;
            default: return Material.NOTE_BLOCK;
        }
    }

    @Override
    protected void build() {
        int slot = 10;
        for (String f : MessageManager.FIELDS) {
            final String field = f;
            String cur = plugin.messages().field(d, key, f);
            boolean over = plugin.messages().hasOverride(d, key, f);
            set(slot++, ItemUtil.make(iconOf(f), "&e" + f + (over ? " &a(personalizado)" : ""),
                    "&7Actual: &f" + (cur.isEmpty() ? "&8(vacio)" : Text.color(cur)),
                    "", "&aClic izq: &7editar", d == null ? "&cClic der: &7dejar vacio" : "&cClic der: &7volver al global",
                    f.equals("sound") ? "&8Ej: entity.ender_dragon.growl" : "&8Placeholders: {level} {boss} {player} {dungeon} {key} {items} {time}"), e -> {
                if (e.isRightClick()) {
                    if (d == null) plugin.messages().setGlobal(key, field, "");
                    else {
                        plugin.messages().clearOverride(d, key, field);
                        plugin.dungeons().save();
                    }
                    refresh();
                } else {
                    plugin.prompts().ask(player, "&eEscribe el nuevo valor de &f" + field + " &e(usa &&-colores, &f\\n &epara salto de linea, &fvacio &epara borrar):", text -> {
                        String v = text.trim().equalsIgnoreCase("vacio") ? "" : text;
                        if (d == null) plugin.messages().setGlobal(key, field, v);
                        else {
                            plugin.messages().setOverride(d, key, field, v);
                            plugin.dungeons().save();
                        }
                        new MessageEditMenu(plugin, player, d, key, back).open();
                    });
                }
            });
        }
        set(22, ItemUtil.make(Material.ENDER_EYE, "&b&lProbar mensaje", "&7Te lo envia a ti con datos de ejemplo."), e -> {
            Map<String, String> ph = new HashMap<>();
            ph.put("level", "2");
            ph.put("boss", "Jefe de Prueba");
            ph.put("key", "Llave de Prueba");
            ph.put("items", "3x Diamante");
            ph.put("time", "10");
            ph.put("players", "3");
            ph.put("max", "5");
            ph.put("min", "1");
            ph.put("state", "Ejemplo");
            plugin.messages().send(player, d, key, ph);
        });
        set(31, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(new MessagesMenu(plugin, player, d, back)));
        fillEmpty();
    }
}
