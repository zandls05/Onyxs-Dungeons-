package com.onyx.dungeons.model;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/** Estado del jugador antes de entrar a la dungeon, para restaurarlo al salir. */
public class PlayerSnapshot {
    public final GameMode gameMode;
    public final boolean allowFlight;
    public final boolean flying;

    public PlayerSnapshot(Player p) {
        this.gameMode = p.getGameMode();
        this.allowFlight = p.getAllowFlight();
        this.flying = p.isFlying();
    }
}
