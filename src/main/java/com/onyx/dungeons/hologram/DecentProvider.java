package com.onyx.dungeons.hologram;

import org.bukkit.Location;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Integracion con la API de DecentHolograms (DHAPI) por reflexion: no hace falta compilar contra ella. */
public class DecentProvider implements HologramProvider {
    private final Method create;
    private final Method remove;
    private final Method get;
    private final Set<String> ids = new HashSet<>();

    public DecentProvider() throws ReflectiveOperationException {
        Class<?> api = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
        create = api.getMethod("createHologram", String.class, Location.class, List.class);
        remove = api.getMethod("removeHologram", String.class);
        get = api.getMethod("getHologram", String.class);
    }

    @Override
    public String name() {
        return "DECENTHOLOGRAMS";
    }

    private String name(String id) {
        return "onyx_" + id.replace(':', '_');
    }

    @Override
    public void createOrUpdate(String id, Location loc, List<String> lines) {
        try {
            String n = name(id);
            if (get.invoke(null, n) != null) remove.invoke(null, n);
            create.invoke(null, n, loc, new ArrayList<>(lines));
            ids.add(id);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // si DecentHolograms falla no rompemos el plugin
        }
    }

    @Override
    public void remove(String id) {
        try {
            String n = name(id);
            if (get.invoke(null, n) != null) remove.invoke(null, n);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // nada
        }
        ids.remove(id);
    }

    @Override
    public void removeAll() {
        for (String id : new ArrayList<>(ids)) remove(id);
    }
}
