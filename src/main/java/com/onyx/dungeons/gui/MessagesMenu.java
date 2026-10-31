package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.manager.MessageManager;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Lista de mensajes editables. d == null -> mensajes globales; si no, sobrescrituras de esa dungeon. */
public class MessagesMenu extends Menu {
    private final Dungeon d;
    private final Supplier<Menu> back;

    public MessagesMenu(OnyxDungeons plugin, Player player, Dungeon d, Supplier<Menu> back) {
        super(plugin, player, d == null ? "&bMensajes globales" : "&bMensajes: " + d.displayName, 6);
        this.d = d;
        this.back = back;
    }

    @Override
    protected void build() {
        int i = 0;
        for (String key : plugin.messages().keys()) {
            if (i >= 45) break;
            List<String> lore = new ArrayList<>();
            boolean over = false;
            for (String f : MessageManager.FIELDS) {
                String v = plugin.messages().field(d, key, f);
                if (plugin.messages().hasOverride(d, key, f)) over = true;
                if (v != null && !v.isEmpty()) lore.add("&7" + f + ": &f" + Text.color(v));
            }
            lore.add("");
            lore.add("&eClic para editar");
            set(i++, ItemUtil.make(over ? Material.WRITTEN_BOOK : Material.PAPER, "&b" + key + (over ? " &a(personalizado)" : ""), lore),
                    e -> go(new MessageEditMenu(plugin, player, d, key, back)));
        }
        set(49, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(back.get()));
        fillEmpty();
    }
}
