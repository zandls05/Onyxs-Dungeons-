package com.onyx.dungeons.listener;

import com.onyx.dungeons.OnyxDungeons;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class WandListener implements Listener {
    private final OnyxDungeons plugin;

    public WandListener(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        plugin.wands().handle(e);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        if (plugin.wands().typeOf(e.getPlayer().getInventory().getItemInMainHand()) != null) e.setCancelled(true);
    }
}
