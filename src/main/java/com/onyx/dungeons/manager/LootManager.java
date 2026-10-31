package com.onyx.dungeons.manager;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.LootTable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LootManager {
    private final OnyxDungeons plugin;
    private final Map<String, LootTable> tables = new LinkedHashMap<>();
    private File file;

    public LootManager(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    public void load() {
        tables.clear();
        file = new File(plugin.getDataFolder(), "loot.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = cfg.getConfigurationSection("tables");
        if (s == null) return;
        for (String id : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(id);
            if (c != null) tables.put(id.toLowerCase(), LootTable.load(id.toLowerCase(), c));
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (LootTable t : tables.values()) t.save(cfg.createSection("tables." + t.id));
        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar loot.yml: " + ex.getMessage());
        }
    }

    public List<String> ids() {
        return new ArrayList<>(tables.keySet());
    }

    public LootTable get(String id) {
        return id == null ? null : tables.get(id.toLowerCase());
    }

    public LootTable create(String id) {
        LootTable t = new LootTable(id.toLowerCase());
        tables.put(t.id, t);
        save();
        return t;
    }

    public void delete(String id) {
        tables.remove(id.toLowerCase());
        save();
    }
}
