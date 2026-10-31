package com.onyx.dungeons.manager;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.util.Text;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Mensajes personalizables (messages.yml). Cada mensaje tiene los campos
 * title, subtitle, chat, actionbar y sound. Cada dungeon puede sobrescribirlos.
 */
public class MessageManager {
    public static final String[] FIELDS = {"title", "subtitle", "chat", "actionbar", "sound"};

    private final OnyxDungeons plugin;
    private File file;
    private YamlConfiguration cfg;
    private YamlConfiguration defaults;

    public MessageManager(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        cfg = YamlConfiguration.loadConfiguration(file);
        try {
            if (plugin.getResource("messages.yml") != null) {
                defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8));
            }
        } catch (RuntimeException ex) {
            defaults = null;
        }
    }

    public void save() {
        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar messages.yml: " + ex.getMessage());
        }
    }

    public String prefix() {
        return cfg.getString("prefix", defaults != null ? defaults.getString("prefix", "") : "");
    }

    /** Claves de mensajes editables (excluye el prefijo). */
    public List<String> keys() {
        TreeSet<String> out = new TreeSet<>();
        if (defaults != null) out.addAll(defaults.getKeys(false));
        out.addAll(cfg.getKeys(false));
        out.remove("prefix");
        return new ArrayList<>(out);
    }

    public String globalField(String key, String field) {
        String v = cfg.getString(key + "." + field);
        if (v == null && defaults != null) v = defaults.getString(key + "." + field);
        return v == null ? "" : v;
    }

    public String field(Dungeon d, String key, String field) {
        if (d != null) {
            Map<String, String> m = d.messages.get(key);
            if (m != null && m.containsKey(field)) return m.get(field);
        }
        return globalField(key, field);
    }

    public boolean hasOverride(Dungeon d, String key, String field) {
        if (d == null) return false;
        Map<String, String> m = d.messages.get(key);
        return m != null && m.containsKey(field);
    }

    public void setGlobal(String key, String field, String value) {
        cfg.set(key + "." + field, value);
        save();
    }

    public void setOverride(Dungeon d, String key, String field, String value) {
        d.messages.computeIfAbsent(key, k -> new HashMap<>()).put(field, value);
    }

    public void clearOverride(Dungeon d, String key, String field) {
        Map<String, String> m = d.messages.get(key);
        if (m == null) return;
        m.remove(field);
        if (m.isEmpty()) d.messages.remove(key);
    }

    public Map<String, String> base(String... kv) {
        Map<String, String> ph = new HashMap<>();
        ph.put("prefix", prefix());
        for (int i = 0; i + 1 < kv.length; i += 2) ph.put(kv[i], kv[i + 1]);
        return ph;
    }

    /** Envia un mensaje completo (titulo, chat, actionbar, sonido) a un jugador. */
    public void send(Player p, Dungeon d, String key, Map<String, String> ph) {
        if (p == null || !p.isOnline()) return;
        Map<String, String> m = ph == null ? new HashMap<>() : new HashMap<>(ph);
        m.putIfAbsent("prefix", prefix());
        if (d != null) m.putIfAbsent("dungeon", Text.color(d.displayName));
        m.putIfAbsent("player", p.getName());
        String title = Text.apply(field(d, key, "title"), m);
        String subtitle = Text.apply(field(d, key, "subtitle"), m);
        String chat = Text.apply(field(d, key, "chat"), m);
        String bar = Text.apply(field(d, key, "actionbar"), m);
        String sound = field(d, key, "sound");
        if (!title.isEmpty() || !subtitle.isEmpty()) p.sendTitle(title, subtitle, 10, 60, 20);
        if (!chat.isEmpty()) {
            for (String line : chat.split("\\\\n|\n")) p.sendMessage(line);
        }
        if (!bar.isEmpty()) p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(bar));
        if (sound != null && !sound.isEmpty()) {
            try {
                p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
            } catch (RuntimeException ignored) {
                // sonido invalido
            }
        }
    }

    public void send(Player p, Dungeon d, String key) {
        send(p, d, key, null);
    }

    /** Solo el campo chat, para respuestas de comandos (CommandSender). */
    public void chat(CommandSender s, String key, Map<String, String> ph) {
        Map<String, String> m = ph == null ? new HashMap<>() : new HashMap<>(ph);
        m.putIfAbsent("prefix", prefix());
        String chat = Text.apply(globalField(key, "chat"), m);
        if (chat.isEmpty()) return;
        for (String line : chat.split("\\\\n|\n")) s.sendMessage(line);
    }

    public void chat(CommandSender s, String key) {
        chat(s, key, null);
    }

    public String text(String key, Map<String, String> ph) {
        Map<String, String> m = ph == null ? new HashMap<>() : new HashMap<>(ph);
        m.putIfAbsent("prefix", prefix());
        return Text.apply(globalField(key, "chat"), m);
    }
}
