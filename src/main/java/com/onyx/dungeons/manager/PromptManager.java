package com.onyx.dungeons.manager;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Captura el siguiente mensaje de chat de un jugador (para editar textos desde los menus). */
public class PromptManager {
    private final OnyxDungeons plugin;
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public PromptManager(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    public void ask(Player p, String question, Consumer<String> callback) {
        Bukkit.getScheduler().runTask(plugin, () -> p.closeInventory());
        pending.put(p.getUniqueId(), callback);
        p.sendMessage(Text.color(plugin.messages().prefix() + question));
        p.sendMessage(Text.color("&7Escribe &ccancelar &7para cancelar."));
    }

    public boolean has(UUID id) {
        return pending.containsKey(id);
    }

    public Consumer<String> take(UUID id) {
        return pending.remove(id);
    }

    public void clear(UUID id) {
        pending.remove(id);
    }
}
