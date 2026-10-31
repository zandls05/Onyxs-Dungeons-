package com.onyx.dungeons.util;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class EntityUtil {
    private EntityUtil() {}

    private static Attribute find(String... keys) {
        for (String k : keys) {
            try {
                Attribute a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(k));
                if (a != null) return a;
            } catch (RuntimeException ignored) {
                // sigue con la siguiente clave
            }
        }
        return null;
    }

    public static void setMaxHealth(LivingEntity e, double value) {
        Attribute a = find("max_health", "generic.max_health");
        if (a != null) {
            AttributeInstance ai = e.getAttribute(a);
            if (ai != null) ai.setBaseValue(value);
        }
        try {
            e.setHealth(value);
        } catch (IllegalArgumentException ex) {
            // vida por encima del maximo permitido: se deja la de por defecto
        }
    }

    public static double getMaxHealth(LivingEntity e) {
        Attribute a = find("max_health", "generic.max_health");
        if (a != null) {
            AttributeInstance ai = e.getAttribute(a);
            if (ai != null) return Math.max(1.0, ai.getValue());
        }
        return Math.max(1.0, e.getHealth());
    }

    public static void setAttackDamage(LivingEntity e, double value) {
        if (value <= 0) return;
        Attribute a = find("attack_damage", "generic.attack_damage");
        if (a == null) return;
        AttributeInstance ai = e.getAttribute(a);
        if (ai != null) ai.setBaseValue(value);
    }

    /** Entrega un item al jugador; si no cabe, lo suelta a sus pies. */
    public static void give(Player p, ItemStack item) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(item);
        for (ItemStack rest : left.values()) {
            Location l = p.getLocation();
            Item drop = l.getWorld().dropItem(l, rest);
            drop.setPickupDelay(0);
        }
    }
}
