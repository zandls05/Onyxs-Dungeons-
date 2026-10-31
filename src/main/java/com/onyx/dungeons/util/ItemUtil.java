package com.onyx.dungeons.util;

import com.onyx.dungeons.OnyxDungeons;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ItemUtil {
    private ItemUtil() {}

    public static ItemStack make(Material m, String name, String... lore) {
        return make(m, name, Arrays.asList(lore));
    }

    public static ItemStack make(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(Text.color(name));
            if (lore != null && !lore.isEmpty()) meta.setLore(Text.color(lore));
            it.setItemMeta(meta);
        }
        return it;
    }

    public static boolean isAir(ItemStack it) {
        return it == null || it.getType() == Material.AIR || it.getAmount() <= 0;
    }

    public static ItemStack setTag(ItemStack it, String key, String value) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.getPersistentDataContainer().set(OnyxDungeons.key(key), PersistentDataType.STRING, value);
        it.setItemMeta(meta);
        return it;
    }

    public static String getTag(ItemStack it, String key) {
        if (isAir(it)) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        NamespacedKey k = OnyxDungeons.key(key);
        return meta.getPersistentDataContainer().get(k, PersistentDataType.STRING);
    }

    /** Copia del item con lineas extra anadidas al lore. */
    public static ItemStack withExtraLore(ItemStack base, List<String> extra) {
        ItemStack it = base.clone();
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.addAll(Text.color(extra));
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    public static String niceName(ItemStack it) {
        if (isAir(it)) return "aire";
        ItemMeta meta = it.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        return it.getType().name().toLowerCase().replace('_', ' ');
    }
}
