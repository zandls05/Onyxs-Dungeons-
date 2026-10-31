package com.onyx.dungeons.hologram;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Integracion con HolographicDisplays v3 (api.HolographicDisplaysAPI) por reflexion. */
public class HolographicProvider implements HologramProvider {
    private final Object api;
    private final Method createHologram;
    private final Map<String, Object> holograms = new HashMap<>();
    private final Map<String, Location> locations = new HashMap<>();

    public HolographicProvider(Plugin plugin) throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("me.filoghost.holographicdisplays.api.HolographicDisplaysAPI");
        api = apiClass.getMethod("get", Plugin.class).invoke(null, plugin);
        createHologram = apiClass.getMethod("createHologram", Location.class);
    }

    @Override
    public String name() {
        return "HOLOGRAPHICDISPLAYS";
    }

    private static Method find(Object o, String name, int args) {
        for (Method m : o.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    @Override
    public void createOrUpdate(String id, Location loc, List<String> lines) {
        try {
            Object holo = holograms.get(id);
            Location old = locations.get(id);
            if (holo == null || old == null || !old.equals(loc)) {
                remove(id);
                holo = createHologram.invoke(api, loc);
                holograms.put(id, holo);
                locations.put(id, loc.clone());
            }
            Object hl = find(holo, "getLines", 0).invoke(holo);
            find(hl, "clear", 0).invoke(hl);
            Method append = null;
            for (Method m : hl.getClass().getMethods()) {
                if (m.getName().equals("appendText") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                    append = m;
                    append.setAccessible(true);
                    break;
                }
            }
            if (append == null) return;
            for (String line : lines) append.invoke(hl, line);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // ignorar fallos de la API externa
        }
    }

    @Override
    public void remove(String id) {
        Object holo = holograms.remove(id);
        locations.remove(id);
        if (holo == null) return;
        try {
            Method del = find(holo, "delete", 0);
            if (del != null) del.invoke(holo);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // nada
        }
    }

    @Override
    public void removeAll() {
        for (String id : new ArrayList<>(holograms.keySet())) remove(id);
    }
}
