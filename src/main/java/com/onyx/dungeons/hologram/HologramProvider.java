package com.onyx.dungeons.hologram;

import org.bukkit.Location;

import java.util.List;

/** Proveedor de hologramas (propio, DecentHolograms o HolographicDisplays). */
public interface HologramProvider {
    String name();

    /** Crea el holograma o actualiza sus lineas si ya existe. */
    void createOrUpdate(String id, Location loc, List<String> lines);

    void remove(String id);

    void removeAll();
}
