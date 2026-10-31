package com.onyx.dungeons.api.events;

import com.onyx.dungeons.model.Dungeon;
import org.bukkit.event.HandlerList;

public class DungeonLevelReachEvent extends DungeonEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int level;

    public DungeonLevelReachEvent(Dungeon dungeon, int level) {
        super(dungeon);
        this.level = level;
    }

    public int getLevel() { return level; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
