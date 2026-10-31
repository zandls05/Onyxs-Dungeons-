package com.onyx.dungeons.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/** Region cubica entre dos puntos (inclusiva). */
public class Cuboid {
    private final String world;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    public Cuboid(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public static Cuboid of(Location a, Location b) {
        return new Cuboid(a.getWorld().getName(), a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ());
    }

    public String getWorldName() { return world; }

    public World getWorld() { return Bukkit.getWorld(world); }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public boolean contains(Location l) {
        if (l == null || l.getWorld() == null || !l.getWorld().getName().equals(world)) return false;
        return containsPoint(l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    public boolean contains(Block b) {
        return b != null && b.getWorld().getName().equals(world) && containsPoint(b.getX(), b.getY(), b.getZ());
    }

    public boolean containsPoint(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public List<Block> getBlocks() {
        List<Block> out = new ArrayList<>();
        World w = getWorld();
        if (w == null) return out;
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    out.add(w.getBlockAt(x, y, z));
        return out;
    }

    public Location getCenter() {
        World w = getWorld();
        if (w == null) return null;
        return new Location(w, (minX + maxX + 1) / 2.0, (minY + maxY + 1) / 2.0, (minZ + maxZ + 1) / 2.0);
    }

    public void save(ConfigurationSection s) {
        s.set("world", world);
        s.set("min", minX + "," + minY + "," + minZ);
        s.set("max", maxX + "," + maxY + "," + maxZ);
    }

    public static Cuboid load(ConfigurationSection s) {
        if (s == null) return null;
        String w = s.getString("world");
        String mn = s.getString("min");
        String mx = s.getString("max");
        if (w == null || mn == null || mx == null) return null;
        try {
            String[] a = mn.split(",");
            String[] b = mx.split(",");
            return new Cuboid(w, Integer.parseInt(a[0].trim()), Integer.parseInt(a[1].trim()), Integer.parseInt(a[2].trim()),
                    Integer.parseInt(b[0].trim()), Integer.parseInt(b[1].trim()), Integer.parseInt(b[2].trim()));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public String describe() {
        return world + " (" + minX + "," + minY + "," + minZ + ") -> (" + maxX + "," + maxY + "," + maxZ + ") [" + volume() + " bloques]";
    }
}
