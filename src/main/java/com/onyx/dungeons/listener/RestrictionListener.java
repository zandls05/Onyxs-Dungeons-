package com.onyx.dungeons.listener;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.Dungeon;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Bloquea tp/tpa/back/fly dentro de la dungeon y evita entrar al mapa sin pasar por la plataforma. */
public class RestrictionListener implements Listener {
    private final OnyxDungeons plugin;

    public RestrictionListener(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    private Set<String> lower(List<String> list) {
        Set<String> out = new HashSet<>();
        for (String s : list) out.add(s.toLowerCase(Locale.ROOT).replace("/", ""));
        return out;
    }

    private boolean bypass(Player p) {
        return p.hasPermission("onyx.dungeons.bypass");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        Dungeon d = plugin.dungeons().getDungeonOf(p);
        if (d == null || bypass(p)) return;
        String msg = e.getMessage();
        if (msg.length() < 2) return;
        String cmd = msg.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        int colon = cmd.indexOf(':');
        if (colon >= 0) cmd = cmd.substring(colon + 1);
        Set<String> always = lower(plugin.getConfig().getStringList("restrictions.always-allowed-commands"));
        if (always.contains(cmd)) return;
        String mode = plugin.getConfig().getString("restrictions.command-mode", "BLACKLIST").toUpperCase(Locale.ROOT);
        boolean block;
        if (mode.equals("WHITELIST")) block = !lower(plugin.getConfig().getStringList("restrictions.allowed-commands")).contains(cmd);
        else block = lower(plugin.getConfig().getStringList("restrictions.blocked-commands")).contains(cmd);
        if (block) {
            e.setCancelled(true);
            plugin.messages().send(p, d, "command-blocked");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (plugin.dungeons().isPluginTeleport(p.getUniqueId()) || bypass(p)) return;
        Dungeon d = plugin.dungeons().getDungeonOf(p);
        if (d != null) {
            Set<String> causes = lower(plugin.getConfig().getStringList("restrictions.blocked-teleport-causes"));
            if (causes.contains(e.getCause().name().toLowerCase(Locale.ROOT))) {
                e.setCancelled(true);
                plugin.messages().send(p, d, "teleport-blocked");
            }
            return;
        }
        if (plugin.getConfig().getBoolean("restrictions.block-entry-without-session", true)
                && e.getTo() != null && plugin.dungeons().findByMapRegion(e.getTo()) != null) {
            e.setCancelled(true);
            plugin.messages().send(p, null, "entry-blocked");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFlight(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (!e.isFlying() || bypass(p)) return;
        Dungeon d = plugin.dungeons().getDungeonOf(p);
        if (d == null || !plugin.getConfig().getBoolean("restrictions.block-fly", true)) return;
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        e.setCancelled(true);
        p.setFlying(false);
        p.setAllowFlight(false);
        plugin.messages().send(p, d, "fly-blocked");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
        Player p = e.getPlayer();
        if (plugin.dungeons().isParticipant(p)) {
            plugin.dungeons().onMove(p, to);
            return;
        }
        if (bypass(p) || !plugin.getConfig().getBoolean("restrictions.block-entry-without-session", true)) return;
        Dungeon d = plugin.dungeons().findByMapRegion(to);
        if (d != null && plugin.dungeons().findByMapRegion(from) == null) {
            e.setTo(from);
            plugin.messages().send(p, d, "entry-blocked");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (bypass(p) || !plugin.getConfig().getBoolean("dungeon.protect-blocks", true)) return;
        Dungeon d = plugin.dungeons().getDungeonOf(p);
        if (d != null && d.mapRegion != null && d.mapRegion.contains(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (bypass(p) || !plugin.getConfig().getBoolean("dungeon.protect-blocks", true)) return;
        Dungeon d = plugin.dungeons().getDungeonOf(p);
        if (d != null && d.mapRegion != null && d.mapRegion.contains(e.getBlock())) e.setCancelled(true);
    }
}
