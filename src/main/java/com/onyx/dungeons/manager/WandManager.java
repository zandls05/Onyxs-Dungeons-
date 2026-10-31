package com.onyx.dungeons.manager;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.Cuboid;
import com.onyx.dungeons.model.DoorDef;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.model.Level;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.LocUtil;
import com.onyx.dungeons.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Varitas de seleccion punto a punto.
 * Clic izquierdo = punto 1, clic derecho = punto 2, agacharse + clic derecho = guardar.
 */
public class WandManager {
    public enum WandType {
        ZONE(Material.BLAZE_ROD, "&6&lVarita de Zona de Espera", "Selecciona la plataforma donde se paran los jugadores."),
        MAP(Material.STICK, "&b&lVarita de Mapa", "Selecciona toda el area del mapa de la dungeon."),
        LEVEL(Material.AMETHYST_SHARD, "&d&lVarita de Nivel", "Selecciona la region de un nivel (anuncia 'bajaste al nivel')."),
        DOOR(Material.GOLDEN_AXE, "&e&lVarita de Puerta", "Selecciona los bloques de una puerta que se abre con llave."),
        CHEST(Material.ECHO_SHARD, "&a&lVarita de Cofres", "Clic derecho en un cofre/barril para registrarlo (o quitarlo).");

        public final Material material;
        public final String name;
        public final String description;

        WandType(Material m, String name, String description) {
            this.material = m;
            this.name = name;
            this.description = description;
        }
    }

    /** Contexto de edicion de un administrador. */
    public static class Context {
        public String dungeon;
        public int level = 1;
        public String table;
        public Location p1;
        public Location p2;
    }

    public static final String TAG = "wand";
    private final OnyxDungeons plugin;
    private final Map<UUID, Context> contexts = new HashMap<>();

    public WandManager(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    public Context context(Player p) {
        return contexts.computeIfAbsent(p.getUniqueId(), k -> new Context());
    }

    public void give(Player p, WandType type, String dungeonId, int level, String table) {
        Context c = context(p);
        c.dungeon = dungeonId;
        c.level = level;
        if (table != null) c.table = table;
        c.p1 = null;
        c.p2 = null;
        ItemStack it = ItemUtil.make(type.material, type.name,
                "&7" + type.description,
                "",
                "&fClic izq: &7punto 1",
                "&fClic der: &7punto 2",
                "&fAgachado + clic der: &aguardar",
                "",
                "&8Dungeon: " + dungeonId + (type == WandType.CHEST ? "" : "  Nivel: " + level));
        ItemUtil.setTag(it, TAG, type.name());
        p.getInventory().addItem(it);
        p.sendMessage(Text.color(plugin.messages().prefix() + "&aRecibiste la &f" + Text.strip(type.name) + "&a para la dungeon &f" + dungeonId + "&a."));
    }

    public WandType typeOf(ItemStack it) {
        String t = ItemUtil.getTag(it, TAG);
        if (t == null) return null;
        try {
            return WandType.valueOf(t);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void handle(PlayerInteractEvent e) {
        if (e.getHand() != null && e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        WandType type = typeOf(item);
        if (type == null) return;
        e.setCancelled(true);
        if (!p.hasPermission("onyx.dungeons.admin")) return;
        Context c = context(p);
        Dungeon d = c.dungeon == null ? null : plugin.dungeons().get(c.dungeon);
        if (d == null) {
            p.sendMessage(Text.color(plugin.messages().prefix() + "&cLa dungeon de esta varita ya no existe."));
            return;
        }
        Action a = e.getAction();
        Block b = e.getClickedBlock();

        if (type == WandType.CHEST) {
            if (a == Action.RIGHT_CLICK_BLOCK && b != null) toggleChest(p, d, c, b);
            else p.sendMessage(Text.color(plugin.messages().prefix() + "&7Clic derecho sobre un cofre o barril."));
            return;
        }

        if (a == Action.LEFT_CLICK_BLOCK && b != null) {
            c.p1 = b.getLocation();
            p.sendMessage(Text.color(plugin.messages().prefix() + "&aPunto 1: &f" + LocUtil.pretty(c.p1)));
        } else if (a == Action.RIGHT_CLICK_BLOCK && b != null && !p.isSneaking()) {
            c.p2 = b.getLocation();
            p.sendMessage(Text.color(plugin.messages().prefix() + "&aPunto 2: &f" + LocUtil.pretty(c.p2)));
        } else if ((a == Action.RIGHT_CLICK_BLOCK || a == Action.RIGHT_CLICK_AIR) && p.isSneaking()) {
            save(p, d, c, type);
        }
    }

    private void toggleChest(Player p, Dungeon d, Context c, Block b) {
        Material m = b.getType();
        if (m != Material.CHEST && m != Material.TRAPPED_CHEST && m != Material.BARREL) {
            p.sendMessage(Text.color(plugin.messages().prefix() + "&cEso no es un cofre ni un barril."));
            return;
        }
        String key = LocUtil.blockKey(b);
        if (d.chests.containsKey(key)) {
            d.chests.remove(key);
            p.sendMessage(Text.color(plugin.messages().prefix() + "&eCofre quitado de la dungeon."));
        } else {
            if (c.table == null || plugin.loot().get(c.table) == null) {
                p.sendMessage(Text.color(plugin.messages().prefix() + "&cPrimero elige una tabla de loot en el menu de cofres."));
                return;
            }
            d.chests.put(key, c.table);
            p.sendMessage(Text.color(plugin.messages().prefix() + "&aCofre registrado con la tabla &f" + c.table + "&a."));
        }
        plugin.dungeons().save();
    }

    private void save(Player p, Dungeon d, Context c, WandType type) {
        if (c.p1 == null || c.p2 == null) {
            p.sendMessage(Text.color(plugin.messages().prefix() + "&cDebes elegir el punto 1 y el punto 2."));
            return;
        }
        if (c.p1.getWorld() == null || c.p2.getWorld() == null || !c.p1.getWorld().equals(c.p2.getWorld())) {
            p.sendMessage(Text.color(plugin.messages().prefix() + "&cLos dos puntos deben estar en el mismo mundo."));
            return;
        }
        Cuboid cub = Cuboid.of(c.p1, c.p2);
        String where = cub.describe();
        switch (type) {
            case ZONE:
                d.lobbyZone = cub;
                p.sendMessage(Text.color(plugin.messages().prefix() + "&aZona de espera guardada: &f" + where));
                break;
            case MAP:
                d.mapRegion = cub;
                p.sendMessage(Text.color(plugin.messages().prefix() + "&aMapa de la dungeon guardado: &f" + where));
                break;
            case LEVEL: {
                Level lv = d.getLevel(c.level);
                if (lv == null) { p.sendMessage(Text.color("&cEse nivel no existe.")); return; }
                lv.region = cub;
                p.sendMessage(Text.color(plugin.messages().prefix() + "&aRegion del nivel " + lv.number + " guardada: &f" + where));
                break;
            }
            case DOOR: {
                Level lv = d.getLevel(c.level);
                if (lv == null) { p.sendMessage(Text.color("&cEse nivel no existe.")); return; }
                if (cub.volume() > 20000) {
                    p.sendMessage(Text.color(plugin.messages().prefix() + "&cLa puerta es demasiado grande (max 20000 bloques)."));
                    return;
                }
                DoorDef door = new DoorDef(lv.nextDoorId(), cub, lv.keyId);
                door.capture();
                lv.doors.add(door);
                p.sendMessage(Text.color(plugin.messages().prefix() + "&aPuerta &f" + door.id + " &aguardada en el nivel " + lv.number
                        + " (" + cub.volume() + " bloques). Llave: &f" + (door.keyId == null ? "ninguna" : door.keyId)));
                break;
            }
            default:
                break;
        }
        c.p1 = null;
        c.p2 = null;
        plugin.dungeons().save();
    }
}
