package com.onyx.dungeons.api.events;

import com.onyx.dungeons.model.Dungeon;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class DungeonPlayerJoinEvent extends DungeonEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public DungeonPlayerJoinEvent(Dungeon dungeon, Player player) {
        super(dungeon);
        this.player = player;
    }

    public Player getPlayer() { return player; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
