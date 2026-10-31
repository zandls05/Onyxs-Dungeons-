package com.onyx.dungeons.manager;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.api.events.DungeonBossDefeatEvent;
import com.onyx.dungeons.api.events.DungeonDoorOpenEvent;
import com.onyx.dungeons.api.events.DungeonEndEvent;
import com.onyx.dungeons.api.events.DungeonLevelReachEvent;
import com.onyx.dungeons.api.events.DungeonPlayerJoinEvent;
import com.onyx.dungeons.api.events.DungeonPlayerLeaveEvent;
import com.onyx.dungeons.api.events.DungeonStartEvent;
import com.onyx.dungeons.model.BossDef;
import com.onyx.dungeons.model.DoorDef;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.model.DungeonRun;
import com.onyx.dungeons.model.Level;
import com.onyx.dungeons.model.LootTable;
import com.onyx.dungeons.model.PlayerSnapshot;
import com.onyx.dungeons.util.EntityUtil;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.LocUtil;
import com.onyx.dungeons.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Logica principal: zona de espera, cuenta atras, partida, niveles, jefes, llaves, puertas y cofres. */
public class DungeonManager {
    public static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,24}");
    public static final String BOSS_TAG = "boss";

    private final OnyxDungeons plugin;
    private final Map<String, Dungeon> dungeons = new LinkedHashMap<>();
    private final Random random = new Random();
    private final Set<UUID> pluginTeleports = new HashSet<>();
    private final Set<UUID> pendingLobby = new HashSet<>();
    private final Map<UUID, PlayerSnapshot> deadSnapshots = new HashMap<>();
    private final Map<UUID, Dungeon> deadFrom = new HashMap<>();
    private Location lobbySpawn;
    private File file;
    private File dataFile;
    private BukkitTask task;
    private int tickCounter;

    public DungeonManager(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ persistencia

    public void load() {
        dungeons.clear();
        file = new File(plugin.getDataFolder(), "dungeons.yml");
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection ds = cfg.getConfigurationSection("dungeons");
        if (ds != null) {
            for (String id : ds.getKeys(false)) {
                ConfigurationSection c = ds.getConfigurationSection(id);
                if (c != null) dungeons.put(id.toLowerCase(), Dungeon.load(id.toLowerCase(), c));
            }
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        lobbySpawn = LocUtil.deserialize(data.getString("lobby-spawn"));
        pendingLobby.clear();
        for (String s : data.getStringList("pending")) {
            try {
                pendingLobby.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // uuid invalido
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Dungeon d : dungeons.values()) d.save(cfg.createSection("dungeons." + d.id));
        YamlConfiguration data = new YamlConfiguration();
        data.set("lobby-spawn", LocUtil.serialize(lobbySpawn));
        List<String> pend = new ArrayList<>();
        for (UUID u : pendingLobby) pend.add(u.toString());
        data.set("pending", pend);
        try {
            cfg.save(file);
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar los datos: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ CRUD

    public Dungeon get(String id) {
        return id == null ? null : dungeons.get(id.toLowerCase());
    }

    public List<Dungeon> all() {
        return new ArrayList<>(dungeons.values());
    }

    public List<String> ids() {
        return new ArrayList<>(dungeons.keySet());
    }

    public Dungeon create(String id) {
        if (!ID_PATTERN.matcher(id).matches() || dungeons.containsKey(id.toLowerCase())) return null;
        Dungeon d = new Dungeon(id.toLowerCase());
        d.minPlayers = Math.max(1, plugin.getConfig().getInt("dungeon.default-min-players", 1));
        d.maxPlayers = Math.max(d.minPlayers, plugin.getConfig().getInt("dungeon.default-max-players", 5));
        d.countdown = Math.max(1, plugin.getConfig().getInt("dungeon.default-countdown", 30));
        d.levels.add(new Level(1));
        d.levels.get(0).last = true;
        dungeons.put(d.id, d);
        save();
        return d;
    }

    public void delete(String id) {
        Dungeon d = get(id);
        if (d == null) return;
        if (d.run != null) endRun(d, false);
        cancelCountdown(d, false);
        plugin.holograms().remove("dungeon_" + d.id);
        dungeons.remove(d.id);
        save();
    }

    public Location getLobbySpawn() {
        if (lobbySpawn != null && lobbySpawn.getWorld() != null) return lobbySpawn.clone();
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public boolean hasLobbySpawn() {
        return lobbySpawn != null;
    }

    public void setLobbySpawn(Location l) {
        this.lobbySpawn = l == null ? null : l.clone();
        save();
    }

    // ------------------------------------------------------------------ consultas

    public Dungeon getDungeonOf(UUID u) {
        for (Dungeon d : dungeons.values()) {
            if (d.run != null && d.run.participants.contains(u)) return d;
        }
        return null;
    }

    public Dungeon getDungeonOf(Player p) {
        return getDungeonOf(p.getUniqueId());
    }

    public boolean isParticipant(Player p) {
        return getDungeonOf(p.getUniqueId()) != null;
    }

    public Dungeon findByMapRegion(Location l) {
        for (Dungeon d : dungeons.values()) {
            if (d.mapRegion != null && d.mapRegion.contains(l)) return d;
        }
        return null;
    }

    public boolean isPluginTeleport(UUID u) {
        return pluginTeleports.contains(u);
    }

    public void teleportPlugin(Player p, Location loc) {
        pluginTeleports.add(p.getUniqueId());
        try {
            p.teleport(loc);
        } finally {
            pluginTeleports.remove(p.getUniqueId());
        }
    }

    public void teleportLobby(Player p) {
        teleportPlugin(p, getLobbySpawn());
    }

    public Map<String, String> placeholders(Dungeon d) {
        Map<String, String> m = new HashMap<>();
        m.put("prefix", plugin.messages().prefix());
        String name = Text.color(d.displayName);
        m.put("dungeon", name);
        m.put("name", name);
        m.put("id", d.id);
        boolean inRun = d.run != null;
        int players = inRun ? d.run.participants.size() : d.waiting.size();
        m.put("players", String.valueOf(players));
        m.put("max", String.valueOf(d.maxPlayers));
        m.put("min", String.valueOf(d.minPlayers));
        String time;
        if (d.state == Dungeon.State.COUNTDOWN) time = Text.time((d.remainingTicks + 19) / 20);
        else if (inRun) time = Text.time(d.run.elapsedSeconds());
        else time = "--:--";
        m.put("time", time);
        m.put("level", inRun ? String.valueOf(d.run.currentLevel) : "-");
        m.put("state", plugin.messages().text("state-" + d.state.name().toLowerCase(), null));
        return m;
    }

    // ------------------------------------------------------------------ bucle principal

    public void startTask() {
        stopTask();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void stopTask() {
        if (task != null) task.cancel();
        task = null;
    }

    private void tick() {
        tickCounter++;
        for (Dungeon d : new ArrayList<>(dungeons.values())) {
            try {
                if (d.state == Dungeon.State.IDLE || d.state == Dungeon.State.COUNTDOWN) tickLobby(d);
                else if (d.state == Dungeon.State.RUNNING) tickRunning(d);
                if (tickCounter % 2 == 0) updateHologram(d);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Error en la dungeon " + d.id + ": " + ex);
            }
        }
        if (tickCounter % 2 == 0) enforceRules();
    }

    private void tickLobby(Dungeon d) {
        if (!d.isReady()) {
            if (d.state == Dungeon.State.COUNTDOWN) cancelCountdown(d, true);
            return;
        }
        Set<UUID> inside = new java.util.LinkedHashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (!d.lobbyZone.contains(p.getLocation())) continue;
            if (isParticipant(p)) continue;
            if (!p.hasPermission("onyx.dungeons.play")) continue;
            inside.add(p.getUniqueId());
        }
        d.waiting.retainAll(inside);
        d.notifiedFull.retainAll(inside);
        for (UUID u : inside) {
            if (d.waiting.contains(u)) continue;
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            if (d.waiting.size() >= d.maxPlayers) {
                if (d.notifiedFull.add(u)) plugin.messages().send(p, d, "zone-full", placeholders(d));
                continue;
            }
            d.waiting.add(u);
            plugin.messages().send(p, d, "zone-joined", placeholders(d));
        }
        if (d.state == Dungeon.State.IDLE) {
            if (!d.waiting.isEmpty() && d.waiting.size() >= d.minPlayers) startCountdown(d);
            return;
        }
        // COUNTDOWN
        boolean cancelBelow = plugin.getConfig().getBoolean("dungeon.cancel-countdown-if-below-min", true);
        if (d.waiting.isEmpty() || (cancelBelow && d.waiting.size() < d.minPlayers)) {
            cancelCountdown(d, true);
            return;
        }
        d.remainingTicks -= 10;
        updateWaitBar(d);
        if (d.remainingTicks % 20 == 0) {
            int secs = d.remainingTicks / 20;
            if (secs > 0 && secs <= plugin.getConfig().getInt("dungeon.countdown-tick-seconds", 5)) {
                Map<String, String> ph = placeholders(d);
                for (UUID u : d.waiting) plugin.messages().send(Bukkit.getPlayer(u), d, "countdown-tick", ph);
            }
        }
        if (d.remainingTicks <= 0) startRun(d);
    }

    private void startCountdown(Dungeon d) {
        d.state = Dungeon.State.COUNTDOWN;
        d.remainingTicks = d.countdown * 20;
        updateWaitBar(d);
        Map<String, String> ph = placeholders(d);
        for (UUID u : d.waiting) plugin.messages().send(Bukkit.getPlayer(u), d, "countdown-start", ph);
    }

    public void cancelCountdown(Dungeon d, boolean notify) {
        if (d.waitBar != null) {
            d.waitBar.removeAll();
            d.waitBar.setVisible(false);
        }
        boolean was = d.state == Dungeon.State.COUNTDOWN;
        if (d.state == Dungeon.State.COUNTDOWN) d.state = Dungeon.State.IDLE;
        d.remainingTicks = 0;
        if (notify && was) {
            Map<String, String> ph = placeholders(d);
            for (UUID u : d.waiting) plugin.messages().send(Bukkit.getPlayer(u), d, "countdown-cancel", ph);
        }
    }

    private void updateWaitBar(Dungeon d) {
        if (d.waitBar == null) d.waitBar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SEGMENTED_10);
        d.waitBar.setVisible(true);
        d.waitBar.setTitle(Text.apply(plugin.messages().field(d, "countdown-bar", "chat"), placeholders(d)));
        double total = Math.max(1, d.countdown * 20.0);
        d.waitBar.setProgress(Math.max(0.0, Math.min(1.0, d.remainingTicks / total)));
        d.waitBar.setColor(d.remainingTicks <= 200 ? BarColor.RED : BarColor.PURPLE);
        for (Player p : new ArrayList<>(d.waitBar.getPlayers())) {
            if (!d.waiting.contains(p.getUniqueId())) d.waitBar.removePlayer(p);
        }
        for (UUID u : d.waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && !d.waitBar.getPlayers().contains(p)) d.waitBar.addPlayer(p);
        }
    }

    private void updateHologram(Dungeon d) {
        if (d.hologramLoc == null || d.hologramLoc.getWorld() == null || d.hologramLines.isEmpty()) return;
        Map<String, String> ph = placeholders(d);
        List<String> lines = new ArrayList<>();
        for (String l : d.hologramLines) lines.add(Text.apply(l, ph));
        plugin.holograms().show("dungeon_" + d.id, d.hologramLoc, lines);
    }

    public void refreshHologram(Dungeon d) {
        plugin.holograms().remove("dungeon_" + d.id);
        updateHologram(d);
    }

    private void tickRunning(Dungeon d) {
        DungeonRun run = d.run;
        if (run == null) return;
        // barras de vida de los jefes
        for (Map.Entry<UUID, BossBar> e : new ArrayList<>(run.bossBars.entrySet())) {
            Entity ent = Bukkit.getEntity(e.getKey());
            if (ent instanceof LivingEntity le && !le.isDead()) {
                e.getValue().setProgress(Math.max(0.0, Math.min(1.0, le.getHealth() / EntityUtil.getMaxHealth(le))));
            } else {
                e.getValue().removeAll();
                run.bossBars.remove(e.getKey());
            }
        }
        int limit = plugin.getConfig().getInt("dungeon.time-limit-minutes", 0);
        if (limit > 0 && !run.ending && run.elapsedSeconds() > limit * 60) {
            for (UUID u : new ArrayList<>(run.participants)) plugin.messages().send(Bukkit.getPlayer(u), d, "time-up", placeholders(d));
            endRun(d, false);
        }
    }

    /** Aplica las reglas: sin fly y modo de juego forzado para los participantes. */
    private void enforceRules() {
        GameMode forced = forcedGameMode();
        boolean noFly = plugin.getConfig().getBoolean("restrictions.block-fly", true);
        for (Dungeon d : dungeons.values()) {
            if (d.run == null) continue;
            for (UUID u : d.run.participants) {
                Player p = Bukkit.getPlayer(u);
                if (p == null || p.hasPermission("onyx.dungeons.bypass")) continue;
                if (forced != null && p.getGameMode() != forced) p.setGameMode(forced);
                if (noFly && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                        && (p.isFlying() || p.getAllowFlight())) {
                    p.setFlying(false);
                    p.setAllowFlight(false);
                    plugin.messages().send(p, d, "fly-blocked");
                }
            }
        }
    }

    private GameMode forcedGameMode() {
        String s = plugin.getConfig().getString("dungeon.force-gamemode", "SURVIVAL");
        if (s == null || s.equalsIgnoreCase("NONE")) return null;
        try {
            return GameMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return GameMode.SURVIVAL;
        }
    }

    // ------------------------------------------------------------------ inicio / fin

    /** Inicio forzado por un admin. Si la plataforma esta vacia, entra el propio admin. */
    public boolean forceStart(Dungeon d, Player admin) {
        if (!d.isReady() || d.state == Dungeon.State.RUNNING || d.state == Dungeon.State.ENDING) return false;
        if (d.waiting.isEmpty() && admin != null) d.waiting.add(admin.getUniqueId());
        if (d.waiting.isEmpty()) return false;
        startRun(d);
        return d.run != null;
    }

    private void startRun(Dungeon d) {
        List<Player> players = new ArrayList<>();
        for (UUID u : new ArrayList<>(d.waiting)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline() && !p.isDead() && players.size() < d.maxPlayers) players.add(p);
        }
        d.waiting.clear();
        cancelCountdown(d, false);
        if (players.isEmpty() || d.teleport == null || d.teleport.getWorld() == null) return;

        DungeonRun run = new DungeonRun(d);
        d.run = run;
        d.state = Dungeon.State.RUNNING;
        prepareArena(d);
        GameMode forced = forcedGameMode();
        for (Player p : players) {
            run.participants.add(p.getUniqueId());
            run.everJoined.add(p.getUniqueId());
            run.snapshots.put(p.getUniqueId(), new PlayerSnapshot(p));
            if (forced != null) p.setGameMode(forced);
            p.setFlying(false);
            p.setAllowFlight(false);
            teleportPlugin(p, d.teleport);
            plugin.messages().send(p, d, "dungeon-start", placeholders(d));
            Bukkit.getPluginManager().callEvent(new DungeonPlayerJoinEvent(d, p));
        }
        reachLevel(run, 1, false);
        Bukkit.getPluginManager().callEvent(new DungeonStartEvent(d, players));
    }

    /** Cierra puertas, vacia los cofres registrados y elimina jefes sobrantes. */
    private void prepareArena(Dungeon d) {
        for (Level lv : d.levels) for (DoorDef door : lv.doors) {
            if (!door.snapshot.isEmpty()) door.restore();
            door.open = false;
        }
        for (String key : d.chests.keySet()) {
            Block b = blockOfKey(key);
            if (b != null && b.getState() instanceof Container c) c.getInventory().clear();
        }
        removeLeftoverBosses(d);
    }

    public void removeLeftoverBosses(Dungeon d) {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                String v = e.getPersistentDataContainer().get(OnyxDungeons.key(BOSS_TAG), PersistentDataType.STRING);
                if (v != null && v.startsWith(d.id + ":")) e.remove();
            }
        }
    }

    public void stop(Dungeon d) {
        if (d.run != null) endRun(d, false);
        else cancelCountdown(d, true);
    }

    public void endRun(Dungeon d, boolean success) {
        DungeonRun run = d.run;
        if (run == null) return;
        run.ending = true;
        d.state = Dungeon.State.ENDING;
        List<Player> online = new ArrayList<>();
        for (UUID u : run.participants) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) online.add(p);
        }
        Bukkit.getPluginManager().callEvent(new DungeonEndEvent(d, success, online));
        if (!success) {
            for (Player p : online) plugin.messages().send(p, d, "dungeon-failed", placeholders(d));
        }
        for (UUID u : new ArrayList<>(run.participants)) {
            run.participants.remove(u);
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                PlayerSnapshot s = run.snapshots.get(u);
                if (s != null) restore(p, s);
                Bukkit.getPluginManager().callEvent(new DungeonPlayerLeaveEvent(d, p, DungeonPlayerLeaveEvent.Reason.END));
                teleportLobby(p);
            } else {
                pendingLobby.add(u);
            }
        }
        cleanupRun(d, run);
        d.run = null;
        d.state = Dungeon.State.IDLE;
        d.waiting.clear();
        save();
    }

    private void cleanupRun(Dungeon d, DungeonRun run) {
        for (BossBar bar : run.bossBars.values()) bar.removeAll();
        run.bossBars.clear();
        for (UUID u : run.bossEntities.keySet()) {
            Entity e = Bukkit.getEntity(u);
            if (e != null) e.remove();
        }
        removeLeftoverBosses(d);
        for (Level lv : d.levels) for (DoorDef door : lv.doors) {
            if (!door.snapshot.isEmpty()) door.restore();
        }
        for (String key : d.chests.keySet()) {
            Block b = blockOfKey(key);
            if (b != null && b.getState() instanceof Container c) c.getInventory().clear();
        }
        run.personalInventories.clear();
    }

    private void restore(Player p, PlayerSnapshot s) {
        if (plugin.getConfig().getBoolean("dungeon.restore-gamemode-on-exit", true)) p.setGameMode(s.gameMode);
        p.setAllowFlight(s.allowFlight);
        if (s.allowFlight && s.flying) p.setFlying(true);
    }

    // ------------------------------------------------------------------ jugadores

    /** Saca a un jugador de la dungeon. */
    public void removeParticipant(Dungeon d, UUID u, DungeonPlayerLeaveEvent.Reason reason) {
        DungeonRun run = d.run;
        if (run == null || !run.participants.remove(u)) return;
        Player p = Bukkit.getPlayer(u);
        PlayerSnapshot snap = run.snapshots.get(u);
        if (p != null) {
            for (BossBar bar : run.bossBars.values()) bar.removePlayer(p);
            Bukkit.getPluginManager().callEvent(new DungeonPlayerLeaveEvent(d, p, reason));
            switch (reason) {
                case DEATH:
                    if (snap != null) deadSnapshots.put(u, snap);
                    deadFrom.put(u, d);
                    break;
                case QUIT:
                    if (snap != null) restore(p, snap);
                    pendingLobby.add(u);
                    save();
                    break;
                default:
                    if (snap != null) restore(p, snap);
                    teleportLobby(p);
                    break;
            }
        } else {
            pendingLobby.add(u);
        }
        if (run.participants.isEmpty()) {
            endRun(d, run.completed);
        } else if (reason == DungeonPlayerLeaveEvent.Reason.DEATH) {
            Map<String, String> ph = placeholders(d);
            ph.put("player", p == null ? "?" : p.getName());
            for (UUID o : run.participants) plugin.messages().send(Bukkit.getPlayer(o), d, "teammate-died", ph);
        }
    }

    public void leave(Player p) {
        Dungeon d = getDungeonOf(p);
        if (d == null) return;
        plugin.messages().send(p, d, "player-left", placeholders(d));
        removeParticipant(d, p.getUniqueId(), DungeonPlayerLeaveEvent.Reason.LEAVE);
    }

    public void onQuit(Player p) {
        Dungeon d = getDungeonOf(p);
        if (d != null) removeParticipant(d, p.getUniqueId(), DungeonPlayerLeaveEvent.Reason.QUIT);
        for (Dungeon x : dungeons.values()) x.waiting.remove(p.getUniqueId());
    }

    public void onJoin(Player p) {
        if (pendingLobby.remove(p.getUniqueId())) {
            save();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) teleportLobby(p);
            }, 5L);
        }
    }

    /** Devuelve true si el jugador murio dentro de una dungeon. */
    public boolean onDeath(Player p) {
        Dungeon d = getDungeonOf(p);
        if (d == null) return false;
        removeParticipant(d, p.getUniqueId(), DungeonPlayerLeaveEvent.Reason.DEATH);
        return true;
    }

    /** Si el jugador murio en una dungeon devuelve el lugar de reaparicion (lobby), si no null. */
    public Location onRespawn(Player p) {
        Dungeon d = deadFrom.remove(p.getUniqueId());
        PlayerSnapshot s = deadSnapshots.remove(p.getUniqueId());
        if (d == null) return null;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (s != null) restore(p, s);
            plugin.messages().send(p, d, "player-died", placeholders(d));
        }, 2L);
        return getLobbySpawn();
    }

    // ------------------------------------------------------------------ niveles y jefes

    /** Marca el nivel como alcanzado: anuncia y hace aparecer sus jefes. */
    public void reachLevel(DungeonRun run, int number, boolean announce) {
        Dungeon d = run.dungeon;
        Level lv = d.getLevel(number);
        if (lv == null) return;
        if (announce && number <= run.currentLevel) return;
        run.currentLevel = Math.max(run.currentLevel, number);
        if (announce) {
            Map<String, String> ph = placeholders(d);
            ph.put("level", String.valueOf(number));
            ph.put("levelname", Text.color(lv.name));
            for (UUID u : run.participants) plugin.messages().send(Bukkit.getPlayer(u), d, "level-reached", ph);
        }
        spawnBosses(run, lv);
        Bukkit.getPluginManager().callEvent(new DungeonLevelReachEvent(d, number));
    }

    private void spawnBosses(DungeonRun run, Level lv) {
        Dungeon d = run.dungeon;
        boolean bars = plugin.getConfig().getBoolean("boss-bar.enabled", true);
        for (BossDef b : lv.bosses) {
            if (b.tag != null && !b.tag.isEmpty()) continue;
            if (b.spawn == null || b.spawn.getWorld() == null) continue;
            Entity e = b.spawn.getWorld().spawnEntity(b.spawn, b.type);
            if (!(e instanceof LivingEntity le)) {
                e.remove();
                continue;
            }
            le.setCustomName(Text.color(b.name));
            le.setCustomNameVisible(true);
            le.setRemoveWhenFarAway(false);
            EntityUtil.setMaxHealth(le, b.health);
            EntityUtil.setAttackDamage(le, b.damage);
            le.getPersistentDataContainer().set(OnyxDungeons.key(BOSS_TAG), PersistentDataType.STRING, d.id + ":" + lv.number + ":" + b.id);
            run.bossEntities.put(le.getUniqueId(), lv.number + ":" + b.id);
            if (bars) {
                BossBar bar = Bukkit.createBossBar(Text.color(b.name), BarColor.RED, BarStyle.SOLID);
                for (UUID u : run.participants) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) bar.addPlayer(p);
                }
                run.bossBars.put(le.getUniqueId(), bar);
            }
        }
    }

    /** Llamado cuando muere cualquier entidad viva. */
    public void onEntityDeath(LivingEntity e, Player killer) {
        String data = e.getPersistentDataContainer().get(OnyxDungeons.key(BOSS_TAG), PersistentDataType.STRING);
        if (data != null) {
            String[] p = data.split(":");
            if (p.length >= 3) {
                Dungeon d = get(p[0]);
                if (d != null && d.run != null) {
                    try {
                        bossDefeated(d.run, Integer.parseInt(p[1]), p[2], e, killer);
                    } catch (NumberFormatException ignored) {
                        // dato invalido
                    }
                }
            }
            return;
        }
        // jefes externos: detectados por tag
        Set<String> tags = e.getScoreboardTags();
        if (tags.isEmpty()) return;
        for (Dungeon d : dungeons.values()) {
            if (d.run == null || d.run.ending) continue;
            Level lv = d.getLevel(d.run.currentLevel);
            if (lv == null) continue;
            for (BossDef b : lv.bosses) {
                if (b.tag != null && !b.tag.isEmpty() && tags.contains(b.tag)) {
                    bossDefeated(d.run, lv.number, b.id, e, killer);
                    return;
                }
            }
            if (tags.contains("onyxboss_" + d.id + "_" + lv.number)) {
                bossDefeated(d.run, lv.number, "tag", e, killer);
                return;
            }
        }
    }

    private void bossDefeated(DungeonRun run, int levelNumber, String bossId, LivingEntity e, Player killer) {
        Dungeon d = run.dungeon;
        Level lv = d.getLevel(levelNumber);
        if (lv == null || run.ending) return;
        if (!run.bossesKilled.add(levelNumber + ":" + bossId) && !bossId.equals("tag")) return;
        run.bossEntities.remove(e.getUniqueId());
        BossBar bar = run.bossBars.remove(e.getUniqueId());
        if (bar != null) bar.removeAll();

        String bossName = e.getCustomName() != null ? e.getCustomName() : bossId;
        boolean cleared = !run.clearedLevels.contains(levelNumber);
        for (BossDef b : lv.bosses) {
            if (!run.bossesKilled.contains(levelNumber + ":" + b.id)) cleared = false;
        }
        if (bossId.equals("tag") && lv.bosses.isEmpty()) cleared = !run.clearedLevels.contains(levelNumber);
        boolean isFinal = levelNumber == d.lastLevelNumber();

        Map<String, String> ph = placeholders(d);
        ph.put("boss", bossName);
        ph.put("level", String.valueOf(levelNumber));
        ph.put("levelname", Text.color(lv.name));
        for (UUID u : run.participants) plugin.messages().send(Bukkit.getPlayer(u), d, "boss-defeated", ph);
        Bukkit.getPluginManager().callEvent(new DungeonBossDefeatEvent(d, levelNumber, bossId, cleared, isFinal));

        if (!cleared) return;
        run.clearedLevels.add(levelNumber);
        giveLevelKey(run, lv, e.getLocation(), killer);
        if (isFinal) completeDungeon(run);
        else {
            for (UUID u : run.participants) plugin.messages().send(Bukkit.getPlayer(u), d, "level-cleared", ph);
        }
    }

    private void giveLevelKey(DungeonRun run, Level lv, Location where, Player killer) {
        if (lv.keyId == null || lv.keyId.isEmpty()) return;
        ItemStack key = plugin.keys().create(lv.keyId, 1);
        if (key == null) {
            plugin.getLogger().warning("La llave '" + lv.keyId + "' del nivel " + lv.number + " no existe.");
            return;
        }
        String mode = plugin.getConfig().getString("keys.give-mode", "ALL").toUpperCase();
        Map<String, String> ph = placeholders(run.dungeon);
        ph.put("key", ItemUtil.niceName(key));
        if (mode.equals("DROP")) {
            where.getWorld().dropItemNaturally(where, key);
            for (UUID u : run.participants) plugin.messages().send(Bukkit.getPlayer(u), run.dungeon, "key-dropped", ph);
        } else if (mode.equals("KILLER") && killer != null && run.participants.contains(killer.getUniqueId())) {
            EntityUtil.give(killer, key);
            plugin.messages().send(killer, run.dungeon, "key-received", ph);
        } else {
            for (UUID u : run.participants) {
                Player p = Bukkit.getPlayer(u);
                if (p == null) continue;
                EntityUtil.give(p, plugin.keys().create(lv.keyId, 1));
                plugin.messages().send(p, run.dungeon, "key-received", ph);
            }
        }
    }

    // ------------------------------------------------------------------ puertas

    /** Intenta abrir una puerta con la llave en mano. Devuelve true si el bloque pertenece a una puerta (el evento se cancela). */
    public boolean tryOpenDoor(Player p, Block b, ItemStack hand) {
        Dungeon d = getDungeonOf(p);
        if (d == null || d.run == null || d.run.ending) return false;
        DungeonRun run = d.run;
        for (Level lv : d.levels) {
            for (DoorDef door : lv.doors) {
                if (door.open || !door.region.contains(b)) continue;
                Map<String, String> ph = placeholders(d);
                ph.put("level", String.valueOf(lv.number));
                if (door.keyId != null && !door.keyId.isEmpty()) {
                    String held = plugin.keys().keyIdOf(hand);
                    if (held == null) {
                        plugin.messages().send(p, d, "door-needs-key", ph);
                        return true;
                    }
                    if (!held.equalsIgnoreCase(door.keyId)) {
                        plugin.messages().send(p, d, "wrong-key", ph);
                        return true;
                    }
                }
                DungeonDoorOpenEvent ev = new DungeonDoorOpenEvent(d, lv.number, door.id, p);
                Bukkit.getPluginManager().callEvent(ev);
                if (ev.isCancelled()) return true;
                door.openDoor();
                if (door.keyId != null && !door.keyId.isEmpty() && plugin.getConfig().getBoolean("keys.consume-on-open", true)) {
                    hand.setAmount(hand.getAmount() - 1);
                }
                p.getWorld().playSound(b.getLocation(), "block.iron_door.open", 1.0f, 0.8f);
                for (UUID u : run.participants) plugin.messages().send(Bukkit.getPlayer(u), d, "door-opened", ph);
                // si todas las puertas del nivel estan abiertas, se desbloquea el siguiente
                boolean all = true;
                for (DoorDef other : lv.doors) if (!other.open) all = false;
                if (all && lv.number == run.currentLevel) {
                    Level next = d.getLevel(lv.number + 1);
                    if (next != null && next.region == null) reachLevel(run, next.number, true);
                }
                return true;
            }
        }
        return false;
    }

    /** Al moverse: si entra a la region de un nivel mas profundo, se anuncia. */
    public void onMove(Player p, Location to) {
        Dungeon d = getDungeonOf(p);
        if (d == null || d.run == null || d.run.ending) return;
        DungeonRun run = d.run;
        for (Level lv : d.levels) {
            if (lv.number > run.currentLevel && lv.region != null && lv.region.contains(to)) {
                reachLevel(run, lv.number, true);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ fin con exito

    private void completeDungeon(DungeonRun run) {
        final Dungeon d = run.dungeon;
        if (run.completed) return;
        run.completed = true;
        run.ending = true;
        d.state = Dungeon.State.ENDING;
        LootTable table = plugin.loot().get(d.finalTable);
        Map<String, String> ph = placeholders(d);
        for (UUID u : new ArrayList<>(run.participants)) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            plugin.messages().send(p, d, "dungeon-complete", ph);
            List<String> names = new ArrayList<>();
            if (table != null) {
                for (ItemStack it : table.roll(random)) {
                    names.add(it.getAmount() + "x " + ItemUtil.niceName(it));
                    EntityUtil.give(p, it);
                }
            }
            for (String cmd : d.finalCommands) {
                if (cmd == null || cmd.trim().isEmpty()) continue;
                String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.replace("%player%", p.getName()));
            }
            Map<String, String> rph = new HashMap<>(ph);
            rph.put("items", names.isEmpty() ? "-" : String.join("&7, &f", names));
            plugin.messages().send(p, d, "rewards-received", rph);
        }
        final int delay = Math.max(0, plugin.getConfig().getInt("dungeon.end-delay-seconds", 10));
        new BukkitRunnable() {
            int left = delay;

            @Override
            public void run() {
                if (d.run == null || d.run != run) {
                    cancel();
                    return;
                }
                if (left <= 0) {
                    cancel();
                    endRun(d, true);
                    return;
                }
                Map<String, String> p2 = placeholders(d);
                p2.put("time", String.valueOf(left));
                for (UUID u : run.participants) plugin.messages().send(Bukkit.getPlayer(u), d, "returning", p2);
                left--;
            }
        }.runTaskTimer(plugin, 40L, 20L);
    }

    // ------------------------------------------------------------------ cofres

    private Block blockOfKey(String key) {
        String[] p = key.split(";");
        if (p.length < 4) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        try {
            return w.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String chestKey(Dungeon d, Block b) {
        String k = LocUtil.blockKey(b);
        if (d.chests.containsKey(k)) return k;
        if (b.getBlockData() instanceof org.bukkit.block.data.type.Chest ch
                && ch.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
            BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            for (BlockFace f : faces) {
                Block n = b.getRelative(f);
                if (n.getType() == b.getType()) {
                    String nk = LocUtil.blockKey(n);
                    if (d.chests.containsKey(nk)) return nk;
                }
            }
        }
        return null;
    }

    private void fill(Inventory inv, List<ItemStack> items) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) slots.add(i);
        Collections.shuffle(slots, random);
        int i = 0;
        for (ItemStack it : items) {
            if (i >= slots.size()) break;
            inv.setItem(slots.get(i++), it);
        }
    }

    /** Devuelve true si el evento debe cancelarse (cofre personal abierto manualmente). */
    public boolean handleChestInteract(Player p, Block b) {
        Dungeon d = getDungeonOf(p);
        if (d == null || d.run == null) return false;
        String key = chestKey(d, b);
        if (key == null) return false;
        LootTable table = plugin.loot().get(d.chests.get(key));
        if (table == null) return false;
        DungeonRun run = d.run;
        if (plugin.getConfig().getBoolean("chests.personal-loot", false)) {
            String ik = key + "|" + p.getUniqueId();
            Inventory inv = run.personalInventories.get(ik);
            if (inv == null) {
                inv = Bukkit.createInventory(null, 27, Text.color(plugin.messages().text("personal-chest-title", null)));
                fill(inv, table.roll(random));
                run.personalInventories.put(ik, inv);
            }
            p.openInventory(inv);
            return true;
        }
        if (run.openedChests.add(key) && b.getState() instanceof Container c) {
            fill(c.getInventory(), table.roll(random));
        }
        return false;
    }

    public List<ItemStack> previewRoll(LootTable t) {
        return t.roll(random);
    }

    // ------------------------------------------------------------------ ciclo de vida

    /** Al iniciar el servidor: deja todo limpio (puertas cerradas, sin jefes huerfanos). */
    public void cleanOnEnable() {
        for (Dungeon d : dungeons.values()) {
            for (Level lv : d.levels) for (DoorDef door : lv.doors) {
                if (!door.snapshot.isEmpty()) door.restore();
            }
            removeLeftoverBosses(d);
        }
    }

    public void shutdown() {
        stopTask();
        for (Dungeon d : new ArrayList<>(dungeons.values())) {
            if (d.run != null) {
                for (UUID u : d.run.participants) pendingLobby.add(u);
                for (UUID u : new ArrayList<>(d.run.participants)) {
                    Player p = Bukkit.getPlayer(u);
                    PlayerSnapshot s = d.run.snapshots.get(u);
                    if (p != null && s != null) restore(p, s);
                }
                cleanupRun(d, d.run);
                d.run = null;
            }
            if (d.waitBar != null) d.waitBar.removeAll();
            d.state = Dungeon.State.IDLE;
        }
        save();
    }
}
