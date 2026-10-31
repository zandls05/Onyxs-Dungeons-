package com.onyx.dungeons.hologram;

import com.onyx.dungeons.OnyxDungeons;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Punto unico para crear hologramas. Elige el proveedor segun config.yml (hologram.provider):
 * AUTO | INTERNAL | DECENTHOLOGRAMS | HOLOGRAPHICDISPLAYS.
 * Tambien es una API: otros plugins pueden usar {@code OnyxDungeonsAPI.showHologram(...)}.
 */
public class HologramManager {
    private final OnyxDungeons plugin;
    private HologramProvider provider;
    private final Map<String, String> lastText = new HashMap<>();

    public HologramManager(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    public void init() {
        String want = plugin.getConfig().getString("hologram.provider", "AUTO").toUpperCase();
        HologramProvider p = null;
        try {
            if ((want.equals("AUTO") || want.equals("DECENTHOLOGRAMS")) && Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
                p = new DecentProvider();
            } else if ((want.equals("AUTO") || want.equals("HOLOGRAPHICDISPLAYS")) && Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
                p = new HolographicProvider(plugin);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("No se pudo usar la API de hologramas externa (" + ex.getMessage() + "), usando la interna.");
            p = null;
        }
        if (p == null) p = new InternalProvider(plugin);
        provider = p;
        plugin.getLogger().info("Hologramas: " + provider.name());
    }

    public String providerName() {
        return provider == null ? "NONE" : provider.name();
    }

    /** Muestra o actualiza un holograma. Solo actualiza si el texto cambio. */
    public void show(String id, Location loc, List<String> lines) {
        if (provider == null || loc == null) return;
        String joined = String.join("\n", lines) + "|" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        if (provider instanceof InternalProvider) {
            provider.createOrUpdate(id, loc, lines);   // el proveedor interno ya comprueba que sigan vivos
            lastText.put(id, joined);
            return;
        }
        if (joined.equals(lastText.get(id))) return;
        provider.createOrUpdate(id, loc, lines);
        lastText.put(id, joined);
    }

    public void remove(String id) {
        if (provider != null) provider.remove(id);
        lastText.remove(id);
    }

    public void shutdown() {
        if (provider != null) provider.removeAll();
        lastText.clear();
    }
}
