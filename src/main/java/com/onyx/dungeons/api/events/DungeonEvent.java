package com.onyx.dungeons.api.events;

import com.onyx.dungeons.model.Dungeon;
import org.bukkit.event.Event;

/** Base de todos los eventos de Onyx Dungeons. */
public abstract class DungeonEvent extends Event {
    private final Dungeon dungeon;

    protected DungeonEvent(Dungeon dungeon) {
        this.dungeon = dungeon;
    }

    public Dungeon getDungeon() {
        return dungeon;
    }
}
