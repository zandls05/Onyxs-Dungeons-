package com.onyx.dungeons.api.events;

import com.onyx.dungeons.model.Dungeon;
import org.bukkit.event.HandlerList;

public class DungeonBossDefeatEvent extends DungeonEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int level;
    private final String bossId;
    private final boolean levelCleared;
    private final boolean finalBoss;

    public DungeonBossDefeatEvent(Dungeon dungeon, int level, String bossId, boolean levelCleared, boolean finalBoss) {
        super(dungeon);
        this.level = level;
        this.bossId = bossId;
        this.levelCleared = levelCleared;
        this.finalBoss = finalBoss;
    }

    public int getLevel() { return level; }

    public String getBossId() { return bossId; }

    /** True si con este jefe se completo el nivel. */
    public boolean isLevelCleared() { return levelCleared; }

    public boolean isFinalBoss() { return finalBoss; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
