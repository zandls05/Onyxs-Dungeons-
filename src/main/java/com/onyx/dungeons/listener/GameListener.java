package com.onyx.dungeons.listener;

import com.onyx.dungeons.OnyxDungeons;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Muertes, reaparicion, jefes, puertas con llave y cofres de recompensa. */
public class GameListener implements Listener {
    private final OnyxDungeons plugin;

    public GameListener(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!plugin.dungeons().onDeath(p)) return;
        if (plugin.getConfig().getBoolean("dungeon.keep-inventory-on-death", false)) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            e.setKeepLevel(true);
            e.setDroppedExp(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Location loc = plugin.dungeons().onRespawn(e.getPlayer());
        if (loc != null) e.setRespawnLocation(loc);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.dungeons().onQuit(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.dungeons().onJoin(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        if (le instanceof Player) return;
        plugin.dungeons().onEntityDeath(le, le.getKiller());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        Player p = e.getPlayer();
        if (plugin.wands().typeOf(e.getItem()) != null) return;
        if (!plugin.dungeons().isParticipant(p)) return;
        if (plugin.dungeons().tryOpenDoor(p, b, p.getInventory().getItemInMainHand())) {
            e.setCancelled(true);
            return;
        }
        if (plugin.dungeons().handleChestInteract(p, b)) e.setCancelled(true);
    }
}
