package com.onyx.dungeons.api;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.model.LootTable;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * API publica de Onyx Dungeons para otros plugins.
 * <pre>
 *   if (OnyxDungeonsAPI.isInDungeon(player)) { ... }
 *   OnyxDungeonsAPI.showHologram("mi-holo", location, List.of("&6Hola", "&7Mundo"));
 * </pre>
 * Eventos disponibles (paquete com.onyx.dungeons.api.events): DungeonStartEvent, DungeonEndEvent,
 * DungeonPlayerJoinEvent, DungeonPlayerLeaveEvent, DungeonLevelReachEvent, DungeonBossDefeatEvent, DungeonDoorOpenEvent.
 */
public final class OnyxDungeonsAPI {
    private static final Random RANDOM = new Random();

    private OnyxDungeonsAPI() {}

    public static OnyxDungeons getPlugin() {
        return OnyxDungeons.getInstance();
    }

    public static boolean isInDungeon(Player p) {
        return OnyxDungeons.getInstance().dungeons().isParticipant(p);
    }

    /** Dungeon en la que esta el jugador, o null. */
    public static Dungeon getDungeon(Player p) {
        return OnyxDungeons.getInstance().dungeons().getDungeonOf(p);
    }

    public static Dungeon getDungeon(String id) {
        return OnyxDungeons.getInstance().dungeons().get(id);
    }

    public static Collection<Dungeon> getDungeons() {
        return Collections.unmodifiableList(OnyxDungeons.getInstance().dungeons().all());
    }

    /** Inicia la dungeon con los jugadores que esten en la plataforma. */
    public static boolean startDungeon(String id) {
        Dungeon d = getDungeon(id);
        return d != null && OnyxDungeons.getInstance().dungeons().forceStart(d, null);
    }

    public static void stopDungeon(String id) {
        Dungeon d = getDungeon(id);
        if (d != null) OnyxDungeons.getInstance().dungeons().stop(d);
    }

    /** Crea o actualiza un holograma usando el proveedor configurado (interno, DecentHolograms o HolographicDisplays). */
    public static void showHologram(String id, Location location, List<String> lines) {
        OnyxDungeons.getInstance().holograms().show("api_" + id, location, lines);
    }

    public static void removeHologram(String id) {
        OnyxDungeons.getInstance().holograms().remove("api_" + id);
    }

    /** Crea el item de una llave configurada (o null si no existe). */
    public static ItemStack createKey(String keyId, int amount) {
        return OnyxDungeons.getInstance().keys().create(keyId, amount);
    }

    /** Tira una tabla de loot (sin repetir items si la tabla es 'unique'). */
    public static List<ItemStack> rollLoot(String tableId) {
        LootTable t = OnyxDungeons.getInstance().loot().get(tableId);
        return t == null ? Collections.<ItemStack>emptyList() : t.roll(RANDOM);
    }
}
