package com.onyx.dungeons.model;

import org.bukkit.boss.BossBar;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Partida en curso de una dungeon. */
public class DungeonRun {
    public final Dungeon dungeon;
    /** Jugadores vivos dentro de la dungeon. */
    public final Set<UUID> participants = new LinkedHashSet<>();
    /** Todos los jugadores que entraron al inicio. */
    public final Set<UUID> everJoined = new LinkedHashSet<>();
    public final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    public int currentLevel = 1;
    public final Set<Integer> clearedLevels = new HashSet<>();
    /** UUID de entidad -> "nivel:idJefe" */
    public final Map<UUID, String> bossEntities = new HashMap<>();
    public final Map<UUID, BossBar> bossBars = new HashMap<>();
    public final Set<String> openedChests = new HashSet<>();
    public final Map<String, Inventory> personalInventories = new HashMap<>();
    public final Set<String> bossesKilled = new HashSet<>();
    public long startedAt = System.currentTimeMillis();
    public boolean ending = false;
    public boolean completed = false;

    public DungeonRun(Dungeon dungeon) {
        this.dungeon = dungeon;
    }

    public int elapsedSeconds() {
        return (int) ((System.currentTimeMillis() - startedAt) / 1000L);
    }
}
