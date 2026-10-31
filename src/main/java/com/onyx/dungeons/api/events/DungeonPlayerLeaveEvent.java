package com.onyx.dungeons.api.events;

import com.onyx.dungeons.model.Dungeon;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class DungeonPlayerLeaveEvent extends DungeonEvent {
    public enum Reason { DEATH, LEAVE, QUIT, END, KICK }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Reason reason;

    public DungeonPlayerLeaveEvent(Dungeon dungeon, Player player, Reason reason) {
        super(dungeon);
        this.player = player;
        this.reason = reason;
    }

    public Player getPlayer() { return player; }

    public Reason getReason() { return reason; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
