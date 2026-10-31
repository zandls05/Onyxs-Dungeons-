package com.onyx.dungeons.model;

import com.onyx.dungeons.util.LocUtil;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Dungeon {
    public enum State { IDLE, COUNTDOWN, RUNNING, ENDING }

    public final String id;
    public String displayName;
    public Cuboid lobbyZone;                 // plataforma de espera
    public Location teleport;                // donde aparecen al iniciar
    public Location hologramLoc;
    public List<String> hologramLines = new ArrayList<>(Arrays.asList(
            "&5&l{name}", "&7Estado: {state}", "&fJugadores: &a{players}&7/&a{max}", "&7Tiempo: &e{time}"));
    public int minPlayers = 1;
    public int maxPlayers = 5;
    public int countdown = 30;
    public Cuboid mapRegion;
    public final List<Level> levels = new ArrayList<>();
    /** Cofres de recompensa: clave de bloque -> id de tabla de loot. */
    public final Map<String, String> chests = new LinkedHashMap<>();
    public String finalTable;
    public final List<String> finalCommands = new ArrayList<>();
    /** Mensajes propios de esta dungeon: clave -> campo -> texto. */
    public final Map<String, Map<String, String>> messages = new HashMap<>();

    // ---- runtime (no se guarda) ----
    public State state = State.IDLE;
    public final LinkedHashSet<UUID> waiting = new LinkedHashSet<>();
    public final LinkedHashSet<UUID> notifiedFull = new LinkedHashSet<>();
    public int remainingTicks;
    public BossBar waitBar;
    public DungeonRun run;
    public String lastHologram = "";

    public Dungeon(String id) {
        this.id = id;
        this.displayName = "&5" + id;
    }

    public Level getLevel(int number) {
        for (Level l : levels) if (l.number == number) return l;
        return null;
    }

    public int lastLevelNumber() {
        int max = 0;
        for (Level l : levels) {
            if (l.last) return l.number;
            max = Math.max(max, l.number);
        }
        return max;
    }

    public boolean isReady() {
        return lobbyZone != null && teleport != null && !levels.isEmpty();
    }

    public void save(ConfigurationSection s) {
        s.set("name", displayName);
        if (lobbyZone != null) lobbyZone.save(s.createSection("lobby-zone"));
        s.set("teleport", LocUtil.serialize(teleport));
        s.set("hologram.location", LocUtil.serialize(hologramLoc));
        s.set("hologram.lines", hologramLines);
        s.set("min-players", minPlayers);
        s.set("max-players", maxPlayers);
        s.set("countdown", countdown);
        if (mapRegion != null) mapRegion.save(s.createSection("map-region"));
        ConfigurationSection ls = s.createSection("levels");
        for (Level l : levels) l.save(ls.createSection(String.valueOf(l.number)));
        ConfigurationSection cs = s.createSection("chests");
        int i = 0;
        for (Map.Entry<String, String> e : chests.entrySet()) {
            ConfigurationSection c = cs.createSection(String.valueOf(i++));
            c.set("loc", e.getKey());
            c.set("table", e.getValue());
        }
        s.set("final.table", finalTable);
        s.set("final.commands", finalCommands);
        ConfigurationSection ms = s.createSection("messages");
        for (Map.Entry<String, Map<String, String>> e : messages.entrySet()) {
            for (Map.Entry<String, String> f : e.getValue().entrySet()) {
                ms.set(e.getKey() + "." + f.getKey(), f.getValue());
            }
        }
    }

    public static Dungeon load(String id, ConfigurationSection s) {
        Dungeon d = new Dungeon(id);
        d.displayName = s.getString("name", d.displayName);
        d.lobbyZone = Cuboid.load(s.getConfigurationSection("lobby-zone"));
        d.teleport = LocUtil.deserialize(s.getString("teleport"));
        d.hologramLoc = LocUtil.deserialize(s.getString("hologram.location"));
        List<String> hl = s.getStringList("hologram.lines");
        if (!hl.isEmpty()) d.hologramLines = new ArrayList<>(hl);
        d.minPlayers = Math.max(1, s.getInt("min-players", 1));
        d.maxPlayers = Math.max(d.minPlayers, s.getInt("max-players", 5));
        d.countdown = Math.max(1, s.getInt("countdown", 30));
        d.mapRegion = Cuboid.load(s.getConfigurationSection("map-region"));
        ConfigurationSection ls = s.getConfigurationSection("levels");
        if (ls != null) {
            List<Integer> nums = new ArrayList<>();
            for (String k : ls.getKeys(false)) {
                try { nums.add(Integer.parseInt(k)); } catch (NumberFormatException ignored) { }
            }
            java.util.Collections.sort(nums);
            for (int n : nums) {
                ConfigurationSection c = ls.getConfigurationSection(String.valueOf(n));
                if (c != null) d.levels.add(Level.load(n, c));
            }
        }
        ConfigurationSection cs = s.getConfigurationSection("chests");
        if (cs != null) for (String k : cs.getKeys(false)) {
            ConfigurationSection c = cs.getConfigurationSection(k);
            if (c != null && c.getString("loc") != null) d.chests.put(c.getString("loc"), c.getString("table", ""));
        }
        d.finalTable = s.getString("final.table");
        d.finalCommands.addAll(s.getStringList("final.commands"));
        ConfigurationSection ms = s.getConfigurationSection("messages");
        if (ms != null) for (String k : ms.getKeys(false)) {
            ConfigurationSection c = ms.getConfigurationSection(k);
            if (c == null) continue;
            Map<String, String> m = new HashMap<>();
            for (String f : c.getKeys(false)) m.put(f, c.getString(f, ""));
            d.messages.put(k, m);
        }
        return d;
    }
}
