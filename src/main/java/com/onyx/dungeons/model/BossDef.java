package com.onyx.dungeons.model;

import com.onyx.dungeons.util.LocUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

/** Definicion de un jefe. Si {@code tag} no esta vacio, el jefe es externo (lo invocas tu) y solo se detecta su muerte. */
public class BossDef {
    public String id;
    public EntityType type = EntityType.ZOMBIE;
    public String name = "&c&lJefe";
    public double health = 200;
    public double damage = 0;          // 0 = valor por defecto de la entidad
    public Location spawn;
    public String tag = "";            // tag de scoreboard para jefes externos (MythicMobs, /summon...)

    public BossDef(String id) { this.id = id; }

    public void save(ConfigurationSection s) {
        s.set("type", type.name());
        s.set("name", name);
        s.set("health", health);
        s.set("damage", damage);
        s.set("spawn", LocUtil.serialize(spawn));
        s.set("tag", tag);
    }

    public static BossDef load(String id, ConfigurationSection s) {
        BossDef b = new BossDef(id);
        try {
            b.type = EntityType.valueOf(s.getString("type", "ZOMBIE"));
        } catch (IllegalArgumentException ex) {
            b.type = EntityType.ZOMBIE;
        }
        b.name = s.getString("name", b.name);
        b.health = s.getDouble("health", 200);
        b.damage = s.getDouble("damage", 0);
        b.spawn = LocUtil.deserialize(s.getString("spawn"));
        b.tag = s.getString("tag", "");
        return b;
    }
}
