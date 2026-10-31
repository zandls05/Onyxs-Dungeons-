package com.onyx.dungeons.api.events;

import com.onyx.dungeons.model.Dungeon;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.List;

/** Se lanza cuando termina la dungeon (con exito o fracaso), justo antes de devolver a los jugadores. */
public class DungeonEndEvent extends DungeonEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final boolean success;
    private final List<Player> players;

    public DungeonEndEvent(Dungeon dungeon, boolean success, List<Player> players) {
        super(dungeon);
        this.success = success;
        this.players = players;
    }

    public boolean isSuccess() { return success; }

    public List<Player> getPlayers() { return players; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
