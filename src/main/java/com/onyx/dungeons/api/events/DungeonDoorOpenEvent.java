package com.onyx.dungeons.api.events;

import com.onyx.dungeons.model.Dungeon;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class DungeonDoorOpenEvent extends DungeonEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int level;
    private final String doorId;
    private final Player player;
    private boolean cancelled;

    public DungeonDoorOpenEvent(Dungeon dungeon, int level, String doorId, Player player) {
        super(dungeon);
        this.level = level;
        this.doorId = doorId;
        this.player = player;
    }

    public int getLevel() { return level; }

    public String getDoorId() { return doorId; }

    public Player getPlayer() { return player; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
