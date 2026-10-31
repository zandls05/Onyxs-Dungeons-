package com.onyx.dungeons.model;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/** Puerta de bloques definida entre dos puntos. Guarda una copia para poder restaurarla. */
public class DoorDef {
    public String id;
    public Cuboid region;
    public String keyId;
    public List<String> snapshot = new ArrayList<>();
    public transient boolean open;

    public DoorDef(String id, Cuboid region, String keyId) {
        this.id = id;
        this.region = region;
        this.keyId = keyId;
    }

    /** Guarda el estado actual de los bloques de la puerta. */
    public void capture() {
        snapshot.clear();
        for (Block b : region.getBlocks()) {
            snapshot.add(b.getX() + ";" + b.getY() + ";" + b.getZ() + ";" + b.getBlockData().getAsString());
        }
    }

    /** Restaura los bloques originales (cierra la puerta). */
    public void restore() {
        World w = region.getWorld();
        if (w == null) return;
        for (String line : snapshot) {
            String[] p = line.split(";", 4);
            if (p.length < 4) continue;
            try {
                Block b = w.getBlockAt(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                BlockData data = Bukkit.createBlockData(p[3]);
                b.setBlockData(data, false);
            } catch (RuntimeException ignored) {
                // dato corrupto: se omite
            }
        }
        open = false;
    }

    /** Elimina los bloques de la puerta. */
    public void openDoor() {
        for (Block b : region.getBlocks()) {
            b.setType(Material.AIR, false);
        }
        open = true;
    }

    public void save(ConfigurationSection s) {
        region.save(s.createSection("region"));
        s.set("key", keyId);
        s.set("snapshot", snapshot);
    }

    public static DoorDef load(String id, ConfigurationSection s) {
        Cuboid c = Cuboid.load(s.getConfigurationSection("region"));
        if (c == null) return null;
        DoorDef d = new DoorDef(id, c, s.getString("key"));
        d.snapshot = new ArrayList<>(s.getStringList("snapshot"));
        return d;
    }
}
