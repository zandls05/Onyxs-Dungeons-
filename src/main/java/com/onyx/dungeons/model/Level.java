package com.onyx.dungeons.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class Level {
    public int number;
    public String name;
    public Cuboid region;                 // opcional: al entrar se anuncia "bajaste al nivel N"
    public String keyId;                  // llave que se entrega al derrotar los jefes del nivel
    public boolean last;                  // ultimo nivel
    public final List<BossDef> bosses = new ArrayList<>();
    public final List<DoorDef> doors = new ArrayList<>();

    public Level(int number) {
        this.number = number;
        this.name = "Nivel " + number;
    }

    public BossDef getBoss(String id) {
        for (BossDef b : bosses) if (b.id.equalsIgnoreCase(id)) return b;
        return null;
    }

    public DoorDef getDoor(String id) {
        for (DoorDef d : doors) if (d.id.equalsIgnoreCase(id)) return d;
        return null;
    }

    public String nextBossId() {
        int i = 1;
        while (getBoss("boss" + i) != null) i++;
        return "boss" + i;
    }

    public String nextDoorId() {
        int i = 1;
        while (getDoor("door" + i) != null) i++;
        return "door" + i;
    }

    public void save(ConfigurationSection s) {
        s.set("name", name);
        s.set("key", keyId);
        s.set("last", last);
        if (region != null) region.save(s.createSection("region"));
        ConfigurationSection bs = s.createSection("bosses");
        for (BossDef b : bosses) b.save(bs.createSection(b.id));
        ConfigurationSection ds = s.createSection("doors");
        for (DoorDef d : doors) d.save(ds.createSection(d.id));
    }

    public static Level load(int number, ConfigurationSection s) {
        Level l = new Level(number);
        l.name = s.getString("name", l.name);
        l.keyId = s.getString("key");
        l.last = s.getBoolean("last", false);
        l.region = Cuboid.load(s.getConfigurationSection("region"));
        ConfigurationSection bs = s.getConfigurationSection("bosses");
        if (bs != null) for (String k : bs.getKeys(false)) {
            ConfigurationSection c = bs.getConfigurationSection(k);
            if (c != null) l.bosses.add(BossDef.load(k, c));
        }
        ConfigurationSection ds = s.getConfigurationSection("doors");
        if (ds != null) for (String k : ds.getKeys(false)) {
            ConfigurationSection c = ds.getConfigurationSection(k);
            if (c == null) continue;
            DoorDef d = DoorDef.load(k, c);
            if (d != null) l.doors.add(d);
        }
        return l;
    }
}
