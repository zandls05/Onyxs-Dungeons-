package com.onyx.dungeons.listener;

import com.onyx.dungeons.OnyxDungeons;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.function.Consumer;

public class PromptListener implements Listener {
    private final OnyxDungeons plugin;

    public PromptListener(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!plugin.prompts().has(e.getPlayer().getUniqueId())) return;
        e.setCancelled(true);
        Consumer<String> cb = plugin.prompts().take(e.getPlayer().getUniqueId());
        if (cb == null) return;
        final String msg = e.getMessage();
        final org.bukkit.entity.Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel")) {
                plugin.messages().chat(p, "prompt-cancelled");
                return;
            }
            cb.accept(msg);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.prompts().clear(e.getPlayer().getUniqueId());
    }
}
