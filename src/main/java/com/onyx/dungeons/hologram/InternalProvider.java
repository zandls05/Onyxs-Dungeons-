package com.onyx.dungeons.hologram;

import com.onyx.dungeons.OnyxDungeons;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Hologramas propios hechos con armor stands invisibles (no necesita ningun otro plugin). */
public class InternalProvider implements HologramProvider {
    private static final String TAG = "holo";
    private final OnyxDungeons plugin;
    private final Map<String, List<UUID>> stands = new HashMap<>();
    private final Map<String, Location> locations = new HashMap<>();

    public InternalProvider(OnyxDungeons plugin) {
        this.plugin = plugin;
        cleanOrphans();
    }

    @Override
    public String name() {
        return "INTERNAL";
    }

    /** Elimina armor stands de hologramas de ejecuciones anteriores. */
    public void cleanOrphans() {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof ArmorStand
                        && e.getPersistentDataContainer().has(OnyxDungeons.key(TAG), PersistentDataType.STRING)) {
                    e.remove();
                }
            }
        }
    }

    private boolean valid(List<UUID> list, int size) {
        if (list == null || list.size() != size) return false;
        for (UUID u : list) {
            Entity e = Bukkit.getEntity(u);
            if (e == null || e.isDead()) return false;
        }
        return true;
    }

    @Override
    public void createOrUpdate(String id, Location loc, List<String> lines) {
        if (loc == null || loc.getWorld() == null) return;
        List<UUID> list = stands.get(id);
        Location old = locations.get(id);
        boolean moved = old == null || !old.getWorld().equals(loc.getWorld()) || old.distanceSquared(loc) > 0.01;
        if (!moved && valid(list, lines.size())) {
            for (int i = 0; i < lines.size(); i++) {
                Entity e = Bukkit.getEntity(list.get(i));
                if (e != null) e.setCustomName(lines.get(i));
            }
            return;
        }
        remove(id);
        loc.getChunk().load();
        List<UUID> created = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Location l = loc.clone().add(0, (lines.size() - 1 - i) * 0.27, 0);
            final String text = lines.get(i);
            final String holoId = id;
            ArmorStand as = loc.getWorld().spawn(l, ArmorStand.class, a -> {
                a.setVisible(false);
                a.setGravity(false);
                a.setMarker(true);
                a.setInvulnerable(true);
                a.setPersistent(false);
                a.setCustomNameVisible(true);
                a.setCustomName(text);
                a.getPersistentDataContainer().set(OnyxDungeons.key(TAG), PersistentDataType.STRING, holoId);
            });
            created.add(as.getUniqueId());
        }
        stands.put(id, created);
        locations.put(id, loc.clone());
    }

    @Override
    public void remove(String id) {
        List<UUID> list = stands.remove(id);
        locations.remove(id);
        if (list == null) return;
        for (UUID u : list) {
            Entity e = Bukkit.getEntity(u);
            if (e != null) e.remove();
        }
    }

    @Override
    public void removeAll() {
        for (String id : new ArrayList<>(stands.keySet())) remove(id);
        cleanOrphans();
    }
}
