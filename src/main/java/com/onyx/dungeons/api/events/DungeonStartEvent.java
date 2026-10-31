package com.onyx.dungeons.api.events;

import com.onyx.dungeons.model.Dungeon;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.List;

/** Se lanza cuando los jugadores son teletransportados y empieza la dungeon. */
public class DungeonStartEvent extends DungeonEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final List<Player> players;

    public DungeonStartEvent(Dungeon dungeon, List<Player> players) {
        super(dungeon);
        this.players = players;
    }

    public List<Player> getPlayers() { return players; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
