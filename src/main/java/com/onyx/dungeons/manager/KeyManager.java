package com.onyx.dungeons.manager;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.util.ItemUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Llaves personalizables: tu eliges el item. Se marcan con un tag para distinguirlas. */
public class KeyManager {
    public static final String TAG = "key";
    private final OnyxDungeons plugin;
    private final Map<String, ItemStack> keys = new LinkedHashMap<>();
    private File file;

    public KeyManager(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    public void load() {
        keys.clear();
        file = new File(plugin.getDataFolder(), "keys.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = cfg.getConfigurationSection("keys");
        if (s == null) return;
        for (String id : s.getKeys(false)) {
            ItemStack it = s.getItemStack(id);
            if (it != null) keys.put(id.toLowerCase(), it);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, ItemStack> e : keys.entrySet()) cfg.set("keys." + e.getKey(), e.getValue());
        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar keys.yml: " + ex.getMessage());
        }
    }

    public List<String> ids() {
        return new ArrayList<>(keys.keySet());
    }

    public boolean exists(String id) {
        return id != null && keys.containsKey(id.toLowerCase());
    }

    public void define(String id, ItemStack template) {
        ItemStack t = template.clone();
        t.setAmount(1);
        keys.put(id.toLowerCase(), t);
        save();
    }

    public void delete(String id) {
        keys.remove(id.toLowerCase());
        save();
    }

    public ItemStack template(String id) {
        ItemStack t = id == null ? null : keys.get(id.toLowerCase());
        return t == null ? null : t.clone();
    }

    /** Crea una copia entregable de la llave, marcada con su id. */
    public ItemStack create(String id, int amount) {
        ItemStack t = template(id);
        if (t == null) return null;
        t.setAmount(Math.max(1, amount));
        return ItemUtil.setTag(t, TAG, id.toLowerCase());
    }

    public String keyIdOf(ItemStack it) {
        return ItemUtil.getTag(it, TAG);
    }

    public boolean isKey(ItemStack it, String id) {
        String k = keyIdOf(it);
        return k != null && id != null && k.equalsIgnoreCase(id);
    }
}
